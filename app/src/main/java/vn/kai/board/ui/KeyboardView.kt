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
import android.widget.Toast
import kotlin.math.cos
import kotlin.math.sin
import vn.kai.board.input.KeyAction
import vn.kai.board.input.KeyboardModeActionPolicy
import vn.kai.board.input.ShiftGesturePolicy
import vn.kai.board.input.SpaceCursorGesturePolicy
import vn.kai.board.input.LongPressSymbolMap
import vn.kai.board.input.EmojiCatalog
import vn.kai.board.input.ClipboardHistoryStore
import vn.kai.board.input.ClipboardEntry
import vn.kai.board.input.ClipboardEntryKind
import vn.kai.board.input.SmartClipboardClassifier
import vn.kai.board.input.NoteStore
import android.graphics.Bitmap
import android.util.LruCache
import vn.kai.board.input.EmojiRecentStore
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.ThemeMode
import vn.kai.board.settings.KeyboardColorStyle
import vn.kai.board.settings.KeyboardThemePalette
import vn.kai.board.touch.KeyGeometry
import vn.kai.board.touch.TouchTargetPolicy
import vn.kai.board.touch.TouchDispatcher
import vn.kai.board.touch.RepeatKeyState
import vn.kai.board.R

class KeyboardView(context: Context) : View(context) {
    var onKeyAction: (KeyAction) -> Unit = {}

    private enum class ResizeDrag { MOVE, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, RESET, DONE }
    private enum class Panel { NONE, EMOJI, CLIPBOARD }

    private data class ClipboardItemPopup(
        val entryId: String,
        val pinned: Boolean,
        val bar: RectF,
        val pinBtn: RectF,
        val deleteBtn: RectF,
    )

    private data class ClipboardClearPopup(
        val bar: RectF,
        val cancelBtn: RectF,
        val confirmBtn: RectF,
        val unpinnedCount: Int,
    )

    private val density = resources.displayMetrics.density
    private val toolbarHeight = 44f * density
    private val translationInputHeight = 54f * density
    private val suggestionMenuTimeoutMs = 2_200L
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyLabelTextSize = 22f * density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = keyLabelTextSize
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
    private var capsLocked = false
    private var lastShiftTapMs = -1L
    private var symbols = false
    private var panel = Panel.NONE
    private var emojiGroup = 0
    private var emojiSearchActive = false
    private var emojiSearchQuery = ""
    private var clipboardTab = 0
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener { capturePrimaryClipboard() }
    private val clipboardThumbCache = object : LruCache<String, Bitmap>(8) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && !oldValue.isRecycled) oldValue.recycle()
        }
    }
    /** Gboard-style compact floating bar for one clipboard item (pin / delete). */
    private var clipboardItemPopup: ClipboardItemPopup? = null
    /** Compact confirm bar for clear-all-unpinned. */
    private var clipboardClearPopup: ClipboardClearPopup? = null
    /**
     * Long-press opens the popup while the finger is still down; the following ACTION_UP
     * must not dismiss it (Gboard keeps the bar until a real tap).
     */
    private var suppressClipboardPopupUp = false
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
    private var privateSession = false
    private var suggestions: List<String> = emptyList()
    private var suggestionMenuActive = false
    private var translationMode = false
    private var translationSourceLabel = "Tiếng Việt"
    private var translationTargetLabel = "Tiếng Anh"
    private var translationInput = ""
    private var translationCursor = 0
    private var translationStatus = ""
    private var aiMode = false
    private var aiPrompt = ""
    private var aiCursor = 0
    private var aiStatus = ""
    private var aiSuggestions: List<String> = emptyList()
    private var aiAnimationFrame = 0
    private var aiToneLabel = "Tự động ngẫu nhiên"
    private var voicePanel = false
    private var voiceStatus = ""
    private var voicePartial = ""
    private var voiceLevel = 0f
    private var voicePaused = false
    private var spaceLabel = "Tiếng Việt"
    private var leadingPunctuation = ','
    private var keyboardHeightDp = 220
    private var keyRadiusDp = 7
    private var keyBorderEnabled = false
    private var keyBorderWidthDp = 1
    private var numberRowEnabled = false
    private var numericMode = false
    private var numericDecimal = false
    private var numericSigned = false
    private var numericPhone = false
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
    /**
     * Gboard-style resize:
     * - While adjustmentMode is on, the view measures at max size once (room to grow).
     * - During drag, only preview* + rebuildKeys + invalidate (no requestLayout) → 60fps follow.
     * - On finger up / ✓, commit geometry and remeasure to the final size.
     */
    private var previewHeightDp = 220
    private var previewBottomDp = 0
    private var previewWidthPercent = 100
    private var previewLeftDp = 0
    private var resizeDragging = false
    private val resizeMinHeightDp = 170
    private val resizeMaxHeightDp = 280
    private val resizeMaxBottomDp = 80
    private val resizeMinWidthPercent = 75
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (resizeDragging && isGeometryPreferenceKey(key)) {
            return@OnSharedPreferenceChangeListener
        }
        post { reloadPreferences() }
    }

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val repeatPointerId = repeatState.pointerId
            if (repeatPointerId != null && pointers[repeatPointerId]?.key?.action == KeyAction.Backspace) {
                onKeyAction(KeyAction.Backspace)
                postDelayed(this, repeatState.nextDelayMs())
            }
        }
    }
    private val restoreToolbarRunnable = Runnable {
        if (!suggestionMenuActive) return@Runnable
        suggestionMenuActive = false
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }
    private val hideAiSuggestionsRunnable = Runnable {
        if (aiSuggestions.isEmpty()) return@Runnable
        aiSuggestions = emptyList()
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
        removeCallbacks(hideAiSuggestionsRunnable)
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
        themePalette = KeyboardThemePalette.resolve(context, dark)
        val themeTypeface = Typeface.create(themePalette.fontFamily, Typeface.NORMAL)
        textPaint.typeface = themeTypeface
        popupTextPaint.typeface = themeTypeface
        hintPaint.typeface = themeTypeface
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
        // Keep preview in sync when not mid-gesture so draw/hit-test use current size.
        if (!resizeDragging) {
            previewHeightDp = keyboardHeightDp
            previewBottomDp = bottomOffsetDp
            previewWidthPercent = widthPercent
            previewLeftDp = leftOffsetDp
        }
        updateBackgroundGradient()
        if (!extendedSymbolsEnabled) symbolPage = 0
        rebuildKeysForActiveGeometry()
        requestLayout()
        invalidate()
    }

    /** Lay out keys for the size currently being shown (preview in adjust mode). */
    private fun rebuildKeysForActiveGeometry() {
        val hDp = activeHeightDp()
        val bDp = activeBottomDp()
        val layoutH = if (adjustmentMode) {
            estimatedTotalHeightPx(hDp, bDp)
        } else {
            height.toFloat()
        }
        if (layoutH <= 0f || width <= 0) return
        rebuildKeys(width.toFloat(), layoutH)
    }

    /** Total keyboard height in px for the given geometry (same formula as onMeasure). */
    private fun estimatedTotalHeightPx(heightDp: Int, bottomDp: Int): Float {
        val baseHeight = heightDp * density
        val extraNumberRow = if (numberRowEnabled && !numericMode) {
            val gap = 5f * density
            val margin = 5f * density
            (baseHeight - margin * 2 - gap * 3) / 4f + gap
        } else 0f
        val featurePanelExtra = when {
            aiMode -> translationInputHeight
            translationMode -> translationInputHeight
            else -> 0f
        }
        return baseHeight + extraNumberRow + toolbarHeight + featurePanelExtra + bottomDp * density
    }

    private fun activeHeightDp(): Int =
        if (resizeDragging || adjustmentMode) previewHeightDp else keyboardHeightDp

    private fun activeBottomDp(): Int =
        if (resizeDragging || adjustmentMode) previewBottomDp else bottomOffsetDp

    private fun activeWidthPercent(): Int =
        if (resizeDragging || adjustmentMode) previewWidthPercent else widthPercent

    private fun activeLeftDp(): Int =
        if (resizeDragging || adjustmentMode) previewLeftDp else leftOffsetDp

    /** Content box of the keyboard inside the (possibly larger) adjustment viewport. */
    private fun contentFrame(): RectF {
        val scaleX = activeWidthPercent() / 100f
        val left = leftPxFor(activeLeftDp(), scaleX)
        val contentH = estimatedTotalHeightPx(activeHeightDp(), activeBottomDp())
        val top = (height - contentH).coerceAtLeast(0f)
        val bottom = (height - activeBottomDp() * density).coerceAtLeast(top + 1f)
        return RectF(left, top, left + width * scaleX, bottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Gboard-style: during resize mode hold max viewport so drag never needs requestLayout.
        val heightDp = if (adjustmentMode) resizeMaxHeightDp else keyboardHeightDp
        val bottomDp = if (adjustmentMode) resizeMaxBottomDp else bottomOffsetDp
        val requestedHeight = estimatedTotalHeightPx(heightDp, bottomDp).toInt()
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val cappedHeight = KeyboardHeightPolicy.cap(
            requestedPixels = requestedHeight,
            screenHeightPixels = resources.displayMetrics.heightPixels,
            landscape = landscape,
        )
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            resolveSize(cappedHeight, heightMeasureSpec),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateBackgroundGradient(w, h)
        if (adjustmentMode) {
            rebuildKeysForActiveGeometry()
        } else {
            rebuildKeys(w.toFloat(), h.toFloat())
        }
    }

    fun setShifted(value: Boolean) {
        shifted = value
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }

    fun setPrivateSession(enabled: Boolean) {
        if (privateSession == enabled) return
        privateSession = enabled
        if (enabled) {
            suggestions = emptyList()
            suggestionMenuActive = false
            // Leave symbols/ABC as-is; only close privacy-sensitive panels.
            if (panel == Panel.CLIPBOARD || panel == Panel.EMOJI || aiMode) {
                panel = Panel.NONE
                aiMode = false
                emojiSearchActive = false
                emojiSearchQuery = ""
            }
        }
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }

    fun setCapsLocked(value: Boolean) {
        capsLocked = value
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
        if (privateSession) {
            removeCallbacks(restoreToolbarRunnable)
            suggestions = emptyList()
            suggestionMenuActive = false
            rebuildKeys(width.toFloat(), height.toFloat())
            invalidate()
            return
        }
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

    fun setTranslationState(
        enabled: Boolean,
        source: String,
        target: String,
        input: String,
        cursor: Int = input.length,
        status: String = "",
    ) {
        val sizeChanged = translationMode != enabled
        val labelsChanged = translationSourceLabel != source || translationTargetLabel != target
        var mediaCleared = false
        translationMode = enabled
        translationSourceLabel = source
        translationTargetLabel = target
        translationInput = input
        translationCursor = cursor.coerceIn(0, input.length)
        translationStatus = status
        suggestionMenuActive = false
        removeCallbacks(restoreToolbarRunnable)
        // Opening Dịch from clipboard/emoji must leave media panels so the body is the letter keys.
        if (enabled && (panel != Panel.NONE || symbols || voicePanel || emojiSearchActive)) {
            dismissClipboardPopups()
            panel = Panel.NONE
            symbols = false
            symbolPage = 0
            emojiSearchActive = false
            emojiSearchQuery = ""
            voicePanel = false
            mediaCleared = true
        }
        // Cursor-only updates just repaint the field (smooth drag); rebuild when chrome changes.
        if (sizeChanged || labelsChanged || mediaCleared) {
            rebuildKeys(width.toFloat(), height.toFloat())
            if (sizeChanged) requestLayout()
        }
        invalidate()
    }

    fun setAiState(
        enabled: Boolean,
        prompt: String,
        cursor: Int,
        status: String,
        toneLabel: String,
        suggestions: List<String> = emptyList(),
    ) {
        val sizeChanged = aiMode != enabled
        val nextSuggestions = suggestions.filter(String::isNotBlank).distinct().take(3)
        val suggestionsChanged = aiSuggestions != nextSuggestions
        val toneChanged = aiToneLabel != toneLabel
        var mediaCleared = false
        aiMode = enabled
        aiPrompt = prompt
        aiCursor = cursor.coerceIn(0, prompt.length)
        aiStatus = status
        aiSuggestions = nextSuggestions
        removeCallbacks(hideAiSuggestionsRunnable)
        if (enabled && aiSuggestions.isNotEmpty()) {
            postDelayed(hideAiSuggestionsRunnable, suggestionMenuTimeoutMs)
        }
        aiToneLabel = toneLabel
        removeCallbacks(aiAnimationRunnable)
        if (isAiProcessing()) post(aiAnimationRunnable) else aiAnimationFrame = 0
        suggestionMenuActive = false
        removeCallbacks(restoreToolbarRunnable)
        // Opening AI from clipboard/emoji must leave media panels so the body is the letter keys + AI chrome.
        if (enabled && (panel != Panel.NONE || symbols || voicePanel || emojiSearchActive)) {
            dismissClipboardPopups()
            panel = Panel.NONE
            symbols = false
            symbolPage = 0
            emojiSearchActive = false
            emojiSearchQuery = ""
            voicePanel = false
            mediaCleared = true
        }
        if (sizeChanged || suggestionsChanged || toneChanged || mediaCleared) {
            rebuildKeys(width.toFloat(), height.toFloat())
            if (sizeChanged) requestLayout()
        }
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
        // Always reset label metrics — clipboard/voice helpers temporarily shrink textPaint.
        textPaint.textSize = keyLabelTextSize
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = themePalette.text
        popupTextPaint.color = textPaint.color
        hintPaint.color = themePalette.hint
        // Gboard-style draw: keys laid out at active size, bottom-aligned in viewport, width via scaleX.
        val drawWidthPercent = activeWidthPercent()
        val drawLeftDp = activeLeftDp()
        val drawHeightDp = activeHeightDp()
        val drawBottomDp = activeBottomDp()
        val scaleX = drawWidthPercent / 100f
        val translateX = leftPxFor(drawLeftDp, scaleX)
        val contentH = estimatedTotalHeightPx(drawHeightDp, drawBottomDp)
        val translateY = height - contentH
        // In adjust mode only fill the keyboard band so the raised/shrunken frame can float.
        if (adjustmentMode) {
            val bandTop = translateY.coerceAtLeast(0f)
            if (backgroundGradient != null) {
                keyPaint.shader = backgroundGradient
                canvas.drawRect(0f, bandTop, width.toFloat(), height.toFloat(), keyPaint)
                keyPaint.shader = null
            } else {
                keyPaint.color = themePalette.background
                canvas.drawRect(0f, bandTop, width.toFloat(), height.toFloat(), keyPaint)
            }
        } else if (backgroundGradient != null) {
            keyPaint.shader = backgroundGradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), keyPaint)
            keyPaint.shader = null
        } else {
            canvas.drawColor(themePalette.background)
        }
        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleX, 1f)
        textPaint.textScaleX = 1f / scaleX
        popupTextPaint.textScaleX = 1f / scaleX
        hintPaint.textScaleX = 1f / scaleX
        val radius = keyRadiusDp * density
        if (voicePanel) drawVoicePanel(canvas)
        keys.forEach { key ->
            val toolbarKey = key.id.startsWith("toolbar-") || key.id.startsWith("translate-") && key.id != "translate-input" || key.id.startsWith("ai-") && key.id != "ai-input"
            val clipboardUiKey = key.id.startsWith("clipboard-")
            val floatingIcon = toolbarKey ||
                key.id.startsWith("suggestion-") ||
                key.id.startsWith("command-suggestion-") ||
                clipboardUiKey ||
                key.id == "voice-toggle"
            val privacyChrome = key.id == "toolbar-privacy-banner" || key.id == "toolbar-privacy-lock"
            keyPaint.color = when {
                pointers.values.any { it.key == key } -> themePalette.pressed
                key.id == "enter" && themePalette.actionKey != null -> themePalette.actionKey ?: themePalette.specialKey
                key.id == "translate-input" || key.id == "ai-input" || key.action is KeyAction.Character || key.action is KeyAction.CommitText || key.action == KeyAction.Space -> themePalette.key
                else -> themePalette.specialKey
            }
            if (!floatingIcon && !privacyChrome) {
                val keyRadius = if (key.id == "translate-input" || key.id == "ai-input") 16f * density else radius
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
                key.id == "clipboard-clear-unpinned" -> drawClipboardClearButton(canvas, key)
                key.id == "toolbar-back" -> drawBackIcon(canvas, key)
                key.id == "toolbar-privacy-banner" -> drawPrivateSessionBanner(canvas, key)
                key.id == "toolbar-privacy-lock" -> drawPrivacyLockIcon(canvas, key)
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
                key.id == "ai-input" -> drawFeatureInputField(
                    canvas,
                    key,
                    text = aiPrompt,
                    cursor = aiCursor,
                    placeholder = "Nhập yêu cầu cho AI",
                    status = if (aiStatus.isNotEmpty()) {
                        if (isAiProcessing()) "AI đang xử lý${".".repeat(aiAnimationFrame)}" else aiStatus
                    } else "",
                    reserveTrailing = if (isAiProcessing()) 76f * density else 30f * density,
                )
                key.id == "translate-input" -> drawFeatureInputField(
                    canvas,
                    key,
                    text = translationInput,
                    cursor = translationCursor,
                    placeholder = "Nhập vào đây để dịch",
                    status = translationStatus,
                    reserveTrailing = 30f * density,
                )
                key.id == "shift" -> drawShiftIcon(canvas, key)
                key.id == "enter" -> drawEnterIcon(canvas, key)
                key.id.startsWith("suggestion-") || key.id.startsWith("command-suggestion-") ->
                    drawSuggestionLabel(canvas, key)
                else -> canvas.drawText(key.label, key.centerX, baseline, textPaint)
            }
            if (longPressSymbolsEnabled && !symbols && key.action is KeyAction.Character) {
                LongPressSymbolMap.forKey(key.action.value)?.let { symbol ->
                    canvas.drawText(symbol.toString(), key.right - 6f * density, key.top + 14f * density, hintPaint)
                }
            }
        }
        previewKey?.takeIf { popupEnabled && it.action is KeyAction.Character }?.let { drawPreview(canvas, it) }
        // Gboard-style compact popups (keyboard space, same transform as keys).
        clipboardItemPopup?.let { drawClipboardItemPopup(canvas, it) }
        clipboardClearPopup?.let { drawClipboardClearPopup(canvas, it) }
        canvas.restore()
        textPaint.textScaleX = 1f
        popupTextPaint.textScaleX = 1f
        hintPaint.textScaleX = 1f
        if (adjustmentMode) {
            drawResizeOverlay(canvas, contentFrame())
        }
    }

    fun setLeadingPunctuation(value: Char) {
        if (leadingPunctuation == value) return
        leadingPunctuation = value
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }

    fun setNumericMode(enabled: Boolean, decimal: Boolean = false, signed: Boolean = false, phone: Boolean = false) {
        if (numericMode == enabled && numericDecimal == decimal && numericSigned == signed && numericPhone == phone) return
        numericMode = enabled
        numericDecimal = decimal
        numericSigned = signed
        numericPhone = phone
        if (enabled) {
            symbols = false
            panel = Panel.NONE
        }
        rebuildKeys(width.toFloat(), height.toFloat())
        requestLayout()
        invalidate()
    }

    private fun drawPrivateSessionBanner(canvas: Canvas, key: KeyGeometry) {
        val rect = RectF(key.left + 2f * density, key.top + 4f * density, key.right - 2f * density, key.bottom - 4f * density)
        val fill = Color.argb(
            if (dark) 55 else 40,
            Color.red(themePalette.accent),
            Color.green(themePalette.accent),
            Color.blue(themePalette.accent),
        )
        keyPaint.style = Paint.Style.FILL
        keyPaint.color = fill
        canvas.drawRoundRect(rect, 10f * density, 10f * density, keyPaint)
        keyPaint.style = Paint.Style.STROKE
        keyPaint.strokeWidth = 1.2f * density
        keyPaint.color = Color.argb(
            if (dark) 200 else 160,
            Color.red(themePalette.accent),
            Color.green(themePalette.accent),
            Color.blue(themePalette.accent),
        )
        canvas.drawRoundRect(rect, 10f * density, 10f * density, keyPaint)
        keyPaint.style = Paint.Style.FILL

        val oldSize = textPaint.textSize
        val oldColor = textPaint.color
        val oldAlign = textPaint.textAlign
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = themePalette.accent
        // Prefer short label; fall back to shorter width-aware text.
        var label = key.label
        textPaint.textSize = 12.5f * density
        val maxWidth = rect.width() - 12f * density
        if (textPaint.measureText(label) > maxWidth) {
            label = context.getString(R.string.private_session_banner_short)
            textPaint.textSize = 12f * density
        }
        if (textPaint.measureText(label) > maxWidth) {
            textPaint.textSize = 11f * density
        }
        val baseline = key.centerY - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(label, key.centerX, baseline, textPaint)
        textPaint.textSize = oldSize
        textPaint.color = oldColor
        textPaint.textAlign = oldAlign
    }

    private fun drawPrivacyLockIcon(canvas: Canvas, key: KeyGeometry) {
        val cx = key.centerX
        val cy = key.centerY
        prepareMonoIconPaint()
        keyPaint.color = themePalette.accent
        keyPaint.strokeWidth = 1.7f * density
        keyPaint.style = Paint.Style.STROKE
        // Shackle
        canvas.drawArc(
            RectF(cx - 5.5f * density, cy - 9f * density, cx + 5.5f * density, cy - 1f * density),
            200f,
            140f,
            false,
            keyPaint,
        )
        // Body
        keyPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            RectF(cx - 7f * density, cy - 2f * density, cx + 7f * density, cy + 8f * density),
            2.2f * density,
            2.2f * density,
            keyPaint,
        )
        // Keyhole
        keyPaint.color = themePalette.background
        canvas.drawCircle(cx, cy + 1.2f * density, 1.6f * density, keyPaint)
        canvas.drawRect(
            cx - 0.7f * density,
            cy + 1.5f * density,
            cx + 0.7f * density,
            cy + 5.2f * density,
            keyPaint,
        )
        keyPaint.style = Paint.Style.FILL
        keyPaint.color = themePalette.text
    }

    private fun drawSuggestionLabel(canvas: Canvas, key: KeyGeometry) {
        val previousSize = textPaint.textSize
        textPaint.textSize = 19f * density
        val baseline = key.centerY - (textPaint.ascent() + textPaint.descent()) / 2f
        val horizontalOffset = when {
            key.id.endsWith("-0") -> -6f * density
            key.id.endsWith("-2") -> 6f * density
            else -> 0f
        }
        canvas.drawText(key.label, key.centerX + horizontalOffset, baseline, textPaint)
        textPaint.textSize = previousSize
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
        val action = key.action as? KeyAction.CommitClipboard
        val pinned = key.id.contains("-pinned-")
        val isImage = action?.contentKind == ClipboardEntryKind.IMAGE.name
        if (isImage && action != null) {
            drawClipboardImageItem(canvas, key, action, pinned)
            return
        }
        val content = (action?.value ?: (key.action as? KeyAction.CommitText)?.value).orEmpty().replace(Regex("\\s+"), " ").trim()
        val lineLimit = ((key.right - key.left) / (7.5f * density)).toInt().coerceAtLeast(10)
        val first = content.take(lineLimit)
        val remainder = content.drop(first.length).trimStart()
        val second = remainder.take(lineLimit - 1) + if (remainder.length >= lineLimit) "…" else ""
        val oldAlign = textPaint.textAlign
        val oldSize = textPaint.textSize
        val oldColor = textPaint.color
        try {
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = 14f * density
            textPaint.color = if (dark) Color.rgb(248, 250, 252) else Color.rgb(17, 24, 39)
            val left = key.left + 13f * density
            val hasKind = !action?.kindLabel.isNullOrEmpty()
            if (hasKind) {
                textPaint.textSize = 10f * density
                textPaint.color = clipboardAccentColor()
                canvas.drawText(action!!.kindLabel, left, key.top + 14f * density, textPaint)
                textPaint.textSize = 14f * density
                textPaint.color = if (dark) Color.rgb(248, 250, 252) else Color.rgb(17, 24, 39)
            }
            val firstY = if (hasKind) {
                key.centerY + 4f * density
            } else if (second.isEmpty()) {
                key.centerY - (textPaint.ascent() + textPaint.descent()) / 2f
            } else {
                key.centerY - 3f * density
            }
            canvas.drawText(first, left, firstY, textPaint)
            if (second.isNotEmpty()) canvas.drawText(second, left, firstY + 18f * density, textPaint)
            if (pinned) drawClipboardPin(canvas, key)
        } finally {
            textPaint.textAlign = oldAlign
            textPaint.textSize = oldSize
            textPaint.color = oldColor
        }
    }

    private fun drawClipboardImageItem(
        canvas: Canvas,
        key: KeyGeometry,
        action: KeyAction.CommitClipboard,
        pinned: Boolean,
    ) {
        val oldAlign = textPaint.textAlign
        val oldSize = textPaint.textSize
        val oldColor = textPaint.color
        try {
            val pad = 8f * density
            val thumbLeft = key.left + pad
            val thumbTop = key.top + pad
            val thumbSize = (key.bottom - key.top - pad * 2).coerceAtMost((key.right - key.left) * 0.42f)
            val thumbRect = RectF(thumbLeft, thumbTop, thumbLeft + thumbSize, thumbTop + thumbSize)
            val bitmap = clipboardThumbFor(action)
            if (bitmap != null && !bitmap.isRecycled) {
                val src = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                val scale = maxOf(thumbSize / bitmap.width, thumbSize / bitmap.height)
                val dw = bitmap.width * scale
                val dh = bitmap.height * scale
                val dx = thumbRect.centerX() - dw / 2f
                val dy = thumbRect.centerY() - dh / 2f
                canvas.save()
                canvas.clipRect(thumbRect)
                canvas.drawBitmap(bitmap, src, RectF(dx, dy, dx + dw, dy + dh), keyPaint)
                canvas.restore()
                keyPaint.style = Paint.Style.STROKE
                keyPaint.strokeWidth = 1f * density
                keyPaint.color = clipboardOutlineColor()
                canvas.drawRoundRect(thumbRect, 6f * density, 6f * density, keyPaint)
                keyPaint.style = Paint.Style.FILL
            } else {
                keyPaint.color = clipboardOutlineColor()
                canvas.drawRoundRect(thumbRect, 6f * density, 6f * density, keyPaint)
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = themePalette.text
                textPaint.textSize = 11f * density
                canvas.drawText(
                    "🖼",
                    thumbRect.centerX(),
                    thumbRect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f,
                    textPaint,
                )
            }
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = 10f * density
            textPaint.color = clipboardAccentColor()
            val labelLeft = thumbRect.right + 8f * density
            canvas.drawText(action.kindLabel.ifBlank { "ẢNH" }, labelLeft, key.top + 16f * density, textPaint)
            textPaint.textSize = 13f * density
            textPaint.color = if (dark) Color.rgb(248, 250, 252) else Color.rgb(17, 24, 39)
            canvas.drawText("Chạm để dán", labelLeft, key.centerY + 6f * density, textPaint)
            if (pinned) drawClipboardPin(canvas, key)
        } finally {
            textPaint.textAlign = oldAlign
            textPaint.textSize = oldSize
            textPaint.color = oldColor
        }
    }

    private fun drawClipboardPin(canvas: Canvas, key: KeyGeometry) {
        keyPaint.color = clipboardAccentColor()
        canvas.drawCircle(key.right - 12f * density, key.top + 12f * density, 3f * density, keyPaint)
        keyPaint.strokeWidth = 1.5f * density
        canvas.drawLine(key.right - 12f * density, key.top + 14f * density, key.right - 12f * density, key.top + 18f * density, keyPaint)
    }

    private fun clipboardThumbFor(action: KeyAction.CommitClipboard): Bitmap? {
        val id = action.entryId.ifBlank { return null }
        clipboardThumbCache.get(id)?.let { return it }
        val entry = ClipboardHistoryStore.get(context, id) ?: return null
        val maxPx = (96 * density).toInt().coerceAtLeast(64)
        val bmp = ClipboardHistoryStore.decodeThumbnail(context, entry, maxPx) ?: return null
        clipboardThumbCache.put(id, bmp)
        return bmp
    }

    private fun drawClipboardEmptyState(canvas: Canvas, key: KeyGeometry) {
        drawClipboardCard(canvas, key)
        val oldSize = textPaint.textSize
        val oldColor = textPaint.color
        val oldAlign = textPaint.textAlign
        try {
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 15f * density
            canvas.drawText(key.label, key.centerX, key.centerY - 4f * density, textPaint)
            textPaint.textSize = 12f * density
            textPaint.color = if (dark) Color.rgb(148, 163, 184) else Color.rgb(100, 116, 139)
            val helper = if (clipboardTab == 0) "Sao chép chữ, HTML hoặc ảnh" else "Thêm nội dung thường dùng trong Quản lý"
            canvas.drawText(helper, key.centerX, key.centerY + 17f * density, textPaint)
        } finally {
            textPaint.textSize = oldSize
            textPaint.color = oldColor
            textPaint.textAlign = oldAlign
        }
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

    private fun drawClipboardClearButton(canvas: Canvas, key: KeyGeometry) {
        drawClipboardCard(canvas, key)
        val cx = key.centerX
        val cy = key.centerY
        prepareMonoIconPaint()
        keyPaint.strokeWidth = 1.7f * density
        // Trash can: lid + body + handle
        canvas.drawLine(cx - 7f * density, cy - 5f * density, cx + 7f * density, cy - 5f * density, keyPaint)
        canvas.drawLine(cx - 3f * density, cy - 8f * density, cx + 3f * density, cy - 8f * density, keyPaint)
        canvas.drawLine(cx - 5.5f * density, cy - 5f * density, cx - 4f * density, cy + 7f * density, keyPaint)
        canvas.drawLine(cx + 5.5f * density, cy - 5f * density, cx + 4f * density, cy + 7f * density, keyPaint)
        canvas.drawLine(cx - 4f * density, cy + 7f * density, cx + 4f * density, cy + 7f * density, keyPaint)
        canvas.drawLine(cx, cy - 3f * density, cx, cy + 4f * density, keyPaint)
        keyPaint.style = Paint.Style.FILL
    }

    private fun confirmClearClipboardUnpinned() {
        val unpinned = ClipboardHistoryStore.readEntries(context).count { !it.pinned }
        if (unpinned <= 0) {
            Toast.makeText(context, R.string.clipboard_clear_unpinned_empty, Toast.LENGTH_SHORT).show()
            return
        }
        clipboardItemPopup = null
        val barW = 220f * density
        val barH = 44f * density
        val pad = 8f * density
        val cx = width / 2f
        val top = keyboardTop + 10f * density
        val bar = RectF(cx - barW / 2f, top, cx + barW / 2f, top + barH)
        val btnH = barH - pad * 2
        val cancelW = 72f * density
        val confirmW = 72f * density
        val confirm = RectF(bar.right - pad - confirmW, bar.top + pad, bar.right - pad, bar.top + pad + btnH)
        val cancel = RectF(confirm.left - 6f * density - cancelW, bar.top + pad, confirm.left - 6f * density, bar.top + pad + btnH)
        clipboardClearPopup = ClipboardClearPopup(bar, cancel, confirm, unpinned)
        // Clear is opened on finger-up already; no suppress needed.
        suppressClipboardPopupUp = false
        invalidate()
    }

    /** Gboard-style compact bar above the long-pressed clipboard card. */
    private fun showClipboardItemActions(action: KeyAction.CommitClipboard, anchorKey: KeyGeometry) {
        val entryId = action.entryId.ifBlank {
            ClipboardHistoryStore.readEntries(context)
                .firstOrNull { it.text == action.sourceText || it.text == action.value }
                ?.id
                .orEmpty()
        }
        if (entryId.isBlank()) return
        val entry = ClipboardHistoryStore.get(context, entryId) ?: return
        clipboardClearPopup = null
        val btnW = 76f * density
        val btnH = 36f * density
        val gap = 8f * density
        val barPad = 6f * density
        val barW = btnW * 2 + gap + barPad * 2
        val barH = btnH + barPad * 2
        // Prefer above the card; flip below if near the toolbar.
        var top = anchorKey.top - barH - 6f * density
        if (top < keyboardTop + 2f * density) {
            top = anchorKey.bottom + 6f * density
        }
        var left = anchorKey.centerX - barW / 2f
        left = left.coerceIn(6f * density, (width - barW - 6f * density).coerceAtLeast(6f * density))
        val bar = RectF(left, top, left + barW, top + barH)
        val pinBtn = RectF(bar.left + barPad, bar.top + barPad, bar.left + barPad + btnW, bar.top + barPad + btnH)
        val deleteBtn = RectF(pinBtn.right + gap, pinBtn.top, pinBtn.right + gap + btnW, pinBtn.bottom)
        clipboardItemPopup = ClipboardItemPopup(entryId, entry.pinned, bar, pinBtn, deleteBtn)
        // Finger is still down from the long-press — ignore the upcoming UP so the bar stays.
        suppressClipboardPopupUp = true
        invalidate()
    }

    private fun dismissClipboardPopups() {
        if (clipboardItemPopup == null && clipboardClearPopup == null) return
        clipboardItemPopup = null
        clipboardClearPopup = null
        suppressClipboardPopupUp = false
        invalidate()
    }

    private fun drawClipboardItemPopup(canvas: Canvas, popup: ClipboardItemPopup) {
        // Soft dim behind the bar only over the panel area (light, Gboard-like).
        keyPaint.color = Color.argb(40, 0, 0, 0)
        canvas.drawRect(0f, keyboardTop, width.toFloat(), height.toFloat(), keyPaint)
        // Elevated pill bar
        keyPaint.color = if (dark) Color.rgb(60, 64, 67) else Color.WHITE
        canvas.drawRoundRect(popup.bar, 22f * density, 22f * density, keyPaint)
        keyPaint.style = Paint.Style.STROKE
        keyPaint.strokeWidth = 1f * density
        keyPaint.color = if (dark) Color.rgb(95, 99, 104) else Color.rgb(218, 220, 224)
        canvas.drawRoundRect(popup.bar, 22f * density, 22f * density, keyPaint)
        keyPaint.style = Paint.Style.FILL
        drawClipboardPopupChip(
            canvas,
            popup.pinBtn,
            if (popup.pinned) "★ ${context.getString(R.string.clipboard_item_unpin)}"
            else "☆ ${context.getString(R.string.clipboard_item_pin)}",
            filled = false,
        )
        drawClipboardPopupChip(
            canvas,
            popup.deleteBtn,
            context.getString(R.string.clipboard_item_delete),
            filled = true,
        )
    }

    private fun drawClipboardClearPopup(canvas: Canvas, popup: ClipboardClearPopup) {
        keyPaint.color = Color.argb(40, 0, 0, 0)
        canvas.drawRect(0f, keyboardTop, width.toFloat(), height.toFloat(), keyPaint)
        keyPaint.color = if (dark) Color.rgb(60, 64, 67) else Color.WHITE
        canvas.drawRoundRect(popup.bar, 22f * density, 22f * density, keyPaint)
        keyPaint.style = Paint.Style.STROKE
        keyPaint.strokeWidth = 1f * density
        keyPaint.color = if (dark) Color.rgb(95, 99, 104) else Color.rgb(218, 220, 224)
        canvas.drawRoundRect(popup.bar, 22f * density, 22f * density, keyPaint)
        keyPaint.style = Paint.Style.FILL
        val oldSize = textPaint.textSize
        val oldAlign = textPaint.textAlign
        val oldColor = textPaint.color
        try {
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = 12.5f * density
            textPaint.color = themePalette.text
            val label = context.getString(R.string.clipboard_clear_popup_label, popup.unpinnedCount)
            canvas.drawText(label, popup.bar.left + 12f * density, popup.bar.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
        } finally {
            textPaint.textSize = oldSize
            textPaint.textAlign = oldAlign
            textPaint.color = oldColor
        }
        drawClipboardPopupChip(canvas, popup.cancelBtn, context.getString(R.string.cancel), filled = false)
        drawClipboardPopupChip(canvas, popup.confirmBtn, context.getString(R.string.clipboard_clear_unpinned_confirm), filled = true)
    }

    private fun drawClipboardPopupChip(canvas: Canvas, rect: RectF, label: String, filled: Boolean) {
        keyPaint.style = Paint.Style.FILL
        if (filled) {
            keyPaint.color = themePalette.accent
        } else {
            keyPaint.color = if (dark) Color.rgb(48, 49, 52) else Color.rgb(241, 243, 244)
        }
        canvas.drawRoundRect(rect, 18f * density, 18f * density, keyPaint)
        val oldSize = textPaint.textSize
        val oldAlign = textPaint.textAlign
        val oldColor = textPaint.color
        try {
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 12f * density
            textPaint.color = if (filled) {
                if (dark) Color.rgb(32, 33, 36) else Color.WHITE
            } else {
                themePalette.text
            }
            canvas.drawText(
                label,
                rect.centerX(),
                rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f,
                textPaint,
            )
        } finally {
            textPaint.textSize = oldSize
            textPaint.textAlign = oldAlign
            textPaint.color = oldColor
        }
    }

    private fun handleClipboardPopupTouch(event: MotionEvent): Boolean {
        val x = toKeyboardX(event.x)
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Tap outside the bar dismisses immediately (Gboard-like).
                val item = clipboardItemPopup
                if (item != null && !item.bar.contains(x, y)) {
                    dismissClipboardPopups()
                    return true
                }
                val clear = clipboardClearPopup
                if (clear != null && !clear.bar.contains(x, y)) {
                    dismissClipboardPopups()
                    return true
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> return true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // Release after long-press that opened the item popup — keep bar visible.
                if (suppressClipboardPopupUp) {
                    suppressClipboardPopupUp = false
                    // Also clear any lingering pointer state from the long-press gesture.
                    cancelActiveGesture()
                    return true
                }
                val item = clipboardItemPopup
                if (item != null) {
                    when {
                        item.pinBtn.contains(x, y) -> {
                            ClipboardHistoryStore.togglePinned(context, item.entryId)
                            dismissClipboardPopups()
                            rebuildKeys(width.toFloat(), height.toFloat())
                            invalidate()
                        }
                        item.deleteBtn.contains(x, y) -> {
                            ClipboardHistoryStore.delete(context, item.entryId)
                            Toast.makeText(context, R.string.clipboard_item_deleted, Toast.LENGTH_SHORT).show()
                            dismissClipboardPopups()
                            rebuildKeys(width.toFloat(), height.toFloat())
                            invalidate()
                        }
                        item.bar.contains(x, y) -> Unit // absorb taps on padding
                        else -> dismissClipboardPopups()
                    }
                    return true
                }
                val clear = clipboardClearPopup
                if (clear != null) {
                    when {
                        clear.confirmBtn.contains(x, y) -> {
                            val removed = ClipboardHistoryStore.clearUnpinned(context)
                            Toast.makeText(
                                context,
                                context.getString(R.string.clipboard_clear_unpinned_done, removed),
                                Toast.LENGTH_SHORT,
                            ).show()
                            dismissClipboardPopups()
                            rebuildKeys(width.toFloat(), height.toFloat())
                            invalidate()
                        }
                        clear.cancelBtn.contains(x, y) -> dismissClipboardPopups()
                        else -> dismissClipboardPopups()
                    }
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (suppressClipboardPopupUp) {
                    // Cancel of the open gesture should still keep the popup.
                    suppressClipboardPopupUp = false
                    cancelActiveGesture()
                } else {
                    dismissClipboardPopups()
                }
            }
        }
        return true
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

    private data class FeatureTextWindow(val start: Int, val end: Int)

    private fun isAiProcessing(): Boolean = aiStatus.contains("đang xử lý", ignoreCase = true)

    /**
     * Keep the caret visible by windowing text around [cursor], like a single-line EditText.
     */
    private fun featureTextWindow(
        text: String,
        cursor: Int,
        key: KeyGeometry,
        reserveTrailing: Float,
    ): FeatureTextWindow {
        if (text.isEmpty()) return FeatureTextWindow(0, 0)
        val oldSize = textPaint.textSize
        textPaint.textSize = 16f * density
        val available = (key.right - key.left - reserveTrailing).coerceAtLeast(40f)
        val caret = cursor.coerceIn(0, text.length)
        var start = caret
        var used = 0f
        while (start > 0) {
            val width = textPaint.measureText(text, start - 1, start)
            if (used + width > available * 0.68f) break
            used += width
            start--
        }
        var end = caret.coerceIn(start, text.length)
        while (end < text.length) {
            val width = textPaint.measureText(text, end, end + 1)
            if (used + width > available) break
            used += width
            end++
        }
        textPaint.textSize = oldSize
        return FeatureTextWindow(start, end)
    }

    /** Draw AI / Dịch source field with caret — same interaction model as a normal text box. */
    private fun drawFeatureInputField(
        canvas: Canvas,
        key: KeyGeometry,
        text: String,
        cursor: Int,
        placeholder: String,
        status: String,
        reserveTrailing: Float,
    ) {
        val oldAlign = textPaint.textAlign
        val oldSize = textPaint.textSize
        val oldTypeface = textPaint.typeface
        val oldColor = textPaint.color
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 16f * density
        textPaint.typeface = Typeface.DEFAULT
        val left = key.left + 14f * density
        val hasStatus = status.isNotEmpty()
        val baseline = key.centerY - (textPaint.ascent() + textPaint.descent()) / 2f -
            if (hasStatus) 4f * density else 0f
        canvas.save()
        canvas.clipRect(key.left + 10f * density, key.top, key.right - 10f * density, key.bottom)
        if (text.isEmpty()) {
            textPaint.color = hintPaint.color
            canvas.drawText(placeholder, left, baseline, textPaint)
            // Empty field still shows a caret at the start (like focused EditText).
            keyPaint.style = Paint.Style.STROKE
            keyPaint.color = themePalette.accent
            keyPaint.strokeWidth = 1.6f * density
            canvas.drawLine(left, baseline + textPaint.ascent(), left, baseline + textPaint.descent(), keyPaint)
            keyPaint.style = Paint.Style.FILL
        } else {
            textPaint.color = themePalette.text
            val window = featureTextWindow(text, cursor, key, reserveTrailing)
            val shown = text.substring(window.start, window.end)
            canvas.drawText(shown, left, baseline, textPaint)
            val caretIndex = cursor.coerceIn(window.start, window.end)
            val cursorPrefix = text.substring(window.start, caretIndex)
            val cursorX = left + textPaint.measureText(cursorPrefix)
            keyPaint.style = Paint.Style.STROKE
            keyPaint.color = themePalette.accent
            keyPaint.strokeWidth = 1.6f * density
            canvas.drawLine(cursorX, baseline + textPaint.ascent(), cursorX, baseline + textPaint.descent(), keyPaint)
            keyPaint.style = Paint.Style.FILL
        }
        if (hasStatus) {
            textPaint.textSize = 10f * density
            textPaint.color = hintPaint.color
            canvas.drawText(status, left, key.bottom - 5f * density, textPaint)
        }
        canvas.restore()
        textPaint.textAlign = oldAlign
        textPaint.textSize = oldSize
        textPaint.typeface = oldTypeface
        textPaint.color = oldColor
    }

    private fun featureCursorForX(
        text: String,
        cursor: Int,
        key: KeyGeometry,
        touchX: Float,
        reserveTrailing: Float,
    ): Int {
        if (text.isEmpty()) return 0
        val oldSize = textPaint.textSize
        textPaint.textSize = 16f * density
        val window = featureTextWindow(text, cursor, key, reserveTrailing)
        val localX = (touchX - key.left - 14f * density).coerceAtLeast(0f)
        var width = 0f
        var best = window.start
        for (index in window.start until window.end) {
            val charWidth = textPaint.measureText(text, index, index + 1)
            if (localX < width + charWidth / 2f) break
            width += charWidth
            best = index + 1
        }
        textPaint.textSize = oldSize
        return best
    }

    private fun isFeatureInputKey(key: KeyGeometry?): Boolean =
        key?.id == "ai-input" || key?.id == "translate-input"

    private fun placeCursorOnFeatureInput(key: KeyGeometry, touchX: Float) {
        when (key.id) {
            "ai-input" -> {
                val index = featureCursorForX(
                    aiPrompt,
                    aiCursor,
                    key,
                    touchX,
                    if (isAiProcessing()) 76f * density else 30f * density,
                )
                // Optimistic local update so caret follows the finger without waiting a frame.
                aiCursor = index
                onKeyAction(KeyAction.SetAiCursor(index))
            }
            "translate-input" -> {
                val index = featureCursorForX(
                    translationInput,
                    translationCursor,
                    key,
                    touchX,
                    30f * density,
                )
                translationCursor = index
                onKeyAction(KeyAction.SetTranslationCursor(index))
            }
        }
        invalidate()
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
        if (capsLocked) canvas.drawLine(cx - 5f * density, cy + 16f * density, cx + 5f * density, cy + 16f * density, keyPaint)
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
        keyPaint.color = themePalette.pressed
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
        if (clipboardItemPopup != null || clipboardClearPopup != null) {
            return handleClipboardPopupTouch(event)
        }
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
            // Drag on AI / Dịch field: move caret like a normal single-line text box.
            val downInput = state.downKey
            if (isFeatureInputKey(downInput)) {
                cancelLongPress(id)
                state.longPressed = true
                placeCursorOnFeatureInput(downInput!!, x)
                continue
            }
            // Space-swipe moves caret in the focused editor — including AI / Dịch fields.
            if (state.downKey?.action == KeyAction.Space) {
                val steps = SpaceCursorGesturePolicy.steps(x - state.downX, 12f * density)
                val change = steps - state.cursorSteps
                if (change != 0) {
                    state.cursorSteps = steps
                    state.longPressed = true
                    onKeyAction(KeyAction.MoveCursor(change))
                }
                continue
            }
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
        val action = if (state.downKey?.action == KeyAction.Space && state.cursorSteps != 0) null
            else state.key?.takeIf { it == releasedOver }?.action
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
            if (panel != Panel.EMOJI) { emojiSearchActive = false; emojiSearchQuery = "" }
            symbols = false
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action == KeyAction.ToggleEmojiSearch) {
            emojiSearchActive = !emojiSearchActive
            emojiSearchQuery = ""
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action is KeyAction.EmojiSearchCharacter) {
            if (emojiSearchQuery.length < 24) emojiSearchQuery += action.value
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action == KeyAction.EmojiSearchBackspace) {
            emojiSearchQuery = emojiSearchQuery.dropLast(1)
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action == KeyAction.ToggleClipboard) {
            dismissClipboardPopups()
            panel = if (panel == Panel.CLIPBOARD) Panel.NONE else Panel.CLIPBOARD
            if (panel == Panel.CLIPBOARD) clipboardTab = 0
            symbols = false
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action is KeyAction.SelectEmojiGroup) {
            emojiGroup = action.index.coerceIn(0, EmojiCatalog.groups.size)
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action is KeyAction.SelectClipboardTab) {
            dismissClipboardPopups()
            clipboardTab = action.index.coerceIn(0, 1)
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (action == KeyAction.ClearClipboardUnpinned) {
            confirmClearClipboardUnpinned()
        } else if (action == KeyAction.HideKeyboard) {
            when {
                panel != Panel.NONE -> panel = Panel.NONE
                symbols -> { symbols = false; symbolPage = 0 }
                else -> onKeyAction(action)
            }
            rebuildKeys(width.toFloat(), height.toFloat())
        } else if (handleAdjustmentAction(action)) {
            Unit
        } else if (
            isFeatureInputKey(state.downKey) &&
            state.downKey == releasedOver
        ) {
            // Tap (or end of drag) places caret under the finger.
            placeCursorOnFeatureInput(state.downKey!!, toKeyboardX(event.getX(index)))
        } else if (action == KeyAction.Shift && !state.longPressed) {
            val now = android.os.SystemClock.uptimeMillis()
            if (ShiftGesturePolicy.isDoubleTap(lastShiftTapMs, now)) {
                lastShiftTapMs = -1L
                onKeyAction(KeyAction.CapsLock)
            } else {
                lastShiftTapMs = now
                onKeyAction(KeyAction.Shift)
            }
        } else if (action != null && !state.longPressed) {
            if (panel == Panel.EMOJI && action is KeyAction.CommitText) {
                EmojiRecentStore.add(context, action.value)
            }
            onKeyAction(action)
            if ((action is KeyAction.CommitText || action is KeyAction.CommitClipboard) && panel != Panel.NONE) {
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
        dismissClipboardPopups()
        panel = Panel.NONE
        rebuildKeys(width.toFloat(), height.toFloat())
        invalidate()
    }

    /**
     * Return to the letter keyboard after the IME is hidden or shown again.
     * Closes emoji / clipboard / AI / translate / voice overlays and ABC symbols.
     */
    fun resetToLetterKeyboard() {
        dismissClipboardPopups()
        val structureChanged = panel != Panel.NONE ||
            emojiSearchActive ||
            symbols ||
            symbolPage != 0 ||
            voicePanel ||
            aiMode ||
            translationMode
        panel = Panel.NONE
        emojiSearchActive = false
        emojiSearchQuery = ""
        symbols = false
        symbolPage = 0
        voicePanel = false
        voiceStatus = ""
        voicePartial = ""
        voiceLevel = 0f
        voicePaused = false
        aiMode = false
        aiPrompt = ""
        aiCursor = 0
        aiStatus = ""
        aiSuggestions = emptyList()
        removeCallbacks(hideAiSuggestionsRunnable)
        removeCallbacks(aiAnimationRunnable)
        aiAnimationFrame = 0
        translationMode = false
        translationInput = ""
        translationCursor = 0
        translationStatus = ""
        suggestionMenuActive = false
        removeCallbacks(restoreToolbarRunnable)
        if (!structureChanged) {
            invalidate()
            return
        }
        rebuildKeys(width.toFloat(), height.toFloat())
        requestLayout()
        invalidate()
    }

    private fun stopRepeat() {
        removeCallbacks(repeatRunnable)
        repeatState.cancel()
    }

    private fun scheduleLongPress(pointerId: Int, key: KeyGeometry) {
        if (key.action == KeyAction.Shift) {
            val runnable = Runnable {
                val state = pointers[pointerId]
                if (state?.key == key && !state.longPressed) {
                    state.longPressed = true
                    lastShiftTapMs = -1L
                    onKeyAction(KeyAction.CapsLock)
                    vibrate()
                }
                longPressRunnables.remove(pointerId)
            }
            longPressRunnables[pointerId] = runnable
            postDelayed(runnable, 420L)
            return
        }
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
        val clipboardAction = if (panel == Panel.CLIPBOARD && clipboardTab == 0) {
            key.action as? KeyAction.CommitClipboard
        } else null
        if (clipboardAction != null) {
            val runnable = Runnable {
                val state = pointers[pointerId]
                if (state?.key == key && !state.longPressed) {
                    state.longPressed = true
                    vibrate()
                    showClipboardItemActions(clipboardAction, key)
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
        keyboardTop = toolbarHeight + when {
            aiMode -> translationInputHeight
            translationMode -> translationInputHeight
            else -> 0f
        }
        addToolbar(totalWidth)
        val layoutBottomDp = if (adjustmentMode) activeBottomDp() else bottomOffsetDp
        val usableHeight = totalHeight - keyboardTop - layoutBottomDp * density
        val rowCount = if (numberRowEnabled && !numericMode) 5 else 4
        val rowHeight = (usableHeight - margin * 2 - gap * (rowCount - 1)) / rowCount
        // Feature modes (AI / Dịch / voice) always own the body — never leave CLIPBOARD/EMOJI
        // underneath their chrome (that made taps from clipboard look like "stuck" or wrong UI).
        val featureBody = aiMode || translationMode
        if (!featureBody && panel == Panel.EMOJI) {
            addEmojiPanel(totalWidth, rowCount, rowHeight, margin, gap)
        } else if (!featureBody && panel == Panel.CLIPBOARD) {
            addClipboardPanel(totalWidth, rowCount, rowHeight, margin, gap)
        } else if (voicePanel) {
            addVoicePanel(totalWidth, totalHeight, margin)
        } else if (numericMode) {
            addNumericPad(totalWidth, rowHeight, margin, gap)
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
        // Letter bottom row when not in media/voice/numpad (includes AI/Dịch typing body).
        if ((featureBody || panel == Panel.NONE) && !voicePanel && !numericMode) {
            addBottomRow(totalWidth, rowCount - 1, rowHeight, margin, gap)
        }
    }

    private fun addNumericPad(totalWidth: Float, rowHeight: Float, margin: Float, gap: Float) {
        val actionWidth = totalWidth * 0.23f
        val digitAreaRight = totalWidth - margin - actionWidth - gap
        val digitWidth = (digitAreaRight - margin - gap * 2) / 3f
        arrayOf("123", "456", "789").forEachIndexed { row, digits ->
            val top = keyboardTop + margin + row * (rowHeight + gap)
            digits.forEachIndexed { column, digit ->
                val left = margin + column * (digitWidth + gap)
                addKey("numpad-$digit", digit.toString(), KeyAction.Character(digit), left, top, left + digitWidth, top + rowHeight)
            }
        }
        val bottomTop = keyboardTop + margin + 3 * (rowHeight + gap)
        val extras = when {
            numericPhone -> '*' to '#'
            numericDecimal && numericSigned -> '-' to '.'
            numericDecimal -> null to '.'
            numericSigned -> '-' to null
            else -> null to null
        }
        extras.first?.let { addKey("numpad-extra-left", it.toString(), KeyAction.Character(it), margin, bottomTop, margin + digitWidth, bottomTop + rowHeight) }
        val zeroLeft = margin + digitWidth + gap
        addKey("numpad-0", "0", KeyAction.Character('0'), zeroLeft, bottomTop, zeroLeft + digitWidth, bottomTop + rowHeight)
        extras.second?.let {
            val left = zeroLeft + digitWidth + gap
            addKey("numpad-extra-right", it.toString(), KeyAction.Character(it), left, bottomTop, left + digitWidth, bottomTop + rowHeight)
        }
        val actionLeft = totalWidth - margin - actionWidth
        val splitTop = keyboardTop + margin + 2 * (rowHeight + gap) - gap / 2f
        addKey("backspace", "⌫", KeyAction.Backspace, actionLeft, keyboardTop + margin, totalWidth - margin, splitTop)
        addKey("enter", "↵", KeyAction.Enter, actionLeft, splitTop + gap, totalWidth - margin, bottomTop + rowHeight)
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
        if (panel == Panel.EMOJI && emojiSearchActive) {
            addEmojiSearchToolbar(totalWidth, margin)
            return
        }
        val bottom = toolbarHeight - 4f * density
        // Keep privacy chrome on both ABC and ?123; do not fall back to full smartbar.
        if (privateSession && panel == Panel.NONE) {
            addPrivateSessionToolbar(totalWidth, margin, bottom)
            return
        }
        if (suggestionMenuActive && panel == Panel.NONE && !symbols) {
            addSuggestionToolbar(totalWidth, margin, bottom)
            return
        }
        val available = listOf(
            Triple("back", "", KeyAction.HideKeyboard),
            Triple("mic", "", KeyAction.VoiceInput),
            Triple("translate", "", KeyAction.OpenTranslator),
            Triple("ai", "", KeyAction.OpenAi),
            Triple("clipboard", "", KeyAction.ToggleClipboard),
            Triple("settings", "", KeyAction.OpenSettings),
            Triple("emoji", "", KeyAction.ToggleEmoji),
        ).associateBy { it.first }
        // Defense in depth if private toolbar path is skipped (e.g. emoji panel).
        val blocked = if (privateSession) {
            setOf("ai", "clipboard", "mic", "translate", "emoji", "settings")
        } else {
            emptySet()
        }
        val actions = KeyboardPreferences.smartbarOrder(context)
            .filterNot(blocked::contains)
            .mapNotNull(available::get)
        val cellWidth = (totalWidth - margin * 2) / actions.size
        actions.forEachIndexed { index, (id, label, action) ->
            val edgeWidth = 50f * density
            val left = when (index) {
                0 -> margin
                actions.lastIndex -> totalWidth - margin - edgeWidth
                else -> margin + index * cellWidth
            }
            val right = when (index) {
                0 -> margin + edgeWidth
                actions.lastIndex -> totalWidth - margin
                else -> left + cellWidth
            }
            addKey("toolbar-$id", label, action, left, 4f * density, right, bottom)
        }
    }

    private fun addEmojiSearchToolbar(totalWidth: Float, margin: Float) {
        val bottom = toolbarHeight - 4f * density
        val edgeWidth = 50f * density
        addKey("toolbar-emoji-search-close", "⌕", KeyAction.ToggleEmojiSearch, margin, 4f * density, margin + edgeWidth, bottom)
        addKey(
            "toolbar-emoji-search-query", if (emojiSearchQuery.isEmpty()) "Tìm emoji" else emojiSearchQuery,
            KeyAction.ToggleEmojiSearch, margin + edgeWidth, 4f * density, totalWidth - margin - edgeWidth, bottom,
        )
        addKey(
            "toolbar-emoji-search-delete", "⌫", KeyAction.EmojiSearchBackspace,
            totalWidth - margin - edgeWidth, 4f * density, totalWidth - margin, bottom,
        )
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
        val micWidth = 48f * density
        var left = margin
        addKey("toolbar-back", "", if (voicePanel) KeyAction.CancelVoice else KeyAction.CloseAi, left, top, left + closeWidth, bottom)
        left += closeWidth + gap
        val middleRight = totalWidth - margin - micWidth - gap
        if (aiSuggestions.isNotEmpty()) {
            val suggestionWidth = (middleRight - left - gap * 2) / 3f
            aiSuggestions.forEachIndexed { index, value ->
                val suggestionLeft = left + index * (suggestionWidth + gap)
                addKey(
                    "command-suggestion-$index",
                    value,
                    KeyAction.SelectAiSuggestion(value),
                    suggestionLeft,
                    top,
                    suggestionLeft + suggestionWidth,
                    bottom,
                )
            }
        } else {
            val controlWidth = (middleRight - left - gap) / 2f
            addKey("ai-tone", "", KeyAction.OpenAi, left, top, left + controlWidth, bottom)
            left += controlWidth + gap
            addKey("ai-mode", "", KeyAction.OpenAi, left, top, middleRight, bottom)
        }
        addKey("ai-mic", "", KeyAction.VoiceAi, totalWidth - margin - micWidth, top, totalWidth - margin, bottom)
    }

    private fun addTranslationPanel(totalWidth: Float, margin: Float) {
        // Label is drawn by drawFeatureInputField (caret + window); keep empty to avoid double-draw.
        addKey(
            "translate-input",
            "",
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
        textPaint.textSize = keyLabelTextSize
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

    /** A: banner + B: lock only — no settings/mic/emoji/translate in password mode. */
    private fun addPrivateSessionToolbar(totalWidth: Float, margin: Float, bottom: Float) {
        val gap = 5f * density
        val edgeWidth = 50f * density
        val lockWidth = 44f * density
        val top = 4f * density
        val bannerLeft = margin + edgeWidth + gap
        val bannerRight = totalWidth - margin - lockWidth - gap
        addKey("toolbar-back", "", KeyAction.HideKeyboard, margin, top, margin + edgeWidth, bottom)
        val bannerLabel = context.getString(R.string.private_session_banner_short)
        addKey(
            "toolbar-privacy-banner",
            bannerLabel,
            KeyAction.NoOp,
            bannerLeft,
            top,
            bannerRight,
            bottom,
        )
        addKey(
            "toolbar-privacy-lock",
            "",
            KeyAction.NoOp,
            totalWidth - margin - lockWidth,
            top,
            totalWidth - margin,
            bottom,
        )
    }

    private fun addEmojiPanel(totalWidth: Float, rowCount: Int, rowHeight: Float, margin: Float, gap: Float) {
        if (emojiSearchActive) {
            addEmojiSearchPanel(rowCount, rowHeight, margin, gap)
            return
        }
        val categories = listOf("⌕" to KeyAction.ToggleEmojiSearch, "◷" to KeyAction.SelectEmojiGroup(0)) +
            EmojiCatalog.groups.mapIndexed { index, group -> group.icon to KeyAction.SelectEmojiGroup(index + 1) }
        val categoryRow = rowCount - 1
        addActionRow(categories, categoryRow, rowHeight, margin, gap)
        val emojiRows = rowCount - 1
        val emojis = if (emojiGroup == 0) EmojiRecentStore.read(context) else EmojiCatalog.groups[emojiGroup - 1].values
        emojis.take(emojiRows * 10).chunked(10).forEachIndexed { row, values ->
            addEmojiRow(values, row, rowHeight, margin, gap)
        }
    }

    private fun addEmojiSearchPanel(rowCount: Int, rowHeight: Float, margin: Float, gap: Float) {
        val resultRows = (rowCount - 3).coerceAtLeast(1)
        val results = if (emojiSearchQuery.isBlank()) {
            EmojiRecentStore.read(context).ifEmpty { EmojiCatalog.groups.first().values }
        } else EmojiCatalog.search(emojiSearchQuery, resultRows * 10)
        results.take(resultRows * 10).chunked(10).forEachIndexed { row, values ->
            addEmojiRow(values, row, rowHeight, margin, gap)
        }
        listOf("qwertyuiop", "asdfghjkl", "zxcvbnm").forEachIndexed { row, letters ->
            addActionRow(
                letters.map { it.toString() to KeyAction.EmojiSearchCharacter(it) },
                resultRows + row, rowHeight, margin, gap,
            )
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
        // Do NOT re-capture primary clip here — that resurrected the newest item after delete/clear.
        // Capture only via clip-changed listener + onAttachedToWindow.
        val clipboardEntries = ClipboardHistoryStore.readEntries(context)
        val notes = NoteStore.read(context)
        val columns = 2
        val contentRows = rowCount - 1
        val capacity = columns * contentRows
        if (clipboardTab == 0) {
            val values = clipboardEntries.take(capacity)
            if (values.isEmpty()) {
                val top = keyboardTop + margin
                val bottom = keyboardTop + margin + contentRows * (rowHeight + gap) - gap
                addKey("clipboard-empty", "Clipboard trống", KeyAction.ToggleClipboard, margin, top, totalWidth - margin, bottom)
            } else {
                val keyWidth = (totalWidth - margin * 2 - gap) / columns
                values.chunked(columns).forEachIndexed { row, items ->
                    val top = keyboardTop + margin + row * (rowHeight + gap)
                    items.forEachIndexed { column, entry ->
                        val left = margin + column * (keyWidth + gap)
                        val state = if (entry.pinned) "pinned" else "normal"
                        addKey(
                            "clipboard-item-$state-$row-$column",
                            "",
                            commitActionForClipboardEntry(entry),
                            left, top, left + keyWidth, top + rowHeight,
                        )
                    }
                }
            }
            // Clear unpinned — top-right on clipboard history tab.
            val clearSize = 40f * density
            val clearRight = totalWidth - margin - 6f * density
            val clearTop = keyboardTop + margin + 6f * density
            addKey(
                "clipboard-clear-unpinned",
                context.getString(R.string.clipboard_clear_unpinned),
                KeyAction.ClearClipboardUnpinned,
                clearRight - clearSize, clearTop, clearRight, clearTop + clearSize,
            )
        } else {
            val values = notes.take(capacity)
            if (values.isEmpty()) {
                val top = keyboardTop + margin
                val bottom = keyboardTop + margin + contentRows * (rowHeight + gap) - gap
                addKey("clipboard-empty", "Chưa có ghi chú", KeyAction.ToggleClipboard, margin, top, totalWidth - margin, bottom)
            } else {
                val keyWidth = (totalWidth - margin * 2 - gap) / columns
                values.chunked(columns).forEachIndexed { row, items ->
                    val top = keyboardTop + margin + row * (rowHeight + gap)
                    items.forEachIndexed { column, value ->
                        val left = margin + column * (keyWidth + gap)
                        addKey(
                            "clipboard-item-normal-$row-$column",
                            "",
                            KeyAction.CommitText(value),
                            left, top, left + keyWidth, top + rowHeight,
                        )
                    }
                }
            }
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

    private fun commitActionForClipboardEntry(entry: ClipboardEntry): KeyAction.CommitClipboard {
        val smart = if (entry.kind == ClipboardEntryKind.TEXT) {
            SmartClipboardClassifier.classify(entry.text)
        } else null
        val kindLabel = when {
            entry.kind == ClipboardEntryKind.IMAGE -> "ẢNH"
            entry.kind == ClipboardEntryKind.HTML -> "HTML"
            smart != null && smart.kind.label.isNotEmpty() -> smart.kind.label
            else -> entry.kindLabel
        }
        return KeyAction.CommitClipboard(
            value = smart?.pasteText ?: entry.text,
            sourceText = entry.text,
            kindLabel = kindLabel,
            entryId = entry.id,
            contentKind = entry.kind.name,
            imageFileName = entry.imageFileName,
            mimeType = entry.mimeType,
            html = entry.html,
        )
    }

    private fun addEmojiRow(values: List<String>, row: Int, rowHeight: Float, margin: Float, gap: Float) {
        val columns = 10
        val keyWidth = (width - margin * 2 - gap * (columns - 1)) / columns
        val top = keyboardTop + margin + row * (rowHeight + gap)
        values.take(columns).forEachIndexed { index, value ->
            val left = margin + index * (keyWidth + gap)
            addKey("emoji-$row-$index", value, KeyAction.CommitText(value), left, top, left + keyWidth, top + rowHeight)
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
        if (privateSession) return
        val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip ?: return
        if (clip.itemCount == 0) return
        ClipboardHistoryStore.captureClip(context, clip)
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
        addKey("comma", leadingPunctuation.toString(), KeyAction.Character(leadingPunctuation), margin + modeWidth + gap, top, spaceLeft, top + rowHeight)
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

    private fun effectiveLeftPx(scaleX: Float): Float = leftPxFor(leftOffsetDp, scaleX)

    private fun leftPxFor(leftDp: Int, scaleX: Float): Float {
        val maximum = width * (1f - scaleX)
        return (leftDp * density).coerceIn(0f, maximum.coerceAtLeast(0f))
    }

    private fun toKeyboardX(screenX: Float): Float {
        // Hit-testing always uses committed geometry so typing targets stay stable mid-drag.
        val scaleX = widthPercent / 100f
        return (screenX - effectiveLeftPx(scaleX)) / scaleX
    }

    private fun drawResizeOverlay(canvas: Canvas, frame: RectF) {
        val left = frame.left
        val top = frame.top
        val right = frame.right
        val bottom = frame.bottom
        val green = themePalette.accent
        // Dim only outside the keyboard content (Gboard-style floating frame).
        keyPaint.color = Color.argb(90, 0, 0, 0)
        if (top > 0f) canvas.drawRect(0f, 0f, width.toFloat(), top, keyPaint)
        if (left > 0f) canvas.drawRect(0f, top, left, bottom, keyPaint)
        if (right < width) canvas.drawRect(right, top, width.toFloat(), bottom, keyPaint)
        if (bottom < height) canvas.drawRect(0f, bottom, width.toFloat(), height.toFloat(), keyPaint)
        keyPaint.color = green
        val stroke = 7f * density
        val arm = 30f * density
        keyPaint.strokeWidth = stroke
        keyPaint.strokeCap = Paint.Cap.ROUND
        listOf(left to top, right to top, left to bottom, right to bottom).forEachIndexed { index, (x, y) ->
            val inwardX = if (index % 2 == 0) 1f else -1f
            val inwardY = if (index < 2) 1f else -1f
            canvas.drawLine(x, y, x + inwardX * arm, y, keyPaint)
            canvas.drawLine(x, y, x, y + inwardY * arm, keyPaint)
        }
        val midY = (top + bottom) / 2f
        canvas.drawRoundRect(RectF(left - stroke / 2, midY - arm, left + stroke / 2, midY + arm), stroke, stroke, keyPaint)
        canvas.drawRoundRect(RectF(right - stroke / 2, midY - arm, right + stroke / 2, midY + arm), stroke, stroke, keyPaint)
        canvas.drawRoundRect(RectF((left + right) / 2 - arm, top - stroke / 2, (left + right) / 2 + arm, top + stroke / 2), stroke, stroke, keyPaint)
        canvas.drawRoundRect(RectF((left + right) / 2 - arm, bottom - stroke / 2, (left + right) / 2 + arm, bottom + stroke / 2), stroke, stroke, keyPaint)

        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
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
        val frame = contentFrame()
        val left = frame.left
        val top = frame.top
        val right = frame.right
        val bottom = frame.bottom
        val centerX = frame.centerX()
        val centerY = frame.centerY()
        val hit = 42f * density
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resizeDragging = true
                resizeDownX = event.x
                resizeDownY = event.y
                // Gesture baseline = current preview (may already differ from last commit if multi-drag).
                startHeightDp = previewHeightDp
                startBottomDp = previewBottomDp
                startWidthPercent = previewWidthPercent
                startLeftDp = previewLeftDp
                fun near(x: Float, y: Float) =
                    (event.x - x) * (event.x - x) + (event.y - y) * (event.y - y) <= hit * hit
                resizeDrag = when {
                    near(centerX - 70f * density, centerY) -> ResizeDrag.RESET
                    near(centerX + 70f * density, centerY) -> ResizeDrag.DONE
                    near(centerX, centerY) -> ResizeDrag.MOVE
                    near(left, top) -> ResizeDrag.TOP_LEFT
                    near(right, top) -> ResizeDrag.TOP_RIGHT
                    near(left, bottom) -> ResizeDrag.BOTTOM_LEFT
                    near(right, bottom) -> ResizeDrag.BOTTOM_RIGHT
                    event.x < left + hit && event.y in top..bottom -> ResizeDrag.LEFT
                    event.x > right - hit && event.y in top..bottom -> ResizeDrag.RIGHT
                    event.y < top + hit && event.x in left..right -> ResizeDrag.TOP
                    event.y > bottom - hit && event.x in left..right -> ResizeDrag.BOTTOM
                    else -> ResizeDrag.MOVE
                }
            }
            MotionEvent.ACTION_MOVE -> updateResizePreview(event.x - resizeDownX, event.y - resizeDownY)
            MotionEvent.ACTION_UP -> {
                when (resizeDrag) {
                    ResizeDrag.RESET -> {
                        previewHeightDp = 220
                        previewBottomDp = 0
                        previewWidthPercent = 100
                        previewLeftDp = 0
                        rebuildKeysForActiveGeometry()
                        finishResizeGesture(applyPreview = true, persist = true, exitAdjustment = false)
                    }
                    ResizeDrag.DONE -> {
                        finishResizeGesture(applyPreview = true, persist = true, exitAdjustment = true)
                    }
                    else -> {
                        updateResizePreview(event.x - resizeDownX, event.y - resizeDownY)
                        // Soft-commit so the next drag starts from here; stay in adjust mode.
                        finishResizeGesture(applyPreview = true, persist = true, exitAdjustment = false)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                previewHeightDp = startHeightDp
                previewBottomDp = startBottomDp
                previewWidthPercent = startWidthPercent
                previewLeftDp = startLeftDp
                rebuildKeysForActiveGeometry()
                finishResizeGesture(applyPreview = false, persist = false, exitAdjustment = false)
            }
        }
        return true
    }

    /**
     * Gboard-style live preview: update geometry every move, rebuild keys, invalidate only.
     * No requestLayout during drag — viewport is already max-sized in adjustment mode.
     */
    private fun updateResizePreview(dx: Float, dy: Float) {
        val dxDp = dx / density
        val dyDp = dy / density
        when (resizeDrag) {
            // Move whole keyboard (horizontal + lift).
            ResizeDrag.MOVE -> {
                previewLeftDp = (startLeftDp + dxDp).toInt().coerceAtLeast(0)
                previewBottomDp = (startBottomDp - dyDp).toInt().coerceIn(0, resizeMaxBottomDp)
            }
            // Left edge: shrink/grow from left, keep right edge roughly stable.
            ResizeDrag.LEFT, ResizeDrag.TOP_LEFT, ResizeDrag.BOTTOM_LEFT -> {
                previewWidthPercent = (startWidthPercent - dx / width * 100f).toInt()
                    .coerceIn(resizeMinWidthPercent, 100)
                previewLeftDp = (startLeftDp + dxDp).toInt().coerceAtLeast(0)
            }
            // Right edge: shrink/grow from right.
            ResizeDrag.RIGHT, ResizeDrag.TOP_RIGHT, ResizeDrag.BOTTOM_RIGHT ->
                previewWidthPercent = (startWidthPercent + dx / width * 100f).toInt()
                    .coerceIn(resizeMinWidthPercent, 100)
            else -> Unit
        }
        when (resizeDrag) {
            // Top edge: height only (finger up → taller), like Gboard.
            ResizeDrag.TOP, ResizeDrag.TOP_LEFT, ResizeDrag.TOP_RIGHT ->
                previewHeightDp = (startHeightDp - dyDp).toInt()
                    .coerceIn(resizeMinHeightDp, resizeMaxHeightDp)
            // Bottom edge: raise/lower only (does not change key size).
            ResizeDrag.BOTTOM, ResizeDrag.BOTTOM_LEFT, ResizeDrag.BOTTOM_RIGHT ->
                previewBottomDp = (startBottomDp - dyDp).toInt().coerceIn(0, resizeMaxBottomDp)
            else -> Unit
        }
        val maxLeftDp = (width * (1f - previewWidthPercent / 100f) / density).toInt().coerceAtLeast(0)
        previewLeftDp = previewLeftDp.coerceIn(0, maxLeftDp)
        rebuildKeysForActiveGeometry()
        invalidate()
    }

    private fun finishResizeGesture(applyPreview: Boolean, persist: Boolean, exitAdjustment: Boolean) {
        resizeDragging = false
        resizeDrag = null
        if (applyPreview) {
            keyboardHeightDp = previewHeightDp
            bottomOffsetDp = previewBottomDp
            widthPercent = previewWidthPercent
            leftOffsetDp = previewLeftDp
        } else {
            keyboardHeightDp = startHeightDp
            bottomOffsetDp = startBottomDp
            widthPercent = startWidthPercent
            leftOffsetDp = startLeftDp
            previewHeightDp = keyboardHeightDp
            previewBottomDp = bottomOffsetDp
            previewWidthPercent = widthPercent
            previewLeftDp = leftOffsetDp
        }
        if (exitAdjustment) {
            adjustmentMode = false
            KeyboardPreferences.setBoolean(context, KeyboardPreferences.ADJUSTMENT_MODE, false)
        }
        if (persist) {
            KeyboardPreferences.setKeyboardGeometry(
                context,
                keyboardHeightDp,
                bottomOffsetDp,
                widthPercent,
                leftOffsetDp,
            )
        }
        // Remeasure only when leaving adjust mode (shrink viewport to final size).
        // While still adjusting, keep max viewport and just refresh keys.
        if (exitAdjustment || !adjustmentMode) {
            requestLayout()
            post {
                rebuildKeys(width.toFloat(), height.toFloat())
                invalidate()
            }
        } else {
            rebuildKeysForActiveGeometry()
        }
        invalidate()
    }

    private fun isGeometryPreferenceKey(key: String?): Boolean =
        key == KeyboardPreferences.HEIGHT_DP ||
            key == KeyboardPreferences.BOTTOM_OFFSET_DP ||
            key == KeyboardPreferences.WIDTH_PERCENT ||
            key == KeyboardPreferences.LEFT_OFFSET_DP

    /** True while the Gboard-style resize chrome is showing. */
    fun isInAdjustmentMode(): Boolean = adjustmentMode

    /**
     * Y offset (px) from the top of this view to the top of the keyboard content band.
     * Used by the IME to set content/touchable insets so the app stays interactive above.
     */
    fun adjustmentContentTopPx(): Int {
        if (!adjustmentMode) return 0
        val contentH = estimatedTotalHeightPx(activeHeightDp(), activeBottomDp())
        return (height - contentH).toInt().coerceAtLeast(0)
    }

    /** Keyboard content rectangle in this view's coordinates (for touchable region). */
    fun adjustmentTouchableRect(): RectF = contentFrame()
}
