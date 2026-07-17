package vn.kai.board

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.HorizontalScrollView
import android.widget.TextView
import android.widget.SeekBar
import android.media.AudioManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.ThemeMode
import vn.kai.board.settings.KeyboardColorStyle
import vn.kai.board.input.UserLexiconStore
import vn.kai.board.input.AutoCorrectionStatsStore
import vn.kai.board.translation.TranslationModelsActivity
import android.widget.Toast
import android.text.InputType
import vn.kai.board.ai.AiPreferences
import vn.kai.board.ai.AiProviderClient
import vn.kai.board.ai.AiTone
import vn.kai.board.ai.SecureApiKeyStore

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        when (KeyboardPreferences.theme(this)) {
            ThemeMode.LIGHT -> setTheme(R.style.Theme_KAIBoard_Light)
            ThemeMode.DARK -> setTheme(R.style.Theme_KAIBoard_Dark)
            ThemeMode.SYSTEM -> setTheme(R.style.Theme_KAIBoard)
        }
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val themeMode = KeyboardPreferences.theme(this)
        val isDark = themeMode == ThemeMode.DARK || themeMode == ThemeMode.SYSTEM &&
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val colorStyle = KeyboardPreferences.colorStyle(this)
        val isAiGradient = colorStyle == KeyboardColorStyle.AI_GRADIENT_2026
        val background = if (isDark) Color.rgb(15, 23, 42) else Color.WHITE
        val primaryText = if (isDark) Color.rgb(248, 250, 252) else Color.rgb(17, 24, 39)
        val secondaryText = if (isDark) Color.rgb(203, 213, 225) else Color.rgb(75, 85, 99)
        val selectedColor = if (isAiGradient) {
            if (isDark) Color.rgb(139, 92, 246) else Color.rgb(79, 70, 229)
        } else Color.rgb(37, 99, 235)
        val idleColor = if (isDark) Color.rgb(51, 65, 85) else Color.rgb(226, 232, 240)
        val cardColor = if (isAiGradient) {
            if (isDark) Color.argb(224, 15, 23, 42) else Color.argb(224, 255, 255, 255)
        } else if (isDark) Color.rgb(30, 41, 59) else Color.rgb(248, 250, 252)
        val outlineColor = if (isAiGradient) {
            if (isDark) Color.rgb(124, 58, 237) else Color.rgb(165, 180, 252)
        } else if (isDark) Color.rgb(71, 85, 105) else Color.rgb(203, 213, 225)
        val controlInactive = if (isDark) Color.rgb(100, 116, 139) else Color.rgb(148, 163, 184)
        val controlTrackInactive = if (isDark) Color.rgb(51, 65, 85) else Color.rgb(203, 213, 225)
        val checkedColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(selectedColor, controlInactive),
        )
        val switchTrackColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.argb(150, Color.red(selectedColor), Color.green(selectedColor), Color.blue(selectedColor)), controlTrackInactive),
        )
        val gradientColors = if (isDark) {
            intArrayOf(Color.rgb(7, 17, 31), Color.rgb(23, 37, 84), Color.rgb(59, 7, 100))
        } else intArrayOf(Color.rgb(207, 250, 254), Color.rgb(221, 214, 254), Color.rgb(252, 231, 243))
        val screenGradient = if (isAiGradient) {
            GradientDrawable(GradientDrawable.Orientation.TL_BR, gradientColors)
        } else null
        window.statusBarColor = if (isAiGradient) gradientColors.first() else background
        window.navigationBarColor = if (isAiGradient) gradientColors.last() else
            if (isDark) Color.rgb(17, 24, 39) else Color.rgb(243, 244, 246)
        window.decorView.systemUiVisibility = if (isDark) 0 else
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(64), dp(24), dp(24))
            if (screenGradient != null) this.background = screenGradient else setBackgroundColor(background)
        }
        lateinit var settingsScroll: ScrollView
        val sectionTargets = mutableMapOf<String, android.view.View>()
        fun textView(value: String, size: Float, color: Int) = TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
        }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            addView(textView(getString(R.string.setup_title), 30f, Color.WHITE))
            addView(textView(getString(R.string.setup_description), 16f, Color.argb(225, 255, 255, 255)).apply {
                setPadding(0, dp(8), 0, 0)
            })
        }
        content.addView(MaterialCardView(this).apply {
            radius = dp(28).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(selectedColor)
            strokeColor = if (isAiGradient) outlineColor else selectedColor
            strokeWidth = dp(2)
            addView(hero)
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
        val navigationItems = listOf(
            "Thiết lập" to "setup", "Nhập liệu" to "input", "KAI AI" to "ai",
            "Bố cục" to "layout", "Giao diện" to "appearance",
        )
        content.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                navigationItems.forEachIndexed { index, (label, key) ->
                    addView(MaterialButton(this@MainActivity).apply {
                        text = label
                        textSize = 14f
                        minWidth = 0; minimumWidth = 0
                        minHeight = 0; minimumHeight = 0
                        insetTop = 0; insetBottom = 0
                        setPadding(dp(14), dp(5), dp(14), dp(5))
                        setTextColor(selectedColor)
                        backgroundTintList = ColorStateList.valueOf(cardColor)
                        strokeColor = ColorStateList.valueOf(selectedColor)
                        strokeWidth = dp(1)
                        setOnClickListener {
                            sectionTargets[key]?.let { target ->
                                settingsScroll.smoothScrollTo(0, (target.top - dp(12)).coerceAtLeast(0))
                            }
                        }
                    }, LinearLayout.LayoutParams(-2, dp(40)).apply { if (index > 0) leftMargin = dp(7) })
                }
            })
        }, LinearLayout.LayoutParams(-1, dp(40)).apply { bottomMargin = dp(12) })
        val typingTest = TextInputEditText(this).apply {
            textSize = 18f
            setSingleLine(false)
            minHeight = dp(88)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setTextColor(primaryText)
            setHintTextColor(if (isDark) Color.rgb(148, 163, 184) else Color.rgb(107, 114, 128))
        }
        val typingField = TextInputLayout(this).apply {
            hint = getString(R.string.typing_test_hint)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = cardColor
            boxStrokeColor = selectedColor
            setEndIconTintList(ColorStateList.valueOf(selectedColor))
            addView(typingTest, LinearLayout.LayoutParams(-1, -2))
        }
        val typingCardContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
            addView(textView(getString(R.string.typing_card_title), 20f, primaryText).apply {
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(2), 0, dp(2), dp(10))
            })
            addView(typingField, LinearLayout.LayoutParams(-1, -2))
        }
        content.addView(MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(cardColor)
            strokeColor = outlineColor
            strokeWidth = dp(2)
            addView(typingCardContent)
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        content.addView(textView(getString(R.string.settings_title), 22f, primaryText).apply {
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        })

        fun addSwitch(target: LinearLayout, label: Int, key: String, checked: Boolean) {
            target.addView(MaterialSwitch(this).apply {
                text = getString(label); textSize = 16f; isChecked = checked; setTextColor(primaryText)
                thumbTintList = checkedColors
                trackTintList = switchTrackColors
                setPadding(0, dp(8), 0, dp(8))
                setOnCheckedChangeListener { _, value -> KeyboardPreferences.setBoolean(this@MainActivity, key, value) }
            })
        }
        fun addIntensitySlider(target: LinearLayout, label: Int, storedValue: Int, systemDefault: Int, key: String) {
            val title = textView("", 16f, primaryText)
            fun update(value: Int, isDefault: Boolean) {
                title.text = if (isDefault) "${getString(label)}: ${getString(R.string.system_default)} ($value%)"
                else "${getString(label)}: $value%"
            }
            val initial = if (storedValue < 0) systemDefault else storedValue
            update(initial, storedValue < 0)
            target.addView(title.apply { setPadding(0, dp(8), 0, 0) })
            target.addView(SeekBar(this).apply {
                max = 100
                progress = initial
                thumbTintList = ColorStateList.valueOf(selectedColor)
                progressTintList = ColorStateList.valueOf(selectedColor)
                progressBackgroundTintList = ColorStateList.valueOf(controlTrackInactive)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        KeyboardPreferences.setIntensity(this@MainActivity, key, progress)
                        update(progress, false)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })
            })
        }
        fun addSection(title: Int, key: String, build: (LinearLayout) -> Unit) {
            val section = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(textView(getString(title), 20f, primaryText).apply {
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, 0, 0, dp(10))
                })
                build(this)
            }
            val sectionCard = MaterialCardView(this).apply {
                radius = dp(20).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(cardColor)
                strokeColor = outlineColor
                strokeWidth = dp(2)
                setContentPadding(dp(16), dp(16), dp(16), dp(16))
                addView(section)
            }
            sectionTargets[key] = sectionCard
            content.addView(sectionCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        }
        addSection(R.string.tab_setup, "setup") { section ->
            section.addView(MaterialButton(this).apply {
                text = getString(R.string.enable_keyboard)
                backgroundTintList = ColorStateList.valueOf(selectedColor)
                setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
            })
            section.addView(MaterialButton(this).apply {
                text = getString(R.string.choose_keyboard)
                backgroundTintList = ColorStateList.valueOf(selectedColor)
                setOnClickListener { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }
            })
            section.addView(textView(getString(R.string.setup_note), 14f, secondaryText).apply { setPadding(0, dp(8), 0, 0) })
        }
        addSection(R.string.tab_input, "input") { section ->
            val audio = getSystemService(AudioManager::class.java)
            val maxVolume = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
            val currentVolume = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            val systemSound = (currentVolume * 100 / maxVolume).coerceIn(0, 100)
            addIntensitySlider(section, R.string.setting_haptic, KeyboardPreferences.hapticIntensity(this), 50, KeyboardPreferences.HAPTIC_INTENSITY)
            addIntensitySlider(section, R.string.setting_sound, KeyboardPreferences.soundIntensity(this), systemSound, KeyboardPreferences.SOUND_INTENSITY)
            addSwitch(section, R.string.setting_popup, KeyboardPreferences.POPUP, KeyboardPreferences.popup(this))
            addSwitch(section, R.string.setting_long_press_symbols, KeyboardPreferences.LONG_PRESS_SYMBOLS, KeyboardPreferences.longPressSymbols(this))
            addSwitch(section, R.string.setting_word_suggestions, KeyboardPreferences.WORD_SUGGESTIONS, KeyboardPreferences.wordSuggestions(this))
            addSwitch(section, R.string.setting_auto_correct, KeyboardPreferences.AUTO_CORRECT, KeyboardPreferences.autoCorrect(this))
            val correctionStats = AutoCorrectionStatsStore.summary(this)
            section.addView(textView(
                getString(R.string.auto_correct_stats, correctionStats.first, correctionStats.second),
                13f,
                secondaryText,
            ).apply { setPadding(dp(4), dp(6), dp(4), dp(2)) })
            val learnedStatus = textView("", 13f, secondaryText).apply {
                setPadding(dp(4), dp(10), dp(4), dp(2))
            }
            var clearLearnedButton: MaterialButton? = null
            fun updateLearnedStatus() {
                val count = UserLexiconStore.count(this@MainActivity)
                learnedStatus.text = getString(
                    R.string.learned_words_status,
                    count,
                    UserLexiconStore.totalUsage(this@MainActivity),
                )
                clearLearnedButton?.isEnabled = count > 0
            }
            section.addView(learnedStatus)
            section.addView(MaterialButton(this).apply {
                text = getString(R.string.manage_learned_words)
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(selectedColor)
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, LearnedWordsActivity::class.java))
                }
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
            val clearButton = MaterialButton(this).apply {
                text = getString(R.string.clear_learned_words)
                setTextColor(selectedColor)
                backgroundTintList = ColorStateList.valueOf(cardColor)
                strokeColor = ColorStateList.valueOf(selectedColor)
                strokeWidth = dp(1)
                setOnClickListener {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle(R.string.clear_learned_words_title)
                        .setMessage(getString(R.string.clear_learned_words_message, UserLexiconStore.count(this@MainActivity)))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.clear) { _, _ ->
                            UserLexiconStore.clear(this@MainActivity)
                            updateLearnedStatus()
                            Toast.makeText(this@MainActivity, R.string.learned_words_cleared, Toast.LENGTH_SHORT).show()
                        }
                        .show()
                }
            }
            clearLearnedButton = clearButton
            section.addView(clearButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
            section.addView(MaterialButton(this).apply {
                text = getString(R.string.manage_translation_models)
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(selectedColor)
                setOnClickListener { startActivity(Intent(this@MainActivity, TranslationModelsActivity::class.java)) }
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
            updateLearnedStatus()
        }
        addSection(R.string.ai_settings_title, "ai") { section ->
            section.addView(textView(getString(R.string.ai_settings_description), 14f, secondaryText).apply {
                setPadding(dp(2), 0, dp(2), dp(10))
            })
            val providers = AiProviderClient.providerChoices
            var providerHint = AiPreferences.providerHint(this)
            if (providerHint !in providers) providerHint = "Tự động"
            val providerDrop = MaterialAutoCompleteTextView(this).apply {
                inputType = 0; setTextColor(primaryText)
                setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, providers))
                setText(providerHint, false)
                setOnItemClickListener { _, _, position, _ ->
                    providerHint = providers[position]
                    AiPreferences.setProviderHint(this@MainActivity, providerHint)
                }
            }
            section.addView(TextInputLayout(this).apply {
                hint = "Nhà cung cấp AI"; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                boxBackgroundColor = cardColor; boxStrokeColor = selectedColor
                endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
                addView(providerDrop, LinearLayout.LayoutParams(-1, -2))
            })
            val keyInput = TextInputEditText(this).apply {
                setText(SecureApiKeyStore.read(this@MainActivity)); setTextColor(primaryText); setSingleLine(true)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            section.addView(TextInputLayout(this).apply {
                hint = "API key"; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                boxBackgroundColor = cardColor; boxStrokeColor = selectedColor
                endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                addView(keyInput, LinearLayout.LayoutParams(-1, -2))
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
            val modelStatus = textView("Chưa quét model", 13f, secondaryText).apply { setPadding(dp(3), dp(8), dp(3), dp(5)) }
            AiPreferences.models(this).takeIf { it.isNotEmpty() }?.let {
                modelStatus.text = "${AiPreferences.provider(this)} • ${it.size} model đã nhận diện"
            }
            section.addView(modelStatus)
            section.addView(MaterialButton(this).apply {
                text = "Lưu key và tự quét model"; setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(selectedColor)
                setOnClickListener {
                    val apiKey = keyInput.text?.toString().orEmpty().trim()
                    if (apiKey.isBlank()) { Toast.makeText(this@MainActivity, "Nhập API key trước", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    isEnabled = false; modelStatus.text = "Đang nhận diện nhà cung cấp và model…"
                    Thread {
                        val result = runCatching { AiProviderClient.discover(apiKey, providerHint) }
                        runOnUiThread {
                            isEnabled = true
                            result.onSuccess { found ->
                                SecureApiKeyStore.save(this@MainActivity, apiKey)
                                AiPreferences.saveDiscovery(this@MainActivity, found.provider, found.models)
                                modelStatus.text = "${found.provider} • ${found.models.size} model • ${found.freeCount} miễn phí"
                            }.onFailure { modelStatus.text = it.message ?: "Không thể quét model" }
                        }
                    }.start()
                }
            })
            val tones = AiTone.entries
            val toneDrop = MaterialAutoCompleteTextView(this).apply {
                inputType = 0; setTextColor(primaryText)
                setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, tones.map { it.label }))
                setText(AiPreferences.tone(this@MainActivity).label, false)
                setOnItemClickListener { _, _, position, _ -> AiPreferences.setTone(this@MainActivity, tones[position]) }
            }
            section.addView(TextInputLayout(this).apply {
                hint = "Giọng văn mặc định"; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                boxBackgroundColor = cardColor; boxStrokeColor = selectedColor
                endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
                addView(toneDrop, LinearLayout.LayoutParams(-1, -2))
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
            val delayTitle = textView("Tự gửi sau khi dừng gõ: ${AiPreferences.delaySeconds(this)} giây", 15f, primaryText)
            section.addView(delayTitle.apply { setPadding(dp(2), dp(12), 0, 0) })
            section.addView(SeekBar(this).apply {
                max = 9; progress = AiPreferences.delaySeconds(this@MainActivity) - 1
                thumbTintList = ColorStateList.valueOf(selectedColor); progressTintList = ColorStateList.valueOf(selectedColor)
                progressBackgroundTintList = ColorStateList.valueOf(controlTrackInactive)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        val seconds = progress + 1
                        delayTitle.text = "Tự gửi sau khi dừng gõ: $seconds giây"
                        AiPreferences.setDelay(this@MainActivity, seconds)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })
            })
            section.addView(MaterialSwitch(this).apply {
                text = "Tự gửi khi dừng gõ"; textSize = 16f; setTextColor(primaryText)
                isChecked = AiPreferences.autoSend(this@MainActivity)
                thumbTintList = checkedColors; trackTintList = switchTrackColors
                setOnCheckedChangeListener { _, checked -> AiPreferences.setAutoSend(this@MainActivity, checked) }
            })
            section.addView(textView("Chỉ gửi nội dung cuối cùng sau debounce; không chạy nền.", 13f, secondaryText).apply {
                setPadding(dp(2), dp(6), dp(2), 0)
            })
        }
        addSection(R.string.tab_layout, "layout") { section ->
            addSwitch(section, R.string.setting_number_row, KeyboardPreferences.NUMBER_ROW, KeyboardPreferences.numberRow(this))
            addSwitch(section, R.string.setting_extended_symbols, KeyboardPreferences.EXTENDED_SYMBOLS, KeyboardPreferences.extendedSymbols(this))
            section.addView(createOneHandSpinner(primaryText, cardColor, selectedColor, ::dp), LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
            })
            section.addView(MaterialButton(this).apply {
                text = getString(R.string.adjust_keyboard_action)
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(selectedColor)
                setOnClickListener {
                    KeyboardPreferences.setBoolean(this@MainActivity, KeyboardPreferences.ADJUSTMENT_MODE, true)
                    typingTest.requestFocus()
                    typingTest.postDelayed({
                        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(typingTest, 0)
                    }, 100L)
                }
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
            section.addView(textView(getString(R.string.adjust_keyboard_description), 13f, secondaryText).apply {
                setPadding(dp(4), dp(4), dp(4), 0)
            })
        }
        addSection(R.string.tab_appearance, "appearance") { section ->
            section.addView(createThemeSpinner(themeMode, primaryText, cardColor, selectedColor, ::dp))
            section.addView(createColorStyleSpinner(primaryText, cardColor, selectedColor), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        }
        settingsScroll = ScrollView(this).apply { addView(content) }
        setContentView(settingsScroll)

        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            typingTest.postDelayed({
                typingTest.requestFocus()
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(typingTest, 0)
            }, 250L)
        }
    }

    private fun createThemeSpinner(themeMode: ThemeMode, primaryText: Int, fieldColor: Int, outlineColor: Int, dp: (Int) -> Int): TextInputLayout {
        val modes = ThemeMode.entries
        val labels = listOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark))
        val dropdown = MaterialAutoCompleteTextView(this).apply {
            inputType = 0
            setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, labels))
            setText(labels[modes.indexOf(themeMode)], false)
            setOnItemClickListener { _, _, position, _ ->
                if (modes[position] != KeyboardPreferences.theme(this@MainActivity)) {
                    KeyboardPreferences.setTheme(this@MainActivity, modes[position])
                    recreate()
                }
            }
        }
        return TextInputLayout(this).apply {
            hint = getString(R.string.setting_theme)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = fieldColor
            boxStrokeColor = outlineColor
            hintTextColor = ColorStateList.valueOf(outlineColor)
            setEndIconTintList(ColorStateList.valueOf(outlineColor))
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(dropdown, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun createOneHandSpinner(primaryText: Int, fieldColor: Int, accentColor: Int, dp: (Int) -> Int): TextInputLayout {
        val labels = listOf(
            getString(R.string.one_hand_off),
            getString(R.string.one_hand_left),
            getString(R.string.one_hand_right),
        )
        val width = KeyboardPreferences.widthPercent(this)
        val left = KeyboardPreferences.leftOffsetDp(this)
        val initial = when {
            width >= 100 -> 0
            left == 0 -> 1
            else -> 2
        }
        val dropdown = MaterialAutoCompleteTextView(this).apply {
            inputType = 0
            setTextColor(primaryText)
            setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, labels))
            setText(labels[initial], false)
            setOnItemClickListener { _, _, position, _ ->
                val oneHandWidth = 85
                val maxLeftDp = (resources.displayMetrics.widthPixels * (1f - oneHandWidth / 100f) /
                    resources.displayMetrics.density).toInt().coerceAtLeast(0)
                val targetWidth = if (position == 0) 100 else oneHandWidth
                val targetLeft = if (position == 2) maxLeftDp else 0
                KeyboardPreferences.setKeyboardGeometry(
                    this@MainActivity,
                    KeyboardPreferences.heightDp(this@MainActivity),
                    KeyboardPreferences.bottomOffsetDp(this@MainActivity),
                    targetWidth,
                    targetLeft,
                )
            }
        }
        return TextInputLayout(this).apply {
            hint = getString(R.string.setting_one_hand)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = fieldColor
            boxStrokeColor = accentColor
            hintTextColor = ColorStateList.valueOf(accentColor)
            setEndIconTintList(ColorStateList.valueOf(accentColor))
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(dropdown, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun createColorStyleSpinner(primaryText: Int, fieldColor: Int, outlineColor: Int): TextInputLayout {
        val styles = KeyboardColorStyle.entries
        val labels = listOf(getString(R.string.color_classic), getString(R.string.color_ai_gradient_2026))
        val current = KeyboardPreferences.colorStyle(this)
        val dropdown = MaterialAutoCompleteTextView(this).apply {
            inputType = 0
            setTextColor(primaryText)
            setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, labels))
            setText(labels[styles.indexOf(current)], false)
            setOnItemClickListener { _, _, position, _ ->
                if (styles[position] != KeyboardPreferences.colorStyle(this@MainActivity)) {
                    KeyboardPreferences.setColorStyle(this@MainActivity, styles[position])
                    recreate()
                }
            }
        }
        return TextInputLayout(this).apply {
            hint = getString(R.string.setting_color_style)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = fieldColor
            boxStrokeColor = outlineColor
            hintTextColor = ColorStateList.valueOf(outlineColor)
            setEndIconTintList(ColorStateList.valueOf(outlineColor))
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            addView(dropdown, LinearLayout.LayoutParams(-1, -2))
        }
    }
}
