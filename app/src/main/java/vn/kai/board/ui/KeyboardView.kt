package vn.kai.board.ui

import android.content.Context
import android.content.ClipboardManager
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.AudioManager
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import vn.kai.board.input.KeyAction
import vn.kai.board.input.KeyboardModeActionPolicy
import vn.kai.board.input.LongPressSymbolMap
import vn.kai.board.input.EmojiCatalog
import vn.kai.board.input.ClipboardHistoryStore
import vn.kai.board.input.NoteStore
import vn.kai.board.input.EmojiRecentStore
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.ThemeMode
import vn.kai.board.settings.KeyboardColorStyle
import vn.kai.board.settings.KeyboardThemePalette
import vn.kai.board.touch.KeyGeometry
import vn.kai.board.touch.TouchTargetPolicy
import vn.kai.board.touch.TouchDispatcher
import vn.kai.board.touch.RepeatKeyState

class KeyboardView(context: Context) : View(context) {
    var onKeyAction: (KeyAction) -> Unit = {}

    private enum class ResizeDrag { MOVE, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, RESET, DONE }
    private enum class Panel { NONE, EMOJI, CLIPBOARD }

    private val density = resources.displayMetrics.density
    private val toolbarHeight = 44f * density
    private val translationInputHeight = 54f * density
    private val suggestionMenuTimeoutMs = 2_200L
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 22f * density
    }
    private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 30f * density
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        textSize = 11f * density
    }
    private val keys = mutableListOf<KeyGeometry>()
    private val policy = TouchTargetPolicy(5f * density, 18f * density)
    private val slideThreshold = 18f * density
    private val pointers = TouchDispatcher(slideThreshold)
    private val longPressRunnables = mutableMapOf<Int, Runnable>()
    private val repeatState = RepeatKeyState()
    private var shifted = false
    private var symbols = false
    private var panel = Panel.NONE
    private var emojiGroup = 0
    private var clipboardTab = 0
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener { capturePrimaryClipboard() }
    private var symbolPage = 0
    private var previewKey: KeyGeometry? = null
    private var hapticIntensity = -1
    private var soundIntensity = -1
    private var popupEnabled = true
    private var dark = false
    private var colorStyle = KeyboardColorStyle.CLASSIC
    private var themePalette = KeyboardThemePalette.resolve(colorStyle, false)
    private var backgroundGradient: LinearGradient? = null
    private var suggestionsEnabled = false
    private var suggestions: List<String> = emptyList()
    private var suggestionMenuActive = false
    private var translationMode = false
    private var translationSourceLabel = "Tiếng Việt"
    private var translationTargetLabel = "Tiếng Anh"
    private var translationInput = ""
    private var translationStatus = ""
    private var aiMode = false
    private var aiPrompt = ""
    private var aiCursor = 0
    private var aiStatus = ""
    private var aiAnimationFrame = 0
    private var aiToneLabel = "Tự động ngẫu nhiên"
    private var voicePanel = false
    private var voiceStatus = ""
    private var voicePartial = ""
    private var voiceLevel = 0f
    private var voicePaused = false
    private var spaceLabel = "Tiếng Việt"
    private var keyboardHeightDp = 220
    private var keyRadiusDp = 7
    private var keyBorderEnabled = false
    private var keyBorderWidthDp = 1
    private var numberRowEnabled = false
    private var extendedSymbolsEnabled = true
    private var longPressSymbolsEnabled = false
    private var adjustmentMode = false
    private var bottomOffsetDp = 0
    private var keyboardTop = 0f
    private var widthPercent = 100
    private var leftOffsetDp = 0
    private var resizeDrag: ResizeDrag? = null
    private var resizeDownX = 0f
    private var resizeDownY = 0f
    private var startHeightDp = 220
    private var startBottomDp = 0
    private var startWidthPercent = 100
    private var startLeftDp = 0
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        post { reloadPreferences() }
    }

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val repeatPointerId = repeatState.pointerId
            if (repeatPointerId != null && pointers[repeatPointerId]?.key?.action == KeyAction.Backspace) {
                onKeyAction(KeyAction.Backspace)
                postDelayed(this, 55L)
            }
        }
    }
    private val restoreToolbarRunnable = Runnable {
        if (!suggestionMenuActive) return@Runnable
        suggestionMenuActive = false
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }
    private val aiAnimationRunnable = object : Runnable {
        override fun run() {
            if (!aiMode || !isAiProcessing()) return
            aiAnimationFrame = (aiAnimationFrame + 1) % 4
            invalidate()
            postDelayed(this, 320L)
        }
    }

    init {
        isFocusable = true
        reloadPreferences()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        KeyboardPreferences.register(context, preferenceListener)
        context.getSystemService(ClipboardManager::class.java)?.addPrimaryClipChangedListener(clipboardListener)
        capturePrimaryClipboard()
        reloadPreferences()
    }

    override fun onDetachedFromWindow() {
        KeyboardPreferences.unregister(context, preferenceListener)
        context.getSystemService(ClipboardManager::class.java)?.removePrimaryClipChangedListener(clipboardListener)
        removeCallbacks(restoreToolbarRunnable)
        removeCallbacks(aiAnimationRunnable)
        cancelActiveGesture()
        super.onDetachedFromWindow()
    }

    private fun reloadPreferences() {
        hapticIntensity = KeyboardPreferences.hapticIntensity(context)
        soundIntensity = KeyboardPreferences.soundIntensity(context)
        popupEnabled = KeyboardPreferences.popup(context)
        dark = when (KeyboardPreferences.theme(context)) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        colorStyle = KeyboardPreferences.colorStyle(context)
        themePalette = KeyboardThemePalette.resolve(colorStyle, dark)
        keyboardHeightDp = KeyboardPreferences.heightDp(context)
        keyRadiusDp = KeyboardPreferences.keyRadiusDp(context)
        keyBorderEnabled = KeyboardPreferences.keyBorder(context)
        keyBorderWidthDp = KeyboardPreferences.keyBorderWidthDp(context)
        numberRowEnabled = KeyboardPreferences.numberRow(context)
        extendedSymbolsEnabled = KeyboardPreferences.extendedSymbols(context)
        longPressSymbolsEnabled = KeyboardPreferences.longPressSymbols(context)
        adjustmentMode = KeyboardPreferences.adjustmentMode(context)
        bottomOffsetDp = KeyboardPreferences.bottomOffsetDp(context)
        widthPercent = KeyboardPreferences.widthPercent(context)
        leftOffsetDp = KeyboardPreferences.leftOffsetDp(context)
        updateBackgroundGradient()
        if (!extendedSymbolsEnabled) symbolPage = 0
        rebuildKeys(width.toFloat(), height.toFloat())
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val baseHeight = keyboardHeightDp * density
        val extraNumberRow = if (numberRowEnabled) {
            val gap = 5f * density
            val margin = 5f * density
            (baseHeight - margin * 2 - gap * 3) / 4f + gap
        } else 0f
        val bottomOffset = bottomOffsetDp * density
        // Both feature panels add their input card above the existing keyboard.
        // Never subtract this space from the character rows or their hitboxes.
        val featurePanelExtra = if (translationMode || aiMode) translationInputHeight else 0f
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            resolveSize((baseHeight + extraNumberRow + toolbarHeight + featurePanelExtra + bottomOffset).toInt(), heightMeasureSpec),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateBackgroundGradient(w, h)
        rebuildKeys(w.toFloat(), h.toFloat())
    }

    fun setShifted(value: Boolean) {
        shifted = value
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }

    fun setSpaceLabel(value: String) {
        if (spaceLabel == value) return
        spaceLabel = value
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }

    fun setSuggestionsEnabled(value: Boolean) {
        if (suggestionsEnabled == value) return
        suggestionsEnabled = value
        if (!value) {
            suggestions = emptyList()
            suggestionMenuActive = false
            removeCallbacks(restoreToolbarRunnable)
        }
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }

    fun setSuggestions(values: List<String>) {
        val next = values.filter(String::isNotBlank).distinct().take(3)
        removeCallbacks(restoreToolbarRunnable)
        suggestionMenuActive = suggestionsEnabled && next.isNotEmpty()
        if (suggestionMenuActive) postDelayed(restoreToolbarRunnable, suggestionMenuTimeoutMs)
        if (suggestions == next && keys.isNotEmpty()) {
            rebuildKeys(width.toFloat(), height.toFloat())
            invalidate()
            return
        }
        suggestions = next
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }

    fun setTranslationState(enabled: Boolean, source: String, target: String, input: String, status: String = "") {
        val sizeChanged = translationMode != enabled
        translationMode = enabled
        translationSourceLabel = source
        translationTargetLabel = target
        translationInput = input
        translationStatus = status
        suggestionMenuActive = false
        removeCallbacks(restoreToolbarRunnable)
        rebuildKeys(width.toFloat(), height.toFloat())
        if (sizeChanged) requestLayout()
        invalidate()
    }

    fun setAiState(enabled: Boolean, prompt: String, cursor: Int, status: String, toneLabel: String) {
        val sizeChanged = aiMode != enabled
        aiMode = enabled
        aiPrompt = prompt
        aiCursor = cursor.coerceIn(0, prompt.length)
        aiStatus = status
        aiToneLabel = toneLabel
        removeCallbacks(aiAnimationRunnable)
        if (isAiProcessing()) post(aiAnimationRunnable) else aiAnimationFrame = 0
        suggestionMenuActive = false
        removeCallbacks(restoreToolbarRunnable)
        rebuildKeys(width.toFloat(), height.toFloat())
        if (sizeChanged) requestLayout()
        invalidate()
    }

    fun setVoicePanel(enabled: Boolean, status: String = "", partial: String = "", level: Float = 0f, paused: Boolean = false) {
        val structureChanged = voicePanel != enabled || voicePaused != paused
        voicePanel = enabled
        voiceStatus = status
        voicePartial = partial
        voiceLevel = level.coerceIn(0f, 1f)
        voicePaused = paused
        if (structureChanged) rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (backgroundGradient != null) {
            keyPaint.shader = backgroundGradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), keyPaint)
            keyPaint.shader = null
        } else {
            canvas.drawColor(themePalette.background)
        }
        textPaint.color = themePalette.text
        popupTextPaint.color = textPaint.color
        hintPaint.color = themePalette.hint
        val scaleX = widthPercent / 100f
        val translateX = effectiveLeftPx(scaleX)
        canvas.save()
        canvas.translate(translateX, 0f)
        canvas.scale(scaleX, 1f)
        textPaint.textScaleX = 1f / scaleX
        popupTextPaint.textScaleX = 1f / scaleX
        hintPaint.textScaleX = 1f / scaleX
        val radius = keyRadiusDp * density
        if (voicePanel) drawVoicePanel(canvas)
        keys.forEach { key ->
            val toolbarKey = key.id.startsWith("toolbar-") || key.id.startsWith("translate-") && key.id != "translate-input" || key.id.startsWith("ai-") && key.id != "ai-input"
            val clipboardUiKey = key.id.startsWith("clipboard-")
            val floatingIcon = toolbarKey || key.id.startsWith("suggestion-") || clipboardUiKey || key.id == "voice-toggle"
            keyPaint.color = when {
                pointers.values.any { it.key == key } -> themePalette.pressed
                key.id == "translate-input" || key.id == "ai-input" || key.action is KeyAction.Character || key.action is KeyAction.CommitText || key.action == KeyAction.Space -> themePalette.key
                else -> themePalette.specialKey
            }
            if (!floatingIcon) {
                val keyRadius = if (key.id == "translate-input") 16f * density else radius
                keyPaint.style = Paint.Style.FILL
                val rect = RectF(key.left, key.top, key.right, key.bottom)
                canvas.drawRoundRect(rect, keyRadius, keyRadius, keyPaint)
                if (keyBorderEnabled) {
                    val stroke = keyBorderWidthDp * density
                    val inset = stroke / 2f
                    keyPaint.style = Paint.Style.STROKE
                    keyPaint.strokeWidth = stroke
                    keyPaint.color = Color.argb(
                        if (dark) 175 else 120,
                        Color.red(themePalette.accent),
                        Color.green(themePalette.accent),
                        Color.blue(themePalette.accent),
                    )
                    canvas.drawRoundRect(
                        RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset),
                        (keyRadius - inset).coerceAtLeast(0f),
                        (keyRadius - inset).coerceAtLeast(0f),
                        keyPaint,
                    )
                    keyPaint.style = Paint.Style.FILL
                }
            }
            val baseline = key.centerY - (textPaint.ascent() + textPaint.descent()) / 2f
            when {
                key.id.startsWith("clipboard-item-") -> drawClipboardItem(canvas, key)
                key.id == "clipboard-empty" -> drawClipboardEmptyState(canvas, key)
                key.id.startsWith("clipboard-tab-") -> drawClipboardPanelTab(canvas, key)
                key.id == "clipboard-manage" -> drawClipboardManageButton(canvas, key)
                key.id == "toolbar-back" -> drawBackIcon(canvas, key)
                key.id == "toolbar-emoji" -> drawSmileyIcon(canvas, key)
                key.id == "toolbar-mic" -> drawMicrophoneIcon(canvas, key)
                key.id == "translate-mic" || key.id == "ai-mic" -> drawMicrophoneIcon(canvas, key)
                key.id == "toolbar-clipboard" || (key.action as? KeyAction.SelectClipboardTab)?.index == 0 ->
                    drawClipboardIcon(canvas, key)
                key.id == "toolbar-settings" -> drawSettingsIcon(canvas, key)
                key.id == "toolbar-translate" -> drawTranslateIcon(canvas, key)
                key.id == "translate-source" || key.id == "translate-swap" || key.id == "translate-target" ->
                    drawTranslationControl(canvas, key)
                key.id == "toolbar-ai" -> drawAiIcon(canvas, key)
                key.id == "ai-send" || key.id == "enter" && aiMode -> drawAiSendIcon(canvas, key)
                key.id == "ai-input" -> drawAiInput(canvas, key)
                key.id == "shift" -> drawShiftIcon(canvas, key)
                key.id == "enter" -> drawEnterIcon(canvas, key)
                else -> canvas.drawText(key.label, key.centerX, baseline, textPaint)
            }
            if (longPressSymbolsEnabled && !symbols && key.action is KeyAction.Character) {
                LongPressSymbolMap.forKey(key.action.value)?.let { symbol ->
                    canvas.drawText(symbol.toString(), key.right - 6f * density, key.top + 14f * density, hintPaint)
                }
            }
        }
        previewKey?.takeIf { popupEnabled && it.action is KeyAction.Character }?.let { drawPreview(canvas, it) }
        canvas.restore()
        textPaint.textScaleX = 1f
        popupTextPaint.textScaleX = 1f
        hintPaint.textScaleX = 1f
        if (adjustmentMode) drawResizeOverlay(canvas, translateX, scaleX)
    }

    private fun updateBackgroundGradient(targetWidth: Int = width, targetHeight: Int = height) {
        val colors = themePalette.gradientColors
        if (colors == null || targetWidth <= 0 || targetHeight <= 0) {
            backgroundGradient = null
            return
        }
        backgroundGradient = LinearGradient(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), colors, null, Shader.TileMode.CLAMP)
    }

    private fun drawMicrophoneIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX
        val cy = key.centerY
        keyPaint.color = textPaint.color
        keyPaint.style = Paint.Style.STROKE
        keyPaint.strokeWidth = 2f * density
        keyPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawRoundRect(RectF(cx - 3.7f * density, cy - 8f * density, cx + 3.7f * density, cy + 2.5f * density), 3.7f * density, 3.7f * density, keyPaint)
        canvas.drawArc(RectF(cx - 7f * density, cy - 2.5f * density, cx + 7f * density, cy + 7f * density), 0f, 180f, false, keyPaint)
        canvas.drawLine(cx, cy + 7f * density, cx, cy + 10f * density, keyPaint)
        canvas.drawLine(cx - 4.5f * density, cy + 10f * density, cx + 4.5f * density, cy + 10f * density, keyPaint)
        keyPaint.style = Paint.Style.FILL
    }

    private fun drawClipboardIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX
        val cy = key.centerY
        val left = cx - 7f * density
        val right = cx + 7f * density
        val top = cy - 6.5f * density
        val bottom = cy + 9f * density
        keyPaint.color = textPaint.color
        keyPaint.style = Paint.Style.STROKE
        keyPaint.strokeWidth = 1.8f * density
        keyPaint.strokeCap = Paint.Cap.ROUND
        keyPaint.strokeJoin = Paint.Join.ROUND
        canvas.drawRoundRect(RectF(left, top, right, bottom), 2.5f * density, 2.5f * density, keyPaint)
        canvas.drawRoundRect(RectF(cx - 4f * density, cy - 9f * density, cx + 4f * density, cy - 4f * density), 1.8f * density, 1.8f * density, keyPaint)
        listOf(cy - 1f * density, cy + 3f * density, cy + 7f * density).forEach { y ->
            canvas.drawLine(cx - 4f * density, y, cx + 4f * density, y, keyPaint)
        }
        keyPaint.style = Paint.Style.FILL
    }

    private fun clipboardCardColor(): Int = themePalette.key

    private fun clipboardOutlineColor(): Int = themePalette.specialKey

    private fun clipboardAccentColor(): Int = themePalette.accent

    private fun drawClipboardCard(canvas: Canvas, key: KeyGeometry, selected: Boolean = false) {
        keyPaint.style = Paint.Style.FILL
        keyPaint.color = if (selected) clipboardAccentColor() else clipboardCardColor()
        canvas.drawRoundRect(RectF(key.left, key.top, key.right, key.bottom), 14f * density, 14f * density, keyPaint)
        keyPaint.style = Paint.Style.STROKE
        keyPaint.strokeWidth = 1f * density
        keyPaint.color = if (selected) clipboardAccentColor() else clipboardOutlineColor()
        canvas.drawRoundRect(RectF(key.left, key.top, key.right, key.bottom), 14f * density, 14f * density, keyPaint)
        keyPaint.style = Paint.Style.FILL
    }

    private fun drawClipboardItem(canvas: Canvas, key: KeyGeometry) {
        drawClipboardCard(canvas, key)
        val content = (key.action as? KeyAction.CommitText)?.value.orEmpty().replace(Regex("\\s+"), " ").trim()
        val pinned = key.id.contains("-pinned-")
        val lineLimit = ((key.right - key.left) / (7.5f * density)).toInt().coerceAtLeast(10)
        val first = content.take(lineLimit)
        val remainder = content.drop(first.length).trimStart()
        val second = remainder.take(lineLimit - 1) + if (remainder.length >= lineLimit) "…" else ""
        val oldAlign = textPaint.textAlign
        val oldSize = textPaint.textSize
        val oldColor = textPaint.color
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 14f * density
        textPaint.color = if (dark) Color.rgb(248, 250, 252) else Color.rgb(17, 24, 39)
        val left = key.left + 13f * density
        val firstY = if (second.isEmpty()) key.centerY - (textPaint.ascent() + textPaint.descent()) / 2f else key.centerY - 3f * density
        canvas.drawText(first, left, firstY, textPaint)
        if (second.isNotEmpty()) canvas.drawText(second, left, firstY + 18f * density, textPaint)
        if (pinned) {
            keyPaint.color = clipboardAccentColor()
            canvas.drawCircle(key.right - 12f * density, key.top + 12f * density, 3f * density, keyPaint)
            keyPaint.strokeWidth = 1.5f * density
            canvas.drawLine(key.right - 12f * density, key.top + 14f * density, key.right - 12f * density, key.top + 18f * density, keyPaint)
        }
        textPaint.textAlign = oldAlign
        textPaint.textSize = oldSize
        textPaint.color = oldColor
    }

    private fun drawClipboardEmptyState(canvas: Canvas, key: KeyGeometry) {
        drawClipboardCard(canvas, key)
        val oldSize = textPaint.textSize
        val oldColor = textPaint.color
        textPaint.textSize = 15f * density
        canvas.drawText(key.label, key.centerX, key.centerY - 4f * density, textPaint)
        textPaint.textSize = 12f * density
        textPaint.color = if (dark) Color.rgb(148, 163, 184) else Color.rgb(100, 116, 139)
        val helper = if (clipboardTab == 0) "Sao chép văn bản để hiển thị tại đây" else "Thêm nội dung thường dùng trong Quản lý"
        canvas.drawText(helper, key.centerX, key.centerY + 17f * density, textPaint)
        textPaint.textSize = oldSize
        textPaint.color = oldColor
    }

    private fun drawClipboardPanelTab(canvas: Canvas, key: KeyGeometry) {
        val selected = key.id == "clipboard-tab-$clipboardTab"
        drawClipboardCard(canvas, key, selected)
        val oldColor = textPaint.color
        textPaint.color = if (selected) Color.WHITE else oldColor
        if (key.id == "clipboard-tab-0") drawClipboardIcon(canvas, key) else drawNoteIcon(canvas, key)
        textPaint.color = oldColor
    }

    private fun drawNoteIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX
        val cy = key.centerY
        prepareMonoIconPaint()
        keyPaint.strokeWidth = 1.7f * density
        canvas.drawRoundRect(RectF(cx - 7f * density, cy - 8f * density, cx + 7f * density, cy + 8f * density), 2f * density, 2f * density, keyPaint)
        repeat(3) { row ->
            val y = cy - 3f * density + row * 4f * density
            canvas.drawLine(cx - 4f * density, y, cx + 4f * density, y, keyPaint)
        }
        keyPaint.style = Paint.Style.FILL
    }

    private fun drawClipboardManageButton(canvas: Canvas, key: KeyGeometry) {
        drawSettingsIcon(canvas, key)
    }

    /** Monochrome translation mark using overlapping language cards. */
    @Suppress("unused")
    private fun drawLegacyTranslateIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX
        val cy = key.centerY
        prepareMonoIconPaint()
        keyPaint.strokeWidth = 1.8f * density
        canvas.drawRoundRect(
            RectF(cx - 9f * density, cy - 8f * density, cx + 3f * density, cy + 5f * density),
            2f * density, 2f * density, keyPaint,
        )
        canvas.drawRoundRect(
            RectF(cx - 2f * density, cy - 3f * density, cx + 10f * density, cy + 10f * density),
            2f * density, 2f * density, keyPaint,
        )
        val oldSize = textPaint.textSize
        textPaint.textSize = 7.5f * density
        canvas.drawText("G", cx - 3.3f * density, cy + 1f * density, textPaint)
        canvas.drawText("文", cx + 4f * density, cy + 7f * density, textPaint)
        textPaint.textSize = oldSize
        keyPaint.style = Paint.Style.FILL
    }

    /** Circular Translate mark based on two language glyphs and two directional arrows. */
    private fun drawTranslateIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX
        val cy = key.centerY
        prepareMonoIconPaint()
        keyPaint.strokeWidth = 1.45f * density
        keyPaint.strokeJoin = Paint.Join.ROUND
        keyPaint.strokeCap = Paint.Cap.ROUND

        val orbit = RectF(
            cx - 8f * density, cy - 8f * density,
            cx + 8f * density, cy + 8f * density,
        )
        canvas.drawArc(orbit, 202f, 103f, false, keyPaint)
        canvas.drawArc(orbit, 22f, 103f, false, keyPaint)

        // Filled arrow heads remain legible after Android scales the 20dp toolbar icon.
        keyPaint.style = Paint.Style.FILL
        canvas.drawPath(Path().apply {
            moveTo(cx + 5.4f * density, cy - 6.2f * density)
            lineTo(cx + 3.2f * density, cy - 6.5f * density)
            lineTo(cx + 5f * density, cy - 4.2f * density)
            close()
        }, keyPaint)
        canvas.drawPath(Path().apply {
            moveTo(cx - 5.4f * density, cy + 6.2f * density)
            lineTo(cx - 3.2f * density, cy + 6.5f * density)
            lineTo(cx - 5f * density, cy + 4.2f * density)
            close()
        }, keyPaint)
        keyPaint.style = Paint.Style.STROKE

        // G replaces the A from the reference, positioned in the upper-right quadrant.
        canvas.drawArc(
            RectF(cx + 1.4f * density, cy - 5.5f * density, cx + 7f * density, cy + 1f * density),
            42f, 285f, false, keyPaint,
        )
        canvas.drawLine(cx + 4.3f * density, cy - 2.2f * density, cx + 7.1f * density, cy - 2.2f * density, keyPaint)
        canvas.drawLine(cx + 7.1f * density, cy - 2.2f * density, cx + 7.1f * density, cy - 0.5f * density, keyPaint)

        // Simplified 文 in the lower-left quadrant.
        canvas.drawLine(cx - 4.8f * density, cy + 0.2f * density, cx - 4.8f * density, cy + 1.6f * density, keyPaint)
        canvas.drawLine(cx - 7.8f * density, cy + 1.8f * density, cx - 1.8f * density, cy + 1.8f * density, keyPaint)
        canvas.drawLine(cx - 7f * density, cy + 3f * density, cx - 2.6f * density, cy + 7f * density, keyPaint)
        canvas.drawLine(cx - 2.6f * density, cy + 3f * density, cx - 7f * density, cy + 7f * density, keyPaint)
        keyPaint.style = Paint.Style.FILL
    }

    private fun drawTranslationControl(canvas: Canvas, key: KeyGeometry) {
        val oldSize = textPaint.textSize
        val availableWidth = key.right - key.left - 8f * density
        var sizeSp = if (key.id == "translate-swap") 15f else 16f
        textPaint.textSize = sizeSp * density
        while (textPaint.measureText(key.label) > availableWidth && sizeSp > 12f) {
            sizeSp -= 1f
            textPaint.textSize = sizeSp * density
        }
        val baseline = key.centerY - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(key.label, key.centerX, baseline, textPaint)
        textPaint.textSize = oldSize
    }

    private fun drawAiIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX; val cy = key.centerY
        prepareMonoIconPaint()
        keyPaint.strokeWidth = 1.7f * density
        val frame = Path().apply {
            moveTo(cx - 1.5f * density, cy - 8f * density)
            lineTo(cx - 6.5f * density, cy - 8f * density)
            quadTo(cx - 9f * density, cy - 8f * density, cx - 9f * density, cy - 5.5f * density)
            lineTo(cx - 9f * density, cy + 5.5f * density)
            quadTo(cx - 9f * density, cy + 8f * density, cx - 6.5f * density, cy + 8f * density)
            lineTo(cx + 5.5f * density, cy + 8f * density)
            quadTo(cx + 8f * density, cy + 8f * density, cx + 8f * density, cy + 5.5f * density)
            lineTo(cx + 8f * density, cy + 1.5f * density)
        }
        canvas.drawPath(frame, keyPaint)

        val oldSize = textPaint.textSize
        val oldTypeface = textPaint.typeface
        textPaint.textSize = 8.5f * density
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("AI", cx - 1.3f * density, cy + 3.3f * density, textPaint)
        textPaint.textSize = oldSize
        textPaint.typeface = oldTypeface

        keyPaint.style = Paint.Style.FILL
        fun sparkle(x: Float, y: Float, radius: Float) {
            canvas.drawPath(Path().apply {
                moveTo(cx + x * density, cy + (y - radius) * density)
                lineTo(cx + (x + radius * 0.55f) * density, cy + y * density)
                lineTo(cx + x * density, cy + (y + radius) * density)
                lineTo(cx + (x - radius * 0.55f) * density, cy + y * density)
                close()
            }, keyPaint)
        }
        sparkle(2.2f, -8f, 3.1f)
        sparkle(7.6f, -8f, 1.7f)
        sparkle(6.5f, -3.2f, 2.3f)
    }

    private fun drawAiSendIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX
        val cy = key.centerY
        prepareMonoIconPaint()
        keyPaint.style = Paint.Style.FILL
        // Solid folded-paper silhouette stays readable at the small toolbar size.
        canvas.drawPath(Path().apply {
            moveTo(cx - 11f * density, cy - 7f * density)
            lineTo(cx + 10f * density, cy)
            lineTo(cx - 11f * density, cy + 7f * density)
            lineTo(cx - 5f * density, cy + 1.4f * density)
            lineTo(cx + 2f * density, cy)
            lineTo(cx - 5f * density, cy - 1.4f * density)
            close()
        }, keyPaint)
        // Small AI sparkle is separated from the plane tip to avoid visual clutter.
        canvas.drawPath(Path().apply {
            moveTo(cx + 6f * density, cy - 10f * density)
            lineTo(cx + 7f * density, cy - 7.8f * density)
            lineTo(cx + 9f * density, cy - 7f * density)
            lineTo(cx + 7f * density, cy - 6.2f * density)
            lineTo(cx + 6f * density, cy - 4f * density)
            lineTo(cx + 5f * density, cy - 6.2f * density)
            lineTo(cx + 3f * density, cy - 7f * density)
            lineTo(cx + 5f * density, cy - 7.8f * density)
            close()
        }, keyPaint)
    }

    private data class AiTextWindow(val start: Int, val end: Int)

    private fun isAiProcessing(): Boolean = aiStatus.contains("đang xử lý", ignoreCase = true)

    private fun aiTextWindow(key: KeyGeometry): AiTextWindow {
        if (aiPrompt.isEmpty()) return AiTextWindow(0, 0)
        val oldSize = textPaint.textSize
        textPaint.textSize = 16f * density
        val available = (key.right - key.left - if (isAiProcessing()) 76f * density else 30f * density).coerceAtLeast(40f)
        var start = aiCursor.coerceIn(0, aiPrompt.length)
        var used = 0f
        while (start > 0) {
            val width = textPaint.measureText(aiPrompt, start - 1, start)
            if (used + width > available * 0.68f) break
            used += width
            start--
        }
        var end = aiCursor.coerceIn(start, aiPrompt.length)
        while (end < aiPrompt.length) {
            val width = textPaint.measureText(aiPrompt, end, end + 1)
            if (used + width > available) break
            used += width
            end++
        }
        textPaint.textSize = oldSize
        return AiTextWindow(start, end)
    }

    private fun drawAiInput(canvas: Canvas, key: KeyGeometry) {
        val oldAlign = textPaint.textAlign
        val oldSize = textPaint.textSize
        val oldTypeface = textPaint.typeface
        val oldColor = textPaint.color
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 16f * density
        textPaint.typeface = Typeface.DEFAULT
        val left = key.left + 14f * density
        val baseline = key.centerY - (textPaint.ascent() + textPaint.descent()) / 2f - if (aiStatus.isNotEmpty()) 4f * density else 0f
        canvas.save()
        canvas.clipRect(key.left + 10f * density, key.top, key.right - 10f * density, key.bottom)
        if (aiPrompt.isEmpty()) {
            textPaint.color = hintPaint.color
            canvas.drawText("Nhập yêu cầu cho AI", left, baseline, textPaint)
        } else {
            val window = aiTextWindow(key)
            val shown = aiPrompt.substring(window.start, window.end)
            canvas.drawText(shown, left, baseline, textPaint)
            val cursorPrefix = aiPrompt.substring(window.start, aiCursor.coerceIn(window.start, window.end))
            val cursorX = left + textPaint.measureText(cursorPrefix)
            keyPaint.color = themePalette.accent
            keyPaint.strokeWidth = 1.6f * density
            canvas.drawLine(cursorX, baseline + textPaint.ascent(), cursorX, baseline + textPaint.descent(), keyPaint)
        }
        if (aiStatus.isNotEmpty()) {
            textPaint.textSize = 10f * density
            textPaint.color = hintPaint.color
            val status = if (isAiProcessing()) "AI đang xử lý${".".repeat(aiAnimationFrame)}" else aiStatus
            canvas.drawText(status, left, key.bottom - 5f * density, textPaint)
        }
        canvas.restore()
        textPaint.textAlign = oldAlign
        textPaint.textSize = oldSize
        textPaint.typeface = oldTypeface
        textPaint.color = oldColor
    }

    private fun aiCursorForX(key: KeyGeometry, touchX: Float): Int {
        if (aiPrompt.isEmpty()) return 0
        val oldSize = textPaint.textSize
        textPaint.textSize = 16f * density
        val window = aiTextWindow(key)
        val localX = (touchX - key.left - 14f * density).coerceAtLeast(0f)
        var width = 0f
        var best = window.start
        for (index in window.start until window.end) {
            val charWidth = textPaint.measureText(aiPrompt, index, index + 1)
            if (localX < width + charWidth / 2f) break
            width += charWidth
            best = index + 1
        }
        textPaint.textSize = oldSize
        return best
    }

    private fun drawSmileyIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX; val cy = key.centerY
        prepareMonoIconPaint()
        canvas.drawCircle(cx, cy, 8.5f * density, keyPaint)
        canvas.drawCircle(cx - 3f * density, cy - 2.5f * density, 0.7f * density, keyPaint)
        canvas.drawCircle(cx + 3f * density, cy - 2.5f * density, 0.7f * density, keyPaint)
        canvas.drawArc(RectF(cx - 4.5f * density, cy - 1f * density, cx + 4.5f * density, cy + 5f * density), 15f, 150f, false, keyPaint)
        keyPaint.style = Paint.Style.FILL
    }

    private fun drawSettingsIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX; val cy = key.centerY
        prepareMonoIconPaint()
        canvas.drawCircle(cx, cy, 5.5f * density, keyPaint)
        canvas.drawCircle(cx, cy, 2f * density, keyPaint)
        repeat(8) { index ->
            val angle = Math.PI * index / 4.0
            canvas.drawLine(
                cx + cos(angle).toFloat() * 6.5f * density,
                cy + sin(angle).toFloat() * 6.5f * density,
                cx + cos(angle).toFloat() * 9f * density,
                cy + sin(angle).toFloat() * 9f * density,
                keyPaint,
            )
        }
        keyPaint.style = Paint.Style.FILL
    }

    private fun drawBackIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX; val cy = key.centerY
        prepareMonoIconPaint()
        canvas.drawLine(cx + 8f * density, cy, cx - 7f * density, cy, keyPaint)
        canvas.drawLine(cx - 7f * density, cy, cx - 1f * density, cy - 6f * density, keyPaint)
        canvas.drawLine(cx - 7f * density, cy, cx - 1f * density, cy + 6f * density, keyPaint)
        keyPaint.style = Paint.Style.FILL
    }

    private fun drawShiftIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX
        val cy = key.centerY
        prepareMonoIconPaint()
        keyPaint.strokeWidth = 2.4f * density
        val icon = Path().apply {
            moveTo(cx, cy - 10f * density)
            lineTo(cx - 9f * density, cy - 1f * density)
            lineTo(cx - 4.5f * density, cy - 1f * density)
            lineTo(cx - 4.5f * density, cy + 9f * density)
            lineTo(cx + 4.5f * density, cy + 9f * density)
            lineTo(cx + 4.5f * density, cy - 1f * density)
            lineTo(cx + 9f * density, cy - 1f * density)
            close()
        }
        canvas.drawPath(icon, keyPaint)
        if (shifted) canvas.drawLine(cx - 5f * density, cy + 13f * density, cx + 5f * density, cy + 13f * density, keyPaint)
        keyPaint.style = Paint.Style.FILL
    }

    private fun drawEnterIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX
        val cy = key.centerY
        prepareMonoIconPaint()
        keyPaint.strokeWidth = 2.6f * density
        canvas.drawLine(cx + 8f * density, cy - 8f * density, cx + 8f * density, cy + 1f * density, keyPaint)
        canvas.drawLine(cx + 8f * density, cy + 1f * density, cx - 7f * density, cy + 1f * density, keyPaint)
        canvas.drawLine(cx - 7f * density, cy + 1f * density, cx - 1f * density, cy - 5f * density, keyPaint)
        canvas.drawLine(cx - 7f * density, cy + 1f * density, cx - 1f * density, cy + 7f * density, keyPaint)
        keyPaint.style = Paint.Style.FILL
    }

    private fun prepareMonoIconPaint() {
        keyPaint.color = textPaint.color
        keyPaint.style = Paint.Style.STROKE
        keyPaint.strokeWidth = 1.8f * density
        keyPaint.strokeCap = Paint.Cap.ROUND
        keyPaint.strokeJoin = Paint.Join.ROUND
    }

    private fun drawPreview(canvas: Canvas, key: KeyGeometry) {
        val width = 58f * density
        val height = 64f * density
        val left = (key.centerX - width / 2).coerceIn(2f * density, this.width - width - 2f * density)
        val stemHeight = 10f * density
        val top = (key.top - height - stemHeight + 2f * density).coerceAtLeast(2f * density)
        val bottom = top + height
        keyPaint.color = if (dark) Color.rgb(71, 85, 105) else Color.WHITE
        canvas.drawRoundRect(RectF(left, top, left + width, bottom), 13f * density, 13f * density, keyPaint)
        val stemCenter = key.centerX.coerceIn(left + 12f * density, left + width - 12f * density)
        val stem = Path().apply {
            moveTo(stemCenter - 8f * density, bottom - 2f * density)
            lineTo(stemCenter, bottom + stemHeight)
            lineTo(stemCenter + 8f * density, bottom - 2f * density)
            close()
        }
        canvas.drawPath(stem, keyPaint)
        val baseline = top + height / 2 - (popupTextPaint.ascent() + popupTextPaint.descent()) / 2
        val pointer = pointers.values.lastOrNull { it.key == key }
        val previewLabel = if (pointer?.longPressed == true && key.action is KeyAction.Character) {
            LongPressSymbolMap.forKey(key.action.value)?.toString() ?: key.label
        } else key.label
        canvas.drawText(previewLabel, left + width / 2, baseline, popupTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (adjustmentMode) return handleResizeTouch(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> startPointer(event, event.actionIndex)
            MotionEvent.ACTION_MOVE -> updatePointers(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> releasePointer(event, event.actionIndex)
            MotionEvent.ACTION_CANCEL -> cancelActiveGesture()
        }
        return true
    }

    private fun startPointer(event: MotionEvent, index: Int) {
        val id = event.getPointerId(index)
        val x = toKeyboardX(event.getX(index))
        val y = event.getY(index)
        val key = policy.resolve(keys, x, y)
        pointers.down(id, key, x, y)
        previewKey = key
        if (key != null) {
            vibrate()
            playKeySound(key.action)
            if (key.action == KeyAction.Backspace && repeatState.start(id)) {
                postDelayed(repeatRunnable, 400L)
            }
            scheduleLongPress(id, key)
        }
        invalidate()
    }

    private fun updatePointers(event: MotionEvent) {
        for (index in 0 until event.pointerCount) {
            val id = event.getPointerId(index)
            val state = pointers[id] ?: continue
            val x = toKeyboardX(event.getX(index))
            val y = event.getY(index)
            if (pointers.crossedSlideThreshold(id, x, y)) {
                val target = policy.resolve(keys, x, y)
                if (target != state.key) {
                    if (repeatState.isActive(id)) stopRepeat()
                    cancelLongPress(id)
                    state.key = target
                    previewKey = target
                    if (target != null) vibrate()
                }
            }
        }
        invalidate()
    }

    private fun releasePointer(event: MotionEvent, index: Int) {
        val id = event.getPointerId(index)
        val state = pointers.remove(id) ?: return
        val releasedOver = policy.resolve(keys, toKeyboardX(event.getX(index)), event.getY(index))
        val action = state.key?.takeIf { it == releasedOver }?.action
        cancelLongPress(id)
        if (repeatState.isActive(id)) stopRepeat()
        previewKey = pointers.values.lastOrNull()?.key
        if (action == KeyAction.ToggleSymbols) {
            panel = Panel.NONE
            symbols = !symbols
            symbolPage = 0
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action == KeyAction.ToggleSymbolPage) {
            symbolPage = 1 - symbolPage
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action == KeyAction.ToggleEmoji) {
            panel = if (panel == Panel.EMOJI) Panel.NONE else Panel.EMOJI
            symbols = false
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action == KeyAction.ToggleClipboard) {
            panel = if (panel == Panel.CLIPBOARD) Panel.NONE else Panel.CLIPBOARD
            if (panel == Panel.CLIPBOARD) clipboardTab = 0
            symbols = false
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action is KeyAction.SelectEmojiGroup) {
            emojiGroup = action.index.coerceIn(0, EmojiCatalog.groups.size)
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action is KeyAction.SelectClipboardTab) {
            clipboardTab = action.index.coerceIn(0, 1)
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action == KeyAction.HideKeyboard) {
            when {
                panel != Panel.NONE -> panel = Panel.NONE
                symbols -> { symbols = false; symbolPage = 0 }
                else -> onKeyAction(action)
            }
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (handleAdjustmentAction(action)) {
            Unit
        } else if (state.key?.id == "ai-input" && state.key == releasedOver && !state.longPressed) {
            val inputKey = state.key ?: return
            onKeyAction(KeyAction.SetAiCursor(aiCursorForX(inputKey, toKeyboardX(event.getX(index)))))
        } else if (action != null && !state.longPressed) {
            if (panel == Panel.EMOJI && action is KeyAction.CommitText) {
                EmojiRecentStore.add(context, action.value)
            }
            onKeyAction(action)
            if (action is KeyAction.CommitText && panel != Panel.NONE) {
                panel = Panel.NONE
                rebuildKeys(width.toFloat(), height.toFloat())
            }
        }
        invalidate()
    }

    fun cancelActiveGesture() {
        pointers.clear()
        longPressRunnables.values.forEach(::removeCallbacks)
        longPressRunnables.clear()
        previewKey = null
        stopRepeat()
        invalidate()
    }

    fun closeMediaPanel() {
        if (panel == Panel.NONE) return
        panel = Panel.NONE
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }

    private fun stopRepeat() {
        removeCallbacks(repeatRunnable)
        repeatState.cancel()
    }

    private fun scheduleLongPress(pointerId: Int, key: KeyGeometry) {
        val suggestion = (key.action as? KeyAction.SelectSuggestion)?.value
        if (suggestion != null) {
            val runnable = Runnable {
                val state = pointers[pointerId]
                if (state?.key == key && !state.longPressed) {
                    state.longPressed = true
                    onKeyAction(KeyAction.ForgetSuggestion(suggestion))
                    vibrate()
                }
                longPressRunnables.remove(pointerId)
            }
            longPressRunnables[pointerId] = runnable
            postDelayed(runnable, 420L)
            return
        }
        val clipboardText = if (panel == Panel.CLIPBOARD && clipboardTab == 0) {
            (key.action as? KeyAction.CommitText)?.value
        } else null
        if (clipboardText != null) {
            val runnable = Runnable {
                val state = pointers[pointerId]
                if (state?.key == key && !state.longPressed) {
                    state.longPressed = true
                    ClipboardHistoryStore.togglePinned(context, clipboardText)
                    vibrate()
                    rebuildKeys(width.toFloat(), height.toFloat())
                    invalidate()
                }
                longPressRunnables.remove(pointerId)
            }
            longPressRunnables[pointerId] = runnable
            postDelayed(runnable, 420L)
            return
        }
        if (!longPressSymbolsEnabled || symbols) return
        val character = (key.action as? KeyAction.Character)?.value ?: return
        val symbol = LongPressSymbolMap.forKey(character) ?: return
        val runnable = Runnable {
            val state = pointers[pointerId]
            if (state?.key == key && !state.longPressed) {
                state.longPressed = true
                onKeyAction(KeyAction.Character(symbol))
                vibrate()
                invalidate()
            }
            longPressRunnables.remove(pointerId)
        }
        longPressRunnables[pointerId] = runnable
        postDelayed(runnable, 420L)
    }

    private fun cancelLongPress(pointerId: Int) {
        longPressRunnables.remove(pointerId)?.let(::removeCallbacks)
    }

    private fun handleAdjustmentAction(action: KeyAction?): Boolean = when (action) {
        KeyAction.MoveKeyboardUp -> {
            KeyboardPreferences.setBottomOffsetDp(context, bottomOffsetDp + 8); true
        }
        KeyAction.MoveKeyboardDown -> {
            KeyboardPreferences.setBottomOffsetDp(context, bottomOffsetDp - 8); true
        }
        KeyAction.DecreaseKeyboardHeight -> {
            KeyboardPreferences.setHeightDp(context, keyboardHeightDp - 10); true
        }
        KeyAction.IncreaseKeyboardHeight -> {
            KeyboardPreferences.setHeightDp(context, keyboardHeightDp + 10); true
        }
        KeyAction.FinishKeyboardAdjustment -> {
            KeyboardPreferences.setBoolean(context, KeyboardPreferences.ADJUSTMENT_MODE, false); true
        }
        else -> false
    }

    private fun vibrate() {
        if (hapticIntensity == 0) return
        val vibrator = context.getSystemService(Vibrator::class.java)
        val effect = if (hapticIntensity < 0) {
            if (Build.VERSION.SDK_INT >= 29) VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            else VibrationEffect.createOneShot(8L, VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            val amplitude = (hapticIntensity * 255 / 100).coerceIn(1, 255)
            VibrationEffect.createOneShot(8L, amplitude)
        }
        vibrator?.vibrate(effect)
    }

    private fun playKeySound(action: KeyAction) {
        if (soundIntensity == 0) return
        val effect = when (action) {
            KeyAction.Space -> AudioManager.FX_KEYPRESS_SPACEBAR
            KeyAction.Backspace -> AudioManager.FX_KEYPRESS_DELETE
            KeyAction.Enter -> AudioManager.FX_KEYPRESS_RETURN
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        if (soundIntensity < 0) audio.playSoundEffect(effect)
        else {
            val linear = soundIntensity / 100f
            audio.playSoundEffect(effect, linear * linear)
        }
    }

    private fun rebuildKeys(totalWidth: Float, totalHeight: Float) {
        if (totalWidth <= 0f || totalHeight <= 0f) return
        keys.clear()
        val gap = 5f * density
        val margin = 5f * density
        keyboardTop = toolbarHeight + if (translationMode || aiMode) translationInputHeight else 0f
        addToolbar(totalWidth)
        val usableHeight = totalHeight - keyboardTop - bottomOffsetDp * density
        val rowCount = if (numberRowEnabled) 5 else 4
        val rowHeight = (usableHeight - margin * 2 - gap * (rowCount - 1)) / rowCount
        if (panel == Panel.EMOJI) {
            addEmojiPanel(totalWidth, rowCount, rowHeight, margin, gap)
        } else if (panel == Panel.CLIPBOARD) {
            addClipboardPanel(totalWidth, rowCount, rowHeight, margin, gap)
        } else if (voicePanel) {
            addVoicePanel(totalWidth, totalHeight, margin)
        } else if (symbols) {
            if (symbolPage == 0) {
                addCharacterRow("1234567890", 0, 0f, 0f, rowHeight, margin, gap)
                val offset = if (numberRowEnabled) {
                    addCharacterRow("`~[]{}<>^|", 1, 0f, 0f, rowHeight, margin, gap)
                    1
                } else 0
                addCharacterRow("@#₫_&-+()/", offset + 1, 0f, 0f, rowHeight, margin, gap)
                addActionCharacterRow("*\"':;!?", offset + 2, totalWidth, rowHeight, margin, gap)
            } else {
                val offset = if (numberRowEnabled) {
                    addCharacterRow("1234567890", 0, 0f, 0f, rowHeight, margin, gap)
                    1
                } else 0
                addCharacterRow("`~[]{}<>^|", offset, 0f, 0f, rowHeight, margin, gap)
                addCharacterRow("€£¥₫%‰°•÷×", offset + 1, 0f, 0f, rowHeight, margin, gap)
                addActionCharacterRow("$¢₹₩¿¡…", offset + 2, totalWidth, rowHeight, margin, gap)
            }
        } else {
            val offset = if (numberRowEnabled) 1 else 0
            if (numberRowEnabled) addCharacterRow("1234567890", 0, 0f, 0f, rowHeight, margin, gap)
            addCharacterRow("qwertyuiop", offset, 0f, 0f, rowHeight, margin, gap)
            addCharacterRow("asdfghjkl", offset + 1, totalWidth * 0.035f, totalWidth * 0.035f, rowHeight, margin, gap)
            addActionCharacterRow("zxcvbnm", offset + 2, totalWidth, rowHeight, margin, gap)
        }
        if (panel == Panel.NONE && !voicePanel) addBottomRow(totalWidth, rowCount - 1, rowHeight, margin, gap)
    }

    @Suppress("unused")
    private fun addLegacyToolbar(totalWidth: Float) {
        val margin = 5f * density
        if (translationMode) {
            addTranslationPanel(totalWidth, margin)
            return
        }
        if (aiMode) {
            addAiPanel(totalWidth, margin)
            return
        }
        val bottom = toolbarHeight - 4f * density
        if (suggestionMenuActive && panel == Panel.NONE && !symbols) {
            addSuggestionToolbar(totalWidth, margin, bottom)
            return
        }
        val menuActions = listOf(
            Triple("back", "", KeyAction.HideKeyboard),
            Triple("emoji", "☺︎", KeyAction.ToggleEmoji),
            Triple("clipboard", "▣", KeyAction.ToggleClipboard),
            Triple("settings", "⚙︎", KeyAction.OpenSettings),
            Triple("mic", "♩", KeyAction.VoiceInput),
            Triple("translate", "", KeyAction.OpenTranslator),
            Triple("ai", "", KeyAction.OpenAi),
        )
        val edgeWidth = 50f * density
        val (_, _, backAction) = menuActions[0]
        val (_, _, emojiAction) = menuActions[1]
        addKey("toolbar-back", "", backAction, margin, 4f * density, margin + edgeWidth, bottom)
        addKey("toolbar-emoji", "", emojiAction, totalWidth - margin - edgeWidth, 4f * density, totalWidth - margin, bottom)

        // Preserve the current Mic/Clipboard/Settings centers. Only this middle
        // region redistributes when more toolbar actions are added later.
        val middleActions = listOf(menuActions[4], menuActions[5], menuActions[6], menuActions[2], menuActions[3])
        val originalCellWidth = (totalWidth - margin * 2) / 5f
        val middleLeft = margin + originalCellWidth
        val middleRight = totalWidth - margin - originalCellWidth
        val middleCellWidth = (middleRight - middleLeft) / middleActions.size
        middleActions.forEachIndexed { index, (id, label, action) ->
            val center = middleLeft + middleCellWidth * (index + 0.5f)
            val half = 25f * density
            addKey("toolbar-$id", label, action, center - half, 4f * density, center + half, bottom)
        }
    }

    private fun addToolbar(totalWidth: Float) {
        val margin = 5f * density
        if (translationMode) { addTranslationPanel(totalWidth, margin); return }
        if (aiMode) { addAiPanel(totalWidth, margin); return }
        val bottom = toolbarHeight - 4f * density
        if (suggestionMenuActive && panel == Panel.NONE && !symbols) {
            addSuggestionToolbar(totalWidth, margin, bottom)
            return
        }
        val actions = listOf(
            Triple("back", "", KeyAction.HideKeyboard),
            Triple("mic", "", KeyAction.VoiceInput),
            Triple("translate", "", KeyAction.OpenTranslator),
            Triple("ai", "", KeyAction.OpenAi),
            Triple("clipboard", "", KeyAction.ToggleClipboard),
            Triple("settings", "", KeyAction.OpenSettings),
            Triple("emoji", "", KeyAction.ToggleEmoji),
        )
        val cellWidth = (totalWidth - margin * 2) / actions.size
        actions.forEachIndexed { index, (id, label, action) ->
            val left = margin + index * cellWidth
            addKey("toolbar-$id", label, action, left, 4f * density, left + cellWidth, bottom)
        }
    }

    @Suppress("unused")
    private fun addLegacyAiPanel(totalWidth: Float, margin: Float) {
        val display = when {
            aiStatus.isNotEmpty() -> aiStatus
            aiPrompt.isEmpty() -> "Nhập yêu cầu cho AI"
            else -> aiPrompt.takeLast(80)
        }
        addKey("ai-input", display, KeyAction.OpenAi, margin, 5f * density, totalWidth - margin, translationInputHeight - 5f * density)
        val top = translationInputHeight + 4f * density
        val bottom = translationInputHeight + toolbarHeight - 4f * density
        val gap = 5f * density
        val backWidth = 50f * density
        val sendWidth = 78f * density
        addKey("toolbar-back", "", KeyAction.CloseAi, margin, top, margin + backWidth, bottom)
        addKey("ai-tone", aiToneLabel, KeyAction.OpenAi, margin + backWidth + gap, top, totalWidth - margin - sendWidth - gap, bottom)
        addKey("ai-send", "Gửi AI", KeyAction.SendAi, totalWidth - margin - sendWidth, top, totalWidth - margin, bottom)
    }

    private fun addAiPanel(totalWidth: Float, margin: Float) {
        addKey("ai-input", "", KeyAction.OpenAi, margin, 5f * density, totalWidth - margin, translationInputHeight - 5f * density)
        val top = translationInputHeight + 4f * density
        val bottom = translationInputHeight + toolbarHeight - 4f * density
        val gap = 4f * density
        val closeWidth = 48f * density
        val cellWidth = (totalWidth - margin * 2 - closeWidth - gap * 3) / 3f
        var left = margin
        addKey("toolbar-back", "", if (voicePanel) KeyAction.CancelVoice else KeyAction.CloseAi, left, top, left + closeWidth, bottom)
        left += closeWidth + gap
        addKey("ai-tone", "", KeyAction.OpenAi, left, top, left + cellWidth, bottom)
        left += cellWidth + gap
        addKey("ai-mode", "", KeyAction.OpenAi, left, top, left + cellWidth, bottom)
        left += cellWidth + gap
        addKey("ai-mic", "", KeyAction.VoiceAi, left, top, totalWidth - margin, bottom)
    }

    private fun addTranslationPanel(totalWidth: Float, margin: Float) {
        val display = when {
            translationInput.isNotEmpty() -> translationInput.takeLast(48)
            translationStatus.isNotEmpty() -> translationStatus
            else -> "Nhập vào đây để dịch"
        }
        addKey(
            "translate-input",
            display,
            KeyAction.OpenTranslator,
            margin,
            5f * density,
            totalWidth - margin,
            translationInputHeight - 5f * density,
        )
        val top = translationInputHeight + 4f * density
        val bottom = translationInputHeight + toolbarHeight - 4f * density
        val gap = 4f * density
        val closeWidth = 48f * density
        val cellWidth = (totalWidth - margin * 2 - closeWidth - gap * 4) / 4f
        var left = margin
        addKey("toolbar-back", "", if (voicePanel) KeyAction.CancelVoice else KeyAction.CloseTranslator, left, top, left + closeWidth, bottom)
        left += closeWidth + gap
        addKey("translate-source", translationSourceLabel, KeyAction.CycleTranslationSource, left, top, left + cellWidth, bottom)
        left += cellWidth + gap
        addKey("translate-swap", "⇄", KeyAction.SwapTranslationLanguages, left, top, left + cellWidth, bottom)
        left += cellWidth + gap
        addKey("translate-target", translationTargetLabel, KeyAction.CycleTranslationTarget, left, top, left + cellWidth, bottom)
        left += cellWidth + gap
        addKey("translate-mic", "", KeyAction.VoiceTranslation, left, top, totalWidth - margin, bottom)
    }

    private fun addVoicePanel(totalWidth: Float, totalHeight: Float, margin: Float) {
        val buttonWidth = 104f * density
        val bottom = totalHeight - bottomOffsetDp * density - margin
        addKey(
            "voice-toggle",
            "",
            KeyAction.ToggleVoicePause,
            margin,
            keyboardTop + margin,
            totalWidth - margin,
            bottom - 68f * density,
        )
        addKey(
            "voice-cancel",
            "Hủy",
            KeyAction.CancelVoice,
            (totalWidth - buttonWidth) / 2f,
            bottom - 60f * density,
            (totalWidth + buttonWidth) / 2f,
            bottom - 12f * density,
        )
        if (voicePaused) {
            val backspaceWidth = 56f * density
            addKey(
                "voice-backspace",
                "⌫",
                KeyAction.Backspace,
                totalWidth - margin - backspaceWidth,
                bottom - 60f * density,
                totalWidth - margin,
                bottom - 12f * density,
            )
        }
    }

    private fun drawVoicePanel(canvas: Canvas) {
        val margin = 8f * density
        val top = keyboardTop + margin
        val bottom = height - bottomOffsetDp * density - margin
        val card = RectF(margin, top, width - margin, bottom)
        keyPaint.style = Paint.Style.FILL
        keyPaint.color = themePalette.specialKey
        canvas.drawRoundRect(card, 20f * density, 20f * density, keyPaint)

        val centerX = width / 2f
        val waveY = bottom - 88f * density

        textPaint.textSize = 13f * density
        textPaint.color = themePalette.hint
        canvas.drawText(voiceStatus.ifEmpty { "Đang nghe…" }, centerX, top + 25f * density, textPaint)

        val liveText = voicePartial.ifBlank { "Hãy nói, nội dung sẽ hiện ở đây" }
        drawVoiceLiveText(
            canvas = canvas,
            text = liveText,
            left = margin + 18f * density,
            right = width - margin - 18f * density,
            firstBaseline = top + 55f * density,
            maxLines = 3,
        )

        val baseHeight = 10f * density
        repeat(5) { index ->
            val distance = kotlin.math.abs(index - 2)
            val factor = (1f - distance * 0.18f).coerceAtLeast(0.55f)
            val barHeight = baseHeight + 34f * density * voiceLevel * factor
            val x = centerX + (index - 2) * 13f * density
            keyPaint.color = themePalette.accent
            canvas.drawRoundRect(
                RectF(x - 3f * density, waveY - barHeight / 2f, x + 3f * density, waveY + barHeight / 2f),
                3f * density,
                3f * density,
                keyPaint,
            )
        }

        textPaint.textSize = 13f * density
        textPaint.color = themePalette.accent
        canvas.drawText(if (voicePaused) "Nhấn để tiếp tục" else "Nhấn để tạm dừng", centerX, bottom - 66f * density, textPaint)
        textPaint.textSize = 22f * density
        textPaint.color = themePalette.text
    }

    private fun drawVoiceLiveText(
        canvas: Canvas,
        text: String,
        left: Float,
        right: Float,
        firstBaseline: Float,
        maxLines: Int,
    ) {
        val availableWidth = (right - left).coerceAtLeast(1f)
        textPaint.textSize = 18f * density
        textPaint.color = if (voicePartial.isBlank()) themePalette.hint else themePalette.text
        val previousAlign = textPaint.textAlign
        textPaint.textAlign = Paint.Align.LEFT

        val lines = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty()) {
            var count = textPaint.breakText(remaining, true, availableWidth, null).coerceAtLeast(1)
            if (count < remaining.length) {
                val wordBreak = remaining.lastIndexOf(' ', count - 1)
                if (wordBreak > 0) count = wordBreak
            }
            lines += remaining.take(count).trim()
            remaining = remaining.drop(count).trimStart()
        }
        val visibleLines = lines.takeLast(maxLines)
        val lineHeight = 22f * density
        visibleLines.forEachIndexed { index, line ->
            val prefix = if (index == 0 && lines.size > maxLines) "…" else ""
            canvas.drawText(prefix + line, left, firstBaseline + index * lineHeight, textPaint)
        }
        textPaint.textAlign = previousAlign
    }

    private fun addSuggestionToolbar(totalWidth: Float, margin: Float, bottom: Float) {
        val gap = 5f * density
        val edgeWidth = 50f * density
        val middleLeft = margin + edgeWidth + gap
        val middleRight = totalWidth - margin - edgeWidth - gap
        val candidateWidth = (middleRight - middleLeft - gap * 2) / 3f
        addKey("toolbar-back", "", KeyAction.HideKeyboard, margin, 4f * density, margin + edgeWidth, bottom)
        suggestions.forEachIndexed { index, value ->
            val left = middleLeft + index * (candidateWidth + gap)
            addKey("suggestion-$index", value, KeyAction.SelectSuggestion(value), left, 4f * density, left + candidateWidth, bottom)
        }
        addKey("toolbar-emoji", "", KeyAction.ToggleEmoji, totalWidth - margin - edgeWidth, 4f * density, totalWidth - margin, bottom)
    }

    private fun addEmojiPanel(totalWidth: Float, rowCount: Int, rowHeight: Float, margin: Float, gap: Float) {
        val categories = listOf("◷" to KeyAction.SelectEmojiGroup(0)) +
            EmojiCatalog.groups.mapIndexed { index, group -> group.icon to KeyAction.SelectEmojiGroup(index + 1) }
        val categoryRow = rowCount - 1
        addActionRow(categories, categoryRow, rowHeight, margin, gap)
        val emojiRows = rowCount - 1
        val emojis = if (emojiGroup == 0) EmojiRecentStore.read(context) else EmojiCatalog.groups[emojiGroup - 1].values
        emojis.take(emojiRows * 10).chunked(10).forEachIndexed { row, values ->
            addTextRow(values, row, rowHeight, margin, gap)
        }
    }

    @Suppress("unused")
    private fun addLegacyClipboardPanel(totalWidth: Float, rowCount: Int, rowHeight: Float, margin: Float, gap: Float) {
        capturePrimaryClipboard()
        val clipboardEntries = ClipboardHistoryStore.readEntries(context)
        val history = if (clipboardTab == 0) clipboardEntries.map { it.text } else NoteStore.read(context)
        val columns = 2
        val contentRows = rowCount - 1
        val values = history.take(columns * contentRows)
        if (values.isEmpty()) {
            val top = keyboardTop + margin
            val bottom = keyboardTop + margin + contentRows * (rowHeight + gap) - gap
            val label = if (clipboardTab == 0) "Clipboard trống" else "Chưa có ghi chú"
            addKey("clipboard-empty", label, KeyAction.ToggleClipboard, margin, top, totalWidth - margin, bottom)
        } else {
            values.chunked(columns).forEachIndexed { row, items ->
                val actions = items.map { text ->
                    val pinned = clipboardTab == 0 && clipboardEntries.firstOrNull { it.text == text }?.pinned == true
                    val prefix = if (pinned) "★ " else ""
                    "$prefix${text.replace(Regex("\\s+"), " ").take(22)}" to KeyAction.CommitText(text)
                }
                addActionRow(actions, row, rowHeight, margin, gap)
            }
        }
        val tabs = mutableListOf<Pair<String, KeyAction>>(
            "▣" to KeyAction.SelectClipboardTab(0),
            "✎" to KeyAction.SelectClipboardTab(1),
        )
        if (clipboardTab == 1) tabs += "⚙︎" to KeyAction.OpenClipboardManager
        addActionRow(tabs, rowCount - 1, rowHeight, margin, gap)
    }

    private fun addClipboardPanel(totalWidth: Float, rowCount: Int, rowHeight: Float, margin: Float, gap: Float) {
        capturePrimaryClipboard()
        val clipboardEntries = ClipboardHistoryStore.readEntries(context)
        val history = if (clipboardTab == 0) clipboardEntries.map { it.text } else NoteStore.read(context)
        val columns = 2
        val contentRows = rowCount - 1
        val values = history.take(columns * contentRows)
        if (values.isEmpty()) {
            val top = keyboardTop + margin
            val bottom = keyboardTop + margin + contentRows * (rowHeight + gap) - gap
            val label = if (clipboardTab == 0) "Clipboard trống" else "Chưa có ghi chú"
            addKey("clipboard-empty", label, KeyAction.ToggleClipboard, margin, top, totalWidth - margin, bottom)
        } else {
            val keyWidth = (totalWidth - margin * 2 - gap) / columns
            values.chunked(columns).forEachIndexed { row, items ->
                val top = keyboardTop + margin + row * (rowHeight + gap)
                items.forEachIndexed { column, value ->
                    val pinned = clipboardTab == 0 && clipboardEntries.firstOrNull { it.text == value }?.pinned == true
                    val left = margin + column * (keyWidth + gap)
                    val state = if (pinned) "pinned" else "normal"
                    addKey("clipboard-item-$state-$row-$column", "", KeyAction.CommitText(value), left, top, left + keyWidth, top + rowHeight)
                }
            }
        }
        if (clipboardTab == 1) {
            val manageSize = 40f * density
            val manageRight = totalWidth - margin - 6f * density
            val manageTop = keyboardTop + margin + 6f * density
            addKey(
                "clipboard-manage", "", KeyAction.OpenClipboardManager,
                manageRight - manageSize, manageTop, manageRight, manageTop + manageSize,
            )
        }
        val actions = listOf<KeyAction>(
            KeyAction.SelectClipboardTab(0),
            KeyAction.SelectClipboardTab(1),
        )
        val tabWidth = (totalWidth - margin * 2 - gap * (actions.size - 1)) / actions.size
        val tabTop = keyboardTop + margin + (rowCount - 1) * (rowHeight + gap)
        actions.forEachIndexed { index, action ->
            val left = margin + index * (tabWidth + gap)
            val id = "clipboard-tab-${(action as KeyAction.SelectClipboardTab).index}"
            addKey(id, "", action, left, tabTop, left + tabWidth, tabTop + rowHeight)
        }
    }

    private fun addTextRow(values: List<String>, row: Int, rowHeight: Float, margin: Float, gap: Float) {
        val keyWidth = (width - margin * 2 - gap * (values.size - 1)) / values.size
        val top = keyboardTop + margin + row * (rowHeight + gap)
        values.forEachIndexed { index, value ->
            val left = margin + index * (keyWidth + gap)
            addKey("text-$row-$index", value, KeyAction.CommitText(value), left, top, left + keyWidth, top + rowHeight)
        }
    }

    private fun addActionRow(values: List<Pair<String, KeyAction>>, row: Int, rowHeight: Float, margin: Float, gap: Float) {
        val keyWidth = (width - margin * 2 - gap * (values.size - 1)) / values.size
        val top = keyboardTop + margin + row * (rowHeight + gap)
        values.forEachIndexed { index, (label, action) ->
            val left = margin + index * (keyWidth + gap)
            addKey("action-$row-$index", label, action, left, top, left + keyWidth, top + rowHeight)
        }
    }

    private fun capturePrimaryClipboard() {
        val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip ?: return
        if (clip.itemCount == 0) return
        ClipboardHistoryStore.add(context, clip.getItemAt(0).coerceToText(context).toString())
    }


    private fun addActionCharacterRow(chars: String, row: Int, totalWidth: Float, rowHeight: Float, margin: Float, gap: Float) {
        val top = keyboardTop + margin + row * (rowHeight + gap)
        val actionWidth = totalWidth * 0.13f
        val available = totalWidth - margin * 2 - actionWidth * 2 - gap * (chars.length + 1)
        val keyWidth = available / chars.length
        val leadingLabel = when {
            !symbols -> "⇧"
            extendedSymbolsEnabled -> if (symbolPage == 0) "#+=" else "123"
            else -> "ABC"
        }
        val leadingAction = when {
            !symbols -> KeyAction.Shift
            extendedSymbolsEnabled -> KeyAction.ToggleSymbolPage
            else -> KeyAction.ToggleSymbols
        }
        addKey("shift", leadingLabel, leadingAction, margin, top, margin + actionWidth, top + rowHeight)
        chars.forEachIndexed { index, char ->
            val left = margin + actionWidth + gap + index * (keyWidth + gap)
            addKey("$row-$char", if (!symbols && shifted) char.uppercaseChar().toString() else char.toString(), KeyAction.Character(char), left, top, left + keyWidth, top + rowHeight)
        }
        addKey("backspace", "⌫", KeyAction.Backspace, totalWidth - margin - actionWidth, top, totalWidth - margin, top + rowHeight)
    }

    private fun addBottomRow(totalWidth: Float, row: Int, rowHeight: Float, margin: Float, gap: Float) {
        val top = keyboardTop + margin + row * (rowHeight + gap)
        val modeWidth = totalWidth * 0.15f
        val punctuationWidth = totalWidth * 0.10f
        val enterWidth = totalWidth * 0.15f
        val spaceLeft = margin + modeWidth + gap + punctuationWidth + gap
        val spaceRight = totalWidth - margin - enterWidth - gap - punctuationWidth - gap
        addKey("mode", if (symbols) "ABC" else "?123", KeyAction.ToggleSymbols, margin, top, margin + modeWidth, top + rowHeight)
        addKey("comma", ",", KeyAction.Character(','), margin + modeWidth + gap, top, spaceLeft, top + rowHeight)
        addKey("space", spaceLabel, KeyAction.Space, spaceLeft + gap, top, spaceRight, top + rowHeight)
        addKey("period", ".", KeyAction.Character('.'), spaceRight + gap, top, spaceRight + gap + punctuationWidth, top + rowHeight)
        addKey(
            "enter", "↵", KeyboardModeActionPolicy.bottomRightAction(aiMode),
            totalWidth - margin - enterWidth, top, totalWidth - margin, top + rowHeight,
        )
    }

    private fun addCharacterRow(chars: String, row: Int, insetStart: Float, insetEnd: Float, rowHeight: Float, margin: Float, gap: Float) {
        val available = width - margin * 2 - insetStart - insetEnd
        val keyWidth = (available - gap * (chars.length - 1)) / chars.length
        val top = keyboardTop + margin + row * (rowHeight + gap)
        chars.forEachIndexed { index, char ->
            val left = margin + insetStart + index * (keyWidth + gap)
            addKey("$row-$char", if (shifted) char.uppercaseChar().toString() else char.toString(), KeyAction.Character(char), left, top, left + keyWidth, top + rowHeight)
        }
    }

    private fun addKey(id: String, label: String, action: KeyAction, left: Float, top: Float, right: Float, bottom: Float) {
        keys += KeyGeometry(id, label, action, left, top, right, bottom)
    }

    private fun effectiveLeftPx(scaleX: Float): Float {
        val maximum = width * (1f - scaleX)
        return (leftOffsetDp * density).coerceIn(0f, maximum.coerceAtLeast(0f))
    }

    private fun toKeyboardX(screenX: Float): Float {
        val scaleX = widthPercent / 100f
        return (screenX - effectiveLeftPx(scaleX)) / scaleX
    }

    private fun drawResizeOverlay(canvas: Canvas, left: Float, scaleX: Float) {
        val right = left + width * scaleX
        val bottom = height - bottomOffsetDp * density
        val green = Color.rgb(34, 197, 94)
        keyPaint.color = Color.argb(90, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), keyPaint)
        keyPaint.color = green
        val stroke = 7f * density
        val arm = 30f * density
        keyPaint.strokeWidth = stroke
        keyPaint.strokeCap = Paint.Cap.ROUND
        listOf(left to 0f, right to 0f, left to bottom, right to bottom).forEachIndexed { index, (x, y) ->
            val inwardX = if (index % 2 == 0) 1f else -1f
            val inwardY = if (index < 2) 1f else -1f
            canvas.drawLine(x, y, x + inwardX * arm, y, keyPaint)
            canvas.drawLine(x, y, x, y + inwardY * arm, keyPaint)
        }
        canvas.drawRoundRect(RectF(left - stroke / 2, bottom / 2 - arm, left + stroke / 2, bottom / 2 + arm), stroke, stroke, keyPaint)
        canvas.drawRoundRect(RectF(right - stroke / 2, bottom / 2 - arm, right + stroke / 2, bottom / 2 + arm), stroke, stroke, keyPaint)
        canvas.drawRoundRect(RectF((left + right) / 2 - arm, -stroke / 2, (left + right) / 2 + arm, stroke / 2), stroke, stroke, keyPaint)
        canvas.drawRoundRect(RectF((left + right) / 2 - arm, bottom - stroke / 2, (left + right) / 2 + arm, bottom + stroke / 2), stroke, stroke, keyPaint)

        val centerX = (left + right) / 2f
        val centerY = bottom / 2f
        val radius = 27f * density
        val centers = listOf(centerX - 70f * density, centerX, centerX + 70f * density)
        centers.forEachIndexed { index, x ->
            keyPaint.color = if (index == 1) Color.WHITE else green
            canvas.drawCircle(x, centerY, radius, keyPaint)
        }
        popupTextPaint.color = themePalette.text
        val baseline = centerY - (popupTextPaint.ascent() + popupTextPaint.descent()) / 2f
        canvas.drawText("↻", centers[0], baseline, popupTextPaint)
        popupTextPaint.color = green
        canvas.drawText("↔", centers[1], baseline, popupTextPaint)
        popupTextPaint.color = Color.WHITE
        canvas.drawText("✓", centers[2], baseline, popupTextPaint)
    }

    private fun handleResizeTouch(event: MotionEvent): Boolean {
        val scaleX = widthPercent / 100f
        val left = effectiveLeftPx(scaleX)
        val right = left + width * scaleX
        val bottom = height - bottomOffsetDp * density
        val centerX = (left + right) / 2f
        val centerY = bottom / 2f
        val hit = 42f * density
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resizeDownX = event.x; resizeDownY = event.y
                startHeightDp = keyboardHeightDp; startBottomDp = bottomOffsetDp
                startWidthPercent = widthPercent; startLeftDp = leftOffsetDp
                fun near(x: Float, y: Float) = (event.x - x) * (event.x - x) + (event.y - y) * (event.y - y) <= hit * hit
                resizeDrag = when {
                    near(centerX - 70f * density, centerY) -> ResizeDrag.RESET
                    near(centerX + 70f * density, centerY) -> ResizeDrag.DONE
                    near(centerX, centerY) -> ResizeDrag.MOVE
                    near(left, 0f) -> ResizeDrag.TOP_LEFT
                    near(right, 0f) -> ResizeDrag.TOP_RIGHT
                    near(left, bottom) -> ResizeDrag.BOTTOM_LEFT
                    near(right, bottom) -> ResizeDrag.BOTTOM_RIGHT
                    event.x < left + hit -> ResizeDrag.LEFT
                    event.x > right - hit -> ResizeDrag.RIGHT
                    event.y < hit -> ResizeDrag.TOP
                    event.y > bottom - hit -> ResizeDrag.BOTTOM
                    else -> ResizeDrag.MOVE
                }
            }
            MotionEvent.ACTION_MOVE -> updateResizePreview(event.x - resizeDownX, event.y - resizeDownY)
            MotionEvent.ACTION_UP -> {
                when (resizeDrag) {
                    ResizeDrag.RESET -> {
                        keyboardHeightDp = 220; bottomOffsetDp = 0; widthPercent = 100; leftOffsetDp = 0
                    }
                    ResizeDrag.DONE -> KeyboardPreferences.setBoolean(context, KeyboardPreferences.ADJUSTMENT_MODE, false)
                    else -> updateResizePreview(event.x - resizeDownX, event.y - resizeDownY)
                }
                KeyboardPreferences.setKeyboardGeometry(context, keyboardHeightDp, bottomOffsetDp, widthPercent, leftOffsetDp)
                resizeDrag = null
            }
            MotionEvent.ACTION_CANCEL -> {
                keyboardHeightDp = startHeightDp; bottomOffsetDp = startBottomDp
                widthPercent = startWidthPercent; leftOffsetDp = startLeftDp
                resizeDrag = null; requestLayout(); invalidate()
            }
        }
        return true
    }

    private fun updateResizePreview(dx: Float, dy: Float) {
        val dxDp = dx / density
        val dyDp = dy / density
        when (resizeDrag) {
            ResizeDrag.MOVE -> {
                leftOffsetDp = (startLeftDp + dxDp).toInt().coerceAtLeast(0)
                bottomOffsetDp = (startBottomDp - dyDp).toInt().coerceIn(0, 80)
            }
            ResizeDrag.LEFT, ResizeDrag.TOP_LEFT, ResizeDrag.BOTTOM_LEFT -> {
                widthPercent = (startWidthPercent - dx / width * 100f).toInt().coerceIn(75, 100)
                leftOffsetDp = (startLeftDp + dxDp).toInt().coerceAtLeast(0)
            }
            ResizeDrag.RIGHT, ResizeDrag.TOP_RIGHT, ResizeDrag.BOTTOM_RIGHT ->
                widthPercent = (startWidthPercent + dx / width * 100f).toInt().coerceIn(75, 100)
            else -> Unit
        }
        when (resizeDrag) {
            ResizeDrag.TOP, ResizeDrag.TOP_LEFT, ResizeDrag.TOP_RIGHT ->
                keyboardHeightDp = (startHeightDp - dyDp).toInt().coerceIn(170, 280)
            ResizeDrag.BOTTOM, ResizeDrag.BOTTOM_LEFT, ResizeDrag.BOTTOM_RIGHT -> {
                keyboardHeightDp = (startHeightDp + dyDp).toInt().coerceIn(170, 280)
                bottomOffsetDp = (startBottomDp - dyDp).toInt().coerceIn(0, 80)
            }
            else -> Unit
        }
        val maxLeftDp = (width * (1f - widthPercent / 100f) / density).toInt().coerceAtLeast(0)
        leftOffsetDp = leftOffsetDp.coerceIn(0, maxLeftDp)
        requestLayout()
        invalidate()
    }

}
