package vn.kai.board

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.HorizontalScrollView
import android.widget.TextView
import android.widget.SeekBar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.KeyboardColorStyle
import vn.kai.board.settings.ThemeMode
import vn.kai.board.settings.SettingsBackup
import vn.kai.board.settings.KeyboardThemePalette
import vn.kai.board.settings.ThemeExtensionStore
import vn.kai.board.input.UserLexiconStore
import vn.kai.board.input.EmailSuggestionStore
import vn.kai.board.input.HashtagSuggestionStore
import vn.kai.board.input.WordDictionaryPack
import vn.kai.board.input.DictionaryLanguagePack
import vn.kai.board.translation.TranslationModelsActivity
import android.widget.Toast
import vn.kai.board.ai.AiPreferences
import vn.kai.board.ai.AiTone
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : Activity() {
    private var observedExtensionId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        when (KeyboardPreferences.theme(this)) {
            ThemeMode.LIGHT -> setTheme(R.style.Theme_KAIBoard_Light)
            ThemeMode.DARK -> setTheme(R.style.Theme_KAIBoard_Dark)
            ThemeMode.SYSTEM -> setTheme(R.style.Theme_KAIBoard)
        }
        super.onCreate(savedInstanceState)
        observedExtensionId = KeyboardPreferences.themeExtensionId(this)
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
        val palette = KeyboardThemePalette.resolve(this, isDark)
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
            setPadding(dp(16), dp(12), dp(16), dp(16))
            if (screenGradient != null) this.background = screenGradient else setBackgroundColor(background)
        }
        lateinit var settingsScroll: ScrollView
        val sectionTargets = mutableMapOf<String, android.view.View>()
        fun textView(value: String, size: Float, color: Int) = TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
        }
        fun blendColor(first: Int, second: Int, secondWeight: Float): Int {
            val weight = secondWeight.coerceIn(0f, 1f)
            return Color.rgb(
                (Color.red(first) * (1f - weight) + Color.red(second) * weight).toInt(),
                (Color.green(first) * (1f - weight) + Color.green(second) * weight).toInt(),
                (Color.blue(first) * (1f - weight) + Color.blue(second) * weight).toInt(),
            )
        }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            this.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(selectedColor, blendColor(selectedColor, outlineColor, 0.28f)),
            )
            addView(textView("K", 24f, Color.WHITE).apply {
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                this.background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(Color.argb(48, 255, 255, 255))
                    setStroke(dp(1), Color.argb(90, 255, 255, 255))
                }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(textView(getString(R.string.setup_title), 23f, Color.WHITE).apply {
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(textView("Bàn phím Việt • Riêng tư", 13f, Color.argb(220, 255, 255, 255)))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            val versionName = runCatching {
                packageManager.getPackageInfo(packageName, 0).versionName
            }.getOrNull().orEmpty()
            addView(textView("v$versionName", 12f, Color.WHITE).apply {
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(5), dp(10), dp(5))
                this.background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(Color.argb(42, 255, 255, 255))
                }
            })
        }
        content.addView(MaterialCardView(this).apply {
            radius = dp(22).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(selectedColor)
            strokeColor = Color.argb(70, 255, 255, 255)
            strokeWidth = dp(1)
            addView(hero)
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        val navigationItems = listOf(
            "Thiết lập" to "setup", "Gõ phím" to "input", "KAI AI" to "ai",
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
                        setPadding(dp(12), dp(3), dp(12), dp(3))
                        setTextColor(selectedColor)
                        backgroundTintList = ColorStateList.valueOf(cardColor)
                        strokeColor = ColorStateList.valueOf(selectedColor)
                        strokeWidth = dp(1)
                        setOnClickListener {
                            sectionTargets[key]?.let { target ->
                                settingsScroll.smoothScrollTo(0, (target.top - dp(12)).coerceAtLeast(0))
                            }
                        }
                    }, LinearLayout.LayoutParams(-2, dp(36)).apply { if (index > 0) leftMargin = dp(6) })
                }
            })
        }, LinearLayout.LayoutParams(-1, dp(36)).apply { bottomMargin = dp(8) })
        val typingTest = TextInputEditText(this).apply {
            textSize = 16f
            hint = getString(R.string.typing_test_hint)
            setSingleLine(false)
            minHeight = dp(48)
            setPadding(dp(12), 0, dp(12), 0)
            setTextColor(primaryText)
            setHintTextColor(if (isDark) Color.rgb(148, 163, 184) else Color.rgb(107, 114, 128))
        }
        val typingField = TextInputLayout(this).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = cardColor
            boxStrokeColor = selectedColor
            setEndIconTintList(ColorStateList.valueOf(selectedColor))
            addView(typingTest, LinearLayout.LayoutParams(-1, -2))
        }
        val typingCardContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            addView(typingField, LinearLayout.LayoutParams(-1, -2))
        }
        val stickyTypingCard = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(cardColor)
            strokeColor = outlineColor
            strokeWidth = dp(2)
            addView(typingCardContent)
        }
        fun addSwitch(target: LinearLayout, label: Int, key: String, checked: Boolean): MaterialSwitch {
            return MaterialSwitch(this).apply {
                text = getString(label); textSize = 16f; isChecked = checked; setTextColor(primaryText)
                thumbTintList = checkedColors
                trackTintList = switchTrackColors
                setPadding(0, dp(4), 0, dp(4))
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
                addView(textView(getString(title), 18f, primaryText).apply {
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, 0, 0, dp(6))
                })
                build(this)
            }
            val sectionCard = MaterialCardView(this).apply {
                radius = dp(18).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(cardColor)
                strokeColor = outlineColor
                strokeWidth = dp(1)
                setContentPadding(dp(12), dp(12), dp(12), dp(12))
                addView(section)
            }
            sectionTargets[key] = sectionCard
            content.addView(sectionCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
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
            section.addView(MaterialCardView(this).apply {
                radius = dp(16).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(cardColor)
                strokeColor = outlineColor
                strokeWidth = dp(1)
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    addView(textView(getString(R.string.privacy_notice_title), 16f, primaryText).apply {
                        setTypeface(typeface, Typeface.BOLD)
                    })
                    addView(textView(getString(R.string.privacy_notice_body), 13f, secondaryText).apply {
                        setPadding(0, dp(6), 0, 0)
                    })
                })
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
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
            addSwitch(section, R.string.setting_auto_correct, KeyboardPreferences.AUTO_CORRECT, KeyboardPreferences.autoCorrect(this))
            addSwitch(section, R.string.setting_auto_capitalization, KeyboardPreferences.AUTO_CAPITALIZATION, KeyboardPreferences.autoCapitalization(this))
            addSwitch(section, R.string.setting_offline_mode, KeyboardPreferences.OFFLINE_MODE, KeyboardPreferences.offlineMode(this))
        }
        addSection(R.string.suggestions_language_title, "suggestions") { section ->
            addSwitch(section, R.string.setting_word_suggestions, KeyboardPreferences.WORD_SUGGESTIONS, KeyboardPreferences.wordSuggestions(this))
            section.addView(MaterialCardView(this).apply {
                radius = dp(14).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = selectedColor
                setCardBackgroundColor(cardColor)
                setContentPadding(dp(14), dp(12), dp(14), dp(12))
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(textView(getString(R.string.word_pack_title), 16f, primaryText).apply {
                        setTypeface(typeface, Typeface.BOLD)
                    })
                    val packStatus = textView("", 13f, secondaryText).apply { setPadding(0, dp(4), 0, dp(4)) }
                    fun secondaryButton() = MaterialButton(this@MainActivity).apply {
                        minWidth = 0
                        minimumHeight = dp(36)
                        textSize = 12f
                        insetTop = 0
                        insetBottom = 0
                        setTextColor(selectedColor)
                        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                        strokeColor = ColorStateList.valueOf(selectedColor)
                        strokeWidth = dp(1)
                    }
                    val viAction = secondaryButton()
                    val enAction = secondaryButton()
                    fun refreshPack() {
                        val viReady = WordDictionaryPack.isDownloaded(this@MainActivity, DictionaryLanguagePack.VIETNAMESE)
                        val enReady = WordDictionaryPack.isDownloaded(this@MainActivity, DictionaryLanguagePack.ENGLISH)
                        packStatus.text = getString(
                            R.string.word_pack_status,
                            if (viReady) "✓" else "—",
                            if (enReady) "✓" else "—",
                        )
                        viAction.text = getString(if (viReady) R.string.word_pack_vi_ready else R.string.word_pack_vi_download)
                        enAction.text = getString(if (enReady) R.string.word_pack_en_ready else R.string.word_pack_en_download)
                        viAction.isEnabled = true
                        enAction.isEnabled = true
                    }
                    fun bindAction(button: MaterialButton, pack: DictionaryLanguagePack) {
                        button.setOnClickListener {
                            if (WordDictionaryPack.isDownloaded(this@MainActivity, pack)) {
                                MaterialAlertDialogBuilder(this@MainActivity)
                                    .setTitle(R.string.word_pack_delete_title)
                                    .setMessage(R.string.word_pack_delete_message)
                                    .setNegativeButton(R.string.cancel, null)
                                    .setPositiveButton(R.string.word_pack_delete) { _, _ ->
                                        Thread({
                                            WordDictionaryPack.delete(this@MainActivity, pack)
                                            runOnUiThread { if (!isDestroyed) refreshPack() }
                                        }, "kai-word-pack-delete").start()
                                    }.show()
                            } else {
                                if (KeyboardPreferences.offlineMode(this@MainActivity)) {
                                    Toast.makeText(this@MainActivity, R.string.ngram_model_offline_error, Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                                button.isEnabled = false
                                packStatus.text = getString(R.string.word_pack_downloading)
                                Thread({
                                    val result = runCatching { WordDictionaryPack.download(this@MainActivity, pack) }
                                    runOnUiThread {
                                        if (isDestroyed) return@runOnUiThread
                                        result.onSuccess {
                                            refreshPack()
                                            Toast.makeText(this@MainActivity, R.string.word_pack_ready, Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            packStatus.text = it.message ?: getString(R.string.word_pack_download_error)
                                            button.isEnabled = true
                                        }
                                    }
                                }, "kai-word-pack-download").start()
                            }
                        }
                    }
                    addView(packStatus)
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        addView(viAction, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(4) })
                        addView(enAction, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(4) })
                    }, LinearLayout.LayoutParams(-1, -2))
                    addView(textView(getString(R.string.word_pack_attribution), 11f, secondaryText).apply {
                        setPadding(0, dp(4), 0, 0)
                    })
                    bindAction(viAction, DictionaryLanguagePack.VIETNAMESE)
                    bindAction(enAction, DictionaryLanguagePack.ENGLISH)
                    refreshPack()
                })
            }, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(10)
                bottomMargin = dp(4)
            })
            val learnedStatus = textView("", 13f, secondaryText).apply {
                setPadding(dp(4), dp(6), dp(4), dp(2))
            }
            var clearLearnedButton: MaterialButton? = null
            fun updateLearnedStatus() {
                val count = UserLexiconStore.count(this@MainActivity) +
                    EmailSuggestionStore.count(this@MainActivity) +
                    HashtagSuggestionStore.count(this@MainActivity)
                val usage = UserLexiconStore.totalUsage(this@MainActivity) +
                    EmailSuggestionStore.totalUsage(this@MainActivity) +
                    HashtagSuggestionStore.totalUsage(this@MainActivity)
                learnedStatus.text = getString(
                    R.string.learned_words_status,
                    count,
                    usage,
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
                        .setMessage(getString(
                            R.string.clear_learned_words_message,
                            UserLexiconStore.count(this@MainActivity) +
                                EmailSuggestionStore.count(this@MainActivity) +
                                HashtagSuggestionStore.count(this@MainActivity),
                        ))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.clear) { _, _ ->
                            UserLexiconStore.clear(this@MainActivity)
                            EmailSuggestionStore.clear(this@MainActivity)
                            HashtagSuggestionStore.clear(this@MainActivity)
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
            section.addView(MaterialButton(this).apply {
                text = getString(R.string.manage_api)
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(selectedColor)
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, ApiManagementActivity::class.java))
                }
            })
            val tones = AiTone.entries
            section.addView(textView("Giọng văn mặc định", 15f, primaryText).apply {
                setPadding(dp(2), dp(12), 0, dp(4))
            })
            section.addView(createChoiceButtons(
                tones.map { it.label },
                tones.indexOf(AiPreferences.tone(this@MainActivity)),
                primaryText, cardColor, selectedColor, ::dp,
            ) { AiPreferences.setTone(this@MainActivity, tones[it]) }, LinearLayout.LayoutParams(-1, dp(42)))
            val delayTitle = textView(
                "Tự gửi sau khi dừng gõ: ${AiPreferences.delaySeconds(this)} giây",
                15f,
                primaryText,
            )
            section.addView(delayTitle.apply { setPadding(dp(2), dp(12), 0, 0) })
            section.addView(SeekBar(this).apply {
                max = 9
                progress = AiPreferences.delaySeconds(this@MainActivity) - 1
                thumbTintList = ColorStateList.valueOf(selectedColor)
                progressTintList = ColorStateList.valueOf(selectedColor)
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
                text = "Tự gửi khi dừng gõ"
                textSize = 16f
                setTextColor(primaryText)
                isChecked = AiPreferences.autoSend(this@MainActivity)
                thumbTintList = checkedColors
                trackTintList = switchTrackColors
                setOnCheckedChangeListener { _, checked ->
                    AiPreferences.setAutoSend(this@MainActivity, checked)
                }
            })
        }
        addSection(R.string.tab_layout, "layout") { section ->
            addSwitch(section, R.string.setting_number_row, KeyboardPreferences.NUMBER_ROW, KeyboardPreferences.numberRow(this))
            addSwitch(section, R.string.setting_extended_symbols, KeyboardPreferences.EXTENDED_SYMBOLS, KeyboardPreferences.extendedSymbols(this))
            section.addView(createKeyboardPresetButtons(primaryText, cardColor, selectedColor, ::dp) {
                KeyboardPreferences.setBoolean(this@MainActivity, KeyboardPreferences.ADJUSTMENT_MODE, true)
                typingTest.requestFocus()
                typingTest.postDelayed({
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(typingTest, 0)
                }, 100L)
            }, LinearLayout.LayoutParams(-1, dp(42)).apply {
                topMargin = dp(8)
            })
            section.addView(textView(getString(R.string.setting_one_hand), 15f, primaryText).apply {
                setPadding(dp(2), dp(12), 0, dp(4))
            })
            section.addView(createOneHandButtons(primaryText, cardColor, selectedColor, ::dp), LinearLayout.LayoutParams(-1, dp(42)).apply {
                topMargin = dp(8)
            })
            section.addView(textView(getString(R.string.smartbar_order_title), 16f, primaryText).apply {
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(18), 0, 0)
            })
            section.addView(textView(getString(R.string.smartbar_order_note), 13f, secondaryText))
            val smartbarLabels = mapOf(
                "back" to "Ẩn bàn phím", "mic" to "Mic", "translate" to "Dịch",
                "ai" to "AI", "clipboard" to "Clipboard", "settings" to "Cài đặt", "emoji" to "Emoji",
            )
            val smartbarAdapter = SmartbarOrderAdapter(
                KeyboardPreferences.smartbarOrder(this).toMutableList(), smartbarLabels,
                primaryText, secondaryText, background, outlineColor,
            )
            section.addView(RecyclerView(this).apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = smartbarAdapter
                isNestedScrollingEnabled = false
                ItemTouchHelper(SmartbarTouchCallback(smartbarAdapter, selectedColor, outlineColor)).attachToRecyclerView(this)
            }, LinearLayout.LayoutParams(-1, dp(7 * 48)))
        }
        addSection(R.string.tab_appearance, "appearance") { section ->
            section.addView(textView(getString(R.string.setting_theme), 15f, primaryText).apply {
                setPadding(dp(2), 0, 0, dp(4))
            })
            section.addView(createThemeButtons(themeMode, primaryText, cardColor, selectedColor, ::dp), LinearLayout.LayoutParams(-1, dp(42)))
            section.addView(textView(getString(R.string.setting_color_style), 15f, primaryText).apply {
                setPadding(dp(2), dp(12), 0, dp(4))
            })
            section.addView(
                createColorStyleButtons(colorStyle, primaryText, cardColor, selectedColor, ::dp),
                LinearLayout.LayoutParams(-1, dp(42)),
            )
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
            val borderEnabled = KeyboardPreferences.keyBorder(this)
            val borderLabel = textView("", 16f, primaryText).apply { setPadding(0, dp(8), 0, 0) }
            fun updateBorderLabel(value: Int) {
                borderLabel.text = getString(R.string.setting_key_border_width_value, value)
            }
            val borderSlider = SeekBar(this).apply {
                max = 4
                progress = KeyboardPreferences.keyBorderWidthDp(this@MainActivity) - 1
                isEnabled = borderEnabled
                alpha = if (borderEnabled) 1f else 0.45f
                thumbTintList = ColorStateList.valueOf(selectedColor)
                progressTintList = ColorStateList.valueOf(selectedColor)
                progressBackgroundTintList = ColorStateList.valueOf(controlTrackInactive)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        val value = progress + 1
                        KeyboardPreferences.setKeyBorderWidthDp(this@MainActivity, value)
                        updateBorderLabel(value)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })
            }
            updateBorderLabel(KeyboardPreferences.keyBorderWidthDp(this))
            section.addView(MaterialSwitch(this).apply {
                text = getString(R.string.setting_key_border)
                textSize = 16f
                isChecked = borderEnabled
                setTextColor(primaryText)
                thumbTintList = checkedColors
                trackTintList = switchTrackColors
                setPadding(0, dp(10), 0, 0)
                setOnCheckedChangeListener { _, enabled ->
                    KeyboardPreferences.setBoolean(this@MainActivity, KeyboardPreferences.KEY_BORDER, enabled)
                    borderSlider.isEnabled = enabled
                    borderSlider.alpha = if (enabled) 1f else 0.45f
                    borderLabel.alpha = if (enabled) 1f else 0.45f
                }
            })
            borderLabel.alpha = if (borderEnabled) 1f else 0.45f
            section.addView(borderLabel)
            section.addView(borderSlider)
        }
        addSection(R.string.backup_settings_title, "backup") { section ->
            section.addView(MaterialButton(this).apply {
                text = getString(R.string.export_settings)
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(selectedColor)
                setOnClickListener {
                    startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TITLE, "kai-board-settings.json")
                    }, REQUEST_EXPORT_SETTINGS)
                }
            })
            section.addView(MaterialButton(this).apply {
                text = getString(R.string.import_settings)
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(selectedColor)
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
        }
        val root = FrameLayout(this).apply {
            if (screenGradient != null) this.background = screenGradient else setBackgroundColor(background)
            addView(settingsScroll, FrameLayout.LayoutParams(-1, -1))
            addView(stickyTypingCard, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
                leftMargin = dp(16); rightMargin = dp(16); bottomMargin = dp(6)
            })
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                stickyTypingCard.translationY = if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                    -ime.bottom.toFloat()
                } else {
                    0f
                }
                settingsScroll.setPadding(0, 0, 0, maxOf(systemBars.bottom, ime.bottom) + dp(76))
                insets
            }
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        if (restoreAppearance) {
            settingsScroll.post {
                sectionTargets["appearance"]?.let { target ->
                    settingsScroll.scrollTo(0, (target.top - dp(12)).coerceAtLeast(0))
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        val current = KeyboardPreferences.themeExtensionId(this)
        if (current != observedExtensionId) {
            observedExtensionId = current
            recreateAtAppearance()
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

    private fun createThemeButtons(themeMode: ThemeMode, primaryText: Int, fieldColor: Int, outlineColor: Int, dp: (Int) -> Int): HorizontalScrollView {
        val modes = ThemeMode.entries
        val labels = listOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark))
        return createChoiceButtons(labels, modes.indexOf(themeMode), primaryText, fieldColor, outlineColor, dp) { position ->
            if (modes[position] != KeyboardPreferences.theme(this@MainActivity)) {
                KeyboardPreferences.setTheme(this@MainActivity, modes[position])
                recreateAtAppearance()
            }
        }
    }

    private fun createColorStyleButtons(
        colorStyle: KeyboardColorStyle,
        primaryText: Int,
        fieldColor: Int,
        accentColor: Int,
        dp: (Int) -> Int,
    ): HorizontalScrollView {
        val activeId = KeyboardPreferences.themeExtensionId(this)
        val customActive = activeId != null && ThemeExtensionStore.installed(this).any { it.id == activeId }
        return createChoiceButtons(
            listOf(getString(R.string.color_classic), getString(R.string.color_custom)),
            if (customActive) 1 else 0, primaryText, fieldColor, accentColor, dp, selectOnClick = false,
        ) { position ->
            if (position == 0) {
                KeyboardPreferences.setColorStyle(this@MainActivity, KeyboardColorStyle.CLASSIC)
                recreateAtAppearance()
            } else {
                startActivity(Intent(this@MainActivity, CustomThemesActivity::class.java))
            }
        }
    }

    private fun createOneHandButtons(primaryText: Int, fieldColor: Int, accentColor: Int, dp: (Int) -> Int): HorizontalScrollView {
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
        return createChoiceButtons(labels, initial, primaryText, fieldColor, accentColor, dp) { position ->
            val oneHandWidth = 85
            val maxLeftDp = (resources.displayMetrics.widthPixels * (1f - oneHandWidth / 100f) /
                resources.displayMetrics.density).toInt().coerceAtLeast(0)
            KeyboardPreferences.setKeyboardGeometry(
                this@MainActivity,
                KeyboardPreferences.heightDp(this@MainActivity),
                KeyboardPreferences.bottomOffsetDp(this@MainActivity),
                if (position == 0) 100 else oneHandWidth,
                if (position == 2) maxLeftDp else 0,
            )
        }
    }

    private fun createChoiceButtons(
        labels: List<String>, initial: Int, primaryText: Int, fieldColor: Int, accentColor: Int,
        toPx: (Int) -> Int, selectOnClick: Boolean = true, onSelected: (Int) -> Unit,
    ): HorizontalScrollView {
        var selected = initial.coerceIn(labels.indices)
        val buttons = mutableListOf<MaterialButton>()
        fun refresh() = buttons.forEachIndexed { index, button ->
            val active = index == selected
            button.setTextColor(if (active) Color.WHITE else primaryText)
            button.backgroundTintList = ColorStateList.valueOf(if (active) accentColor else fieldColor)
            button.strokeColor = ColorStateList.valueOf(accentColor)
            button.strokeWidth = toPx(1)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            labels.forEachIndexed { index, label ->
                addView(MaterialButton(this@MainActivity).apply {
                    text = label; textSize = 13f
                    minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
                    insetTop = 0; insetBottom = 0
                    setPadding(toPx(12), 0, toPx(12), 0)
                    setOnClickListener {
                        if (selectOnClick) { selected = index; refresh() }
                        onSelected(index)
                    }
                }.also(buttons::add), LinearLayout.LayoutParams(-2, toPx(38)).apply {
                    if (index > 0) leftMargin = toPx(6)
                })
            }
        }
        refresh()
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            addView(row)
        }
    }

    private inner class SmartbarOrderAdapter(
        private val items: MutableList<String>,
        private val labels: Map<String, String>,
        private val textColor: Int,
        private val hintColor: Int,
        private val surfaceColor: Int,
        private val outlineColor: Int,
    ) : RecyclerView.Adapter<SmartbarOrderAdapter.Holder>() {
        private val density = resources.displayMetrics.density
        private fun px(value: Int) = (value * density).toInt()

        private val icons = mapOf(
            "back" to "⌄", "mic" to "🎤", "translate" to "文", "ai" to "AI",
            "clipboard" to "▣", "settings" to "⚙", "emoji" to "☺",
        )

        inner class Holder(val card: MaterialCardView, val icon: TextView, val title: TextView, val handle: TextView) :
            RecyclerView.ViewHolder(card)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val title = TextView(this@MainActivity).apply {
                textSize = 15f; setTextColor(textColor); gravity = Gravity.CENTER_VERTICAL
            }
            val icon = TextView(this@MainActivity).apply {
                textSize = 18f; setTextColor(textColor); gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
            }
            val handle = TextView(this@MainActivity).apply {
                text = "≡"; textSize = 24f; setTextColor(hintColor); gravity = Gravity.CENTER
                contentDescription = "Kéo để đổi vị trí"
            }
            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(icon, LinearLayout.LayoutParams(px(36), -1))
                addView(title, LinearLayout.LayoutParams(0, -1, 1f))
                addView(handle, LinearLayout.LayoutParams(px(36), -1))
            }
            val card = MaterialCardView(this@MainActivity).apply {
                radius = px(14).toFloat(); cardElevation = 0f
                setCardBackgroundColor(surfaceColor); strokeColor = outlineColor; strokeWidth = px(1)
                setContentPadding(px(8), 0, px(6), 0)
                layoutParams = RecyclerView.LayoutParams(-1, px(44)).apply { bottomMargin = px(4) }
                addView(row)
            }
            return Holder(card, icon, title, handle)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val id = items[position]
            holder.icon.text = icons[id].orEmpty()
            holder.icon.contentDescription = labels[id]
            holder.title.text = labels[id].orEmpty()
            holder.card.contentDescription = "${holder.title.text}, vị trí ${position + 1}. Chạm giữ để kéo."
        }

        override fun getItemCount() = items.size

        fun move(from: Int, to: Int): Boolean {
            if (from !in items.indices || to !in items.indices || from == to) return false
            val item = items.removeAt(from)
            items.add(to, item)
            // Keep the reorder visual-only while the finger is down. Writing preferences here
            // would rebuild the keyboard after every crossed row and interrupt long drags.
            notifyItemMoved(from, to)
            return true
        }

        fun persistOrder() = KeyboardPreferences.setSmartbarOrder(this@MainActivity, items)
    }

    private inner class SmartbarTouchCallback(
        private val adapter: SmartbarOrderAdapter,
        private val accentColor: Int,
        private val outlineColor: Int,
    ) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        private var orderChanged = false
        private val density = resources.displayMetrics.density

        override fun isLongPressDragEnabled() = true

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return adapter.move(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition).also {
                orderChanged = orderChanged || it
            }
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState != ItemTouchHelper.ACTION_STATE_DRAG || viewHolder == null) return
            (viewHolder.itemView.parent as? RecyclerView)?.parent?.requestDisallowInterceptTouchEvent(true)
            viewHolder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            (viewHolder.itemView as? MaterialCardView)?.apply {
                cardElevation = 10f * density; strokeWidth = (2f * density).toInt(); strokeColor = accentColor
                animate().scaleX(1.02f).scaleY(1.02f).setDuration(120L).start()
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            recyclerView.parent?.requestDisallowInterceptTouchEvent(false)
            (viewHolder.itemView as? MaterialCardView)?.apply {
                cardElevation = 0f; strokeWidth = density.toInt().coerceAtLeast(1); strokeColor = outlineColor
                animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
            }
            // Commit once, only after ItemTouchHelper reports that the finger was released.
            if (orderChanged) adapter.persistOrder()
            orderChanged = false
        }
    }

    private fun createKeyboardPresetButtons(
        primaryText: Int,
        fieldColor: Int,
        accentColor: Int,
        toPx: (Int) -> Int,
        onCustom: () -> Unit,
    ): HorizontalScrollView {
        data class Preset(val label: String, val heightDp: Int, val bottomDp: Int)
        val presets = listOf(
            Preset(getString(R.string.keyboard_preset_default), 220, 0),
            Preset(getString(R.string.keyboard_preset_raised), 220, 40),
            Preset(getString(R.string.keyboard_preset_tall), 250, 0),
        )
        val currentHeight = KeyboardPreferences.heightDp(this)
        val currentBottom = KeyboardPreferences.bottomOffsetDp(this)
        val currentWidth = KeyboardPreferences.widthPercent(this)
        val currentLeft = KeyboardPreferences.leftOffsetDp(this)
        var selected = presets.indexOfFirst {
            it.heightDp == currentHeight && it.bottomDp == currentBottom && currentWidth == 100 && currentLeft == 0
        }.takeIf { it >= 0 } ?: presets.size
        val buttons = mutableListOf<MaterialButton>()
        fun updateButtons() = buttons.forEachIndexed { index, button ->
            val active = index == selected
            button.setTextColor(if (active) Color.WHITE else primaryText)
            button.backgroundTintList = ColorStateList.valueOf(if (active) accentColor else fieldColor)
            button.strokeColor = ColorStateList.valueOf(accentColor)
            button.strokeWidth = toPx(1)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            (presets.map { it.label } + getString(R.string.keyboard_preset_custom)).forEachIndexed { index, label ->
                addView(MaterialButton(this@MainActivity).apply {
                    text = label
                    textSize = 13f
                    minWidth = 0; minimumWidth = 0
                    minHeight = 0; minimumHeight = 0
                    insetTop = 0; insetBottom = 0
                    setPadding(toPx(12), 0, toPx(12), 0)
                    setOnClickListener {
                        selected = index
                        updateButtons()
                        presets.getOrNull(index)?.let { preset ->
                            KeyboardPreferences.setBoolean(this@MainActivity, KeyboardPreferences.ADJUSTMENT_MODE, false)
                            KeyboardPreferences.setKeyboardGeometry(this@MainActivity, preset.heightDp, preset.bottomDp, 100, 0)
                            Toast.makeText(this@MainActivity, R.string.keyboard_preset_applied, Toast.LENGTH_SHORT).show()
                        } ?: onCustom()
                    }
                }.also(buttons::add), LinearLayout.LayoutParams(-2, toPx(38)).apply {
                    if (index > 0) leftMargin = toPx(6)
                })
            }
        }
        updateButtons()
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            addView(row)
        }
    }

}
