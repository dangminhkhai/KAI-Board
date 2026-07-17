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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.ThemeMode
import vn.kai.board.settings.SettingsBackup
import vn.kai.board.settings.KeyboardThemePalette
import vn.kai.board.input.UserLexiconStore
import vn.kai.board.input.AutoCorrectionStatsStore
import vn.kai.board.translation.TranslationModelsActivity
import android.widget.Toast
import android.text.InputType
import vn.kai.board.ai.AiPreferences
import vn.kai.board.ai.AiProviderClient
import vn.kai.board.ai.AiTone
import vn.kai.board.ai.SecureApiKeyStore
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        when (KeyboardPreferences.theme(this)) {
            ThemeMode.LIGHT -> setTheme(R.style.Theme_KAIBoard_Light)
            ThemeMode.DARK -> setTheme(R.style.Theme_KAIBoard_Dark)
            ThemeMode.SYSTEM -> setTheme(R.style.Theme_KAIBoard)
        }
        super.onCreate(savedInstanceState)
        val restoreAppearance = intent.getBooleanExtra(EXTRA_RESTORE_APPEARANCE, false)
        intent.removeExtra(EXTRA_RESTORE_APPEARANCE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val themeMode = KeyboardPreferences.theme(this)
        val isDark = themeMode == ThemeMode.DARK || themeMode == ThemeMode.SYSTEM &&
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val colorStyle = KeyboardPreferences.colorStyle(this)
        val palette = KeyboardThemePalette.resolve(this, colorStyle, isDark)
        val isAiGradient = palette.gradientColors != null
        val background = palette.background
        val primaryText = palette.text
        val secondaryText = palette.hint
        val selectedColor = palette.accent
        val idleColor = palette.specialKey
        val cardColor = palette.key
        val outlineColor = palette.specialKey
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
        val gradientColors = palette.gradientColors
        val screenGradient = gradientColors?.let { GradientDrawable(GradientDrawable.Orientation.TL_BR, it) }
        window.statusBarColor = gradientColors?.first() ?: background
        window.navigationBarColor = gradientColors?.last() ?: background
        window.decorView.systemUiVisibility = if (isDark) 0 else
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
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
            "Bố cục" to "layout", "Giao diện" to "appearance", "Sao lưu" to "backup",
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

        fun addSwitch(target: LinearLayout, label: Int, key: String, checked: Boolean): MaterialSwitch {
            return MaterialSwitch(this).apply {
                text = getString(label); textSize = 16f; isChecked = checked; setTextColor(primaryText)
                thumbTintList = checkedColors
                trackTintList = switchTrackColors
                setPadding(0, dp(8), 0, dp(8))
                setOnCheckedChangeListener { _, value -> KeyboardPreferences.setBoolean(this@MainActivity, key, value) }
            }.also { target.addView(it) }
        }
        fun addIntensitySlider(target: LinearLayout, label: Int, storedValue: Int, key: String): SeekBar {
            val title = textView("", 16f, primaryText)
            fun update(value: Int) {
                title.text = if (value == 0) "${getString(label)}: ${getString(R.string.system_default)}"
                else "${getString(label)}: $value%"
            }
            update(storedValue)
            target.addView(title.apply { setPadding(0, dp(8), 0, 0) })
            return SeekBar(this).apply {
                max = 100
                progress = storedValue
                thumbTintList = ColorStateList.valueOf(selectedColor)
                progressTintList = ColorStateList.valueOf(selectedColor)
                progressBackgroundTintList = ColorStateList.valueOf(controlTrackInactive)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        KeyboardPreferences.setIntensity(this@MainActivity, key, progress)
                        update(progress)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })
            }.also { target.addView(it) }
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
            val hapticSwitch = addSwitch(section, R.string.setting_haptic_enabled, KeyboardPreferences.HAPTIC, KeyboardPreferences.haptic(this))
            val hapticSlider = addIntensitySlider(section, R.string.setting_haptic_intensity, KeyboardPreferences.hapticLevel(this), KeyboardPreferences.HAPTIC_INTENSITY)
            hapticSlider.isEnabled = hapticSwitch.isChecked
            hapticSwitch.setOnCheckedChangeListener { _, enabled ->
                KeyboardPreferences.setBoolean(this@MainActivity, KeyboardPreferences.HAPTIC, enabled)
                hapticSlider.isEnabled = enabled
            }
            val soundSwitch = addSwitch(section, R.string.setting_sound_enabled, KeyboardPreferences.SOUND, KeyboardPreferences.sound(this))
            val soundSlider = addIntensitySlider(section, R.string.setting_sound_intensity, KeyboardPreferences.soundLevel(this), KeyboardPreferences.SOUND_INTENSITY)
            soundSlider.isEnabled = soundSwitch.isChecked
            soundSwitch.setOnCheckedChangeListener { _, enabled ->
                KeyboardPreferences.setBoolean(this@MainActivity, KeyboardPreferences.SOUND, enabled)
                soundSlider.isEnabled = enabled
            }
            addSwitch(section, R.string.setting_popup, KeyboardPreferences.POPUP, KeyboardPreferences.popup(this))
            addSwitch(section, R.string.setting_long_press_symbols, KeyboardPreferences.LONG_PRESS_SYMBOLS, KeyboardPreferences.longPressSymbols(this))
            addSwitch(section, R.string.setting_word_suggestions, KeyboardPreferences.WORD_SUGGESTIONS, KeyboardPreferences.wordSuggestions(this))
            addSwitch(section, R.string.setting_auto_correct, KeyboardPreferences.AUTO_CORRECT, KeyboardPreferences.autoCorrect(this))
            addSwitch(section, R.string.setting_offline_mode, KeyboardPreferences.OFFLINE_MODE, KeyboardPreferences.offlineMode(this))
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
                setText(SecureApiKeyStore.readAll(this@MainActivity).joinToString("\n")); setTextColor(primaryText)
                minLines = 2; maxLines = 5
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            section.addView(TextInputLayout(this).apply {
                hint = "API keys (mỗi dòng một key)"; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
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
                    if (KeyboardPreferences.offlineMode(this@MainActivity)) {
                        Toast.makeText(this@MainActivity, "Tắt chế độ offline để kiểm tra API", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val apiKeys = vn.kai.board.ai.ApiKeyPool.parse(keyInput.text?.toString().orEmpty())
                    if (apiKeys.isEmpty()) { Toast.makeText(this@MainActivity, "Nhập API key trước", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    isEnabled = false; modelStatus.text = "Đang nhận diện nhà cung cấp và model…"
                    Thread {
                        val result = runCatching { AiProviderClient.discover(apiKeys.first(), providerHint) }
                        runOnUiThread {
                            isEnabled = true
                            result.onSuccess { found ->
                                SecureApiKeyStore.saveAll(this@MainActivity, apiKeys)
                                AiPreferences.saveDiscovery(this@MainActivity, found.provider, found.models)
                                modelStatus.text = "${found.provider} • ${found.models.size} model • ${apiKeys.size} key • ${found.accessSummary()}"
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
            section.addView(MaterialCardView(this).apply {
                radius = dp(16).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(cardColor)
                strokeColor = outlineColor
                strokeWidth = dp(1)
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    addView(textView(getString(R.string.keyboard_templates_title), 17f, primaryText).apply {
                        setTypeface(typeface, Typeface.BOLD)
                    })
                    addView(textView(getString(R.string.keyboard_templates_coming_soon), 13f, secondaryText).apply {
                        setPadding(0, dp(4), 0, 0)
                    })
                })
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
            val radiusLabel = textView("", 16f, primaryText).apply { setPadding(0, dp(14), 0, 0) }
            fun updateRadiusLabel(value: Int) { radiusLabel.text = getString(R.string.setting_key_radius_value, value) }
            val initialRadius = KeyboardPreferences.keyRadiusDp(this)
            updateRadiusLabel(initialRadius)
            section.addView(radiusLabel)
            section.addView(SeekBar(this).apply {
                max = 24
                progress = initialRadius
                thumbTintList = ColorStateList.valueOf(selectedColor)
                progressTintList = ColorStateList.valueOf(selectedColor)
                progressBackgroundTintList = ColorStateList.valueOf(controlTrackInactive)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        KeyboardPreferences.setKeyRadiusDp(this@MainActivity, progress)
                        updateRadiusLabel(progress)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })
            })
        }
        addSection(R.string.backup_settings_title, "backup") { section ->
            section.addView(MaterialButton(this).apply {
                text = getString(R.string.export_settings)
                setOnClickListener {
                    startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TITLE, "kai-board-settings.json")
                    }, REQUEST_EXPORT_SETTINGS)
                }
            })
            section.addView(MaterialButton(this).apply {
                text = getString(R.string.import_settings)
                setOnClickListener {
                    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        type = "application/json"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }, REQUEST_IMPORT_SETTINGS)
                }
            })
            section.addView(textView(getString(R.string.backup_settings_note), 13f, secondaryText))
        }
        settingsScroll = ScrollView(this).apply {
            clipToPadding = false
            addView(content)
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    maxOf(systemBars.bottom, ime.bottom),
                )
                insets
            }
        }
        setContentView(settingsScroll)
        ViewCompat.requestApplyInsets(settingsScroll)
        if (restoreAppearance) {
            settingsScroll.post {
                sectionTargets["appearance"]?.let { target ->
                    settingsScroll.scrollTo(0, (target.top - dp(12)).coerceAtLeast(0))
                }
            }
        }

        if (!restoreAppearance && applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE,
            )
            typingTest.postDelayed({
                typingTest.requestFocus()
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(typingTest, 0)
            }, 250L)
        }
    }

    @Deprecated("Activity result callback for document picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        runCatching {
            when (requestCode) {
                REQUEST_EXPORT_SETTINGS -> contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(SettingsBackup.export(this))
                } ?: error("Không thể mở tệp")
                REQUEST_IMPORT_SETTINGS -> {
                    val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Không thể đọc tệp")
                    require(!SettingsBackup.containsSecrets(raw)) { "Tệp chứa trường bí mật không hợp lệ" }
                    SettingsBackup.import(this, raw)
                    recreate()
                }
                else -> return
            }
            Toast.makeText(this, if (requestCode == REQUEST_EXPORT_SETTINGS) "Đã xuất cài đặt" else "Đã nhập cài đặt", Toast.LENGTH_SHORT).show()
        }.onFailure { Toast.makeText(this, it.message ?: "Không thể xử lý tệp", Toast.LENGTH_LONG).show() }
    }

    companion object {
        private const val REQUEST_EXPORT_SETTINGS = 501
        private const val REQUEST_IMPORT_SETTINGS = 502
        private const val EXTRA_RESTORE_APPEARANCE = "restore_appearance"
    }

    private fun recreateAtAppearance() {
        intent.putExtra(EXTRA_RESTORE_APPEARANCE, true)
        recreate()
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
                    recreateAtAppearance()
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

}
