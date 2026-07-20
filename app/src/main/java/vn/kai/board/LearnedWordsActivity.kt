package vn.kai.board

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import vn.kai.board.input.UserLexiconStore
import vn.kai.board.input.UserWord
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.KeyboardThemePalette
import vn.kai.board.settings.ThemeMode

class LearnedWordsActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private lateinit var status: TextView
    private var query = ""
    private var density = 1f
    private var primaryText = Color.BLACK
    private var secondaryText = Color.DKGRAY
    private var accent = Color.BLUE
    private var cardColor = Color.WHITE
    private var outlineColor = Color.LTGRAY

    override fun onCreate(savedInstanceState: Bundle?) {
        when (KeyboardPreferences.theme(this)) {
            ThemeMode.LIGHT -> setTheme(R.style.Theme_KAIBoard_Light)
            ThemeMode.DARK -> setTheme(R.style.Theme_KAIBoard_Dark)
            ThemeMode.SYSTEM -> setTheme(R.style.Theme_KAIBoard)
        }
        super.onCreate(savedInstanceState)
        density = resources.displayMetrics.density
        val mode = KeyboardPreferences.theme(this)
        val dark = mode == ThemeMode.DARK || mode == ThemeMode.SYSTEM &&
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val palette = KeyboardThemePalette.resolve(this, dark)
        primaryText = palette.text
        secondaryText = palette.hint
        accent = palette.accent
        cardColor = palette.key
        outlineColor = palette.specialKey
        window.statusBarColor = palette.gradientColors?.first() ?: palette.background
        window.navigationBarColor = palette.gradientColors?.last() ?: palette.background

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(20))
            palette.gradientColors?.let {
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, it)
            } ?: setBackgroundColor(palette.background)
        }
        root.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(MaterialButton(this@LearnedWordsActivity).apply {
                text = "‹"; textSize = 28f
                minWidth = 0; minimumWidth = 0
                setTextColor(primaryText)
                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                contentDescription = getString(R.string.back)
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(dp(46), dp(46)))
            addView(LinearLayout(this@LearnedWordsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@LearnedWordsActivity).apply {
                    text = getString(R.string.manage_learned_words)
                    textSize = 25f; setTextColor(primaryText); setTypeface(typeface, Typeface.BOLD)
                })
                status = TextView(this@LearnedWordsActivity).apply {
                    textSize = 13f; setTextColor(secondaryText)
                }
                addView(status)
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        })
        val search = EditText(this).apply {
            hint = getString(R.string.search_learned_words)
            setTextColor(primaryText)
            setHintTextColor(secondaryText)
            setSingleLine()
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    query = s.toString(); render()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        root.addView(TextInputLayout(this).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = cardColor
            boxStrokeColor = accent
            addView(search)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12); bottomMargin = dp(12) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        empty = TextView(this).apply {
            text = getString(R.string.no_learned_words)
            textSize = 15f
            setTextColor(secondaryText)
            setPadding(dp(12), dp(18), dp(12), dp(18))
            visibility = View.GONE
        }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        render()
    }

    private fun render() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val values = UserLexiconStore.entries(this).filter { it.word.contains(query.trim(), true) }
        val total = UserLexiconStore.count(this)
        status.text = if (query.isBlank()) getString(R.string.learned_words_count, total)
            else getString(R.string.learned_words_search_count, values.size, total)
        empty.visibility = if (values.isEmpty()) View.VISIBLE else View.GONE
        if (values.isEmpty()) list.addView(empty)
        values.forEach { list.addView(wordRow(it)) }
    }

    private fun wordRow(item: UserWord): View = MaterialCardView(this).apply {
        radius = dp(16).toFloat()
        cardElevation = 0f
        strokeWidth = dp(1)
        strokeColor = outlineColor
        setCardBackgroundColor(cardColor)
        addView(LinearLayout(this@LearnedWordsActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(10))
            addView(LinearLayout(this@LearnedWordsActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(this@LearnedWordsActivity).apply {
                    text = item.word; textSize = 20f; setTextColor(primaryText); setTypeface(typeface, Typeface.BOLD)
                    maxLines = 1
                }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(TextView(this@LearnedWordsActivity).apply {
                    text = getString(R.string.learned_word_score, UserLexiconStore.priorityScore(item) / 1000f)
                    textSize = 12f; setTextColor(accent); setTypeface(typeface, Typeface.BOLD)
                    background = GradientDrawable().apply {
                        setColor(Color.argb(24, Color.red(accent), Color.green(accent), Color.blue(accent)))
                        cornerRadius = dp(12).toFloat()
                    }
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                })
            })
            addView(TextView(this@LearnedWordsActivity).apply {
                text = getString(R.string.learned_word_counts, item.typed, item.suggestion, item.autoCorrect)
                textSize = 13f; setTextColor(secondaryText); setPadding(0, dp(8), 0, 0)
            })
            addView(TextView(this@LearnedWordsActivity).apply {
                val days = UserLexiconStore.daysSinceLastUse(item)
                text = getString(R.string.learned_word_last_used, if (days == 0L) getString(R.string.today) else getString(R.string.days_ago, days))
                textSize = 12f; setTextColor(secondaryText); setPadding(0, dp(2), 0, 0)
            })
            addView(LinearLayout(this@LearnedWordsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(cardButton(getString(R.string.reset_priority), false) {
                    UserLexiconStore.resetPriority(this@LearnedWordsActivity, item.word); render()
                }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(3) })
                addView(cardButton(getString(R.string.edit), true) { edit(item) }, LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                    marginStart = dp(3); marginEnd = dp(3)
                })
                addView(cardButton(getString(R.string.clear), false) { confirmDelete(item) }, LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                    marginStart = dp(3)
                })
            }, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(9) })
        })
    }.also { it.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) } }

    private fun edit(item: UserWord) {
        val input = EditText(this).apply { setText(item.word); selectAll() }
        MaterialAlertDialogBuilder(this).setTitle(R.string.edit_learned_word).setView(input)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val replacement = input.text.toString().trim()
                if (replacement.matches(Regex("[\\p{L}Đđ]{2,32}"))) {
                    UserLexiconStore.update(this, item.word, replacement)
                    render()
                }
            }.show()
    }

    private fun confirmDelete(item: UserWord) {
        MaterialAlertDialogBuilder(this).setTitle(R.string.delete_learned_word)
            .setMessage(getString(R.string.delete_learned_word_message, item.word))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear) { _, _ -> UserLexiconStore.forget(this, item.word); render() }
            .show()
    }

    private fun cardButton(title: String, filled: Boolean, action: () -> Unit) = MaterialButton(this).apply {
        text = title; textSize = 11f
        minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        insetTop = 0; insetBottom = 0; setPadding(dp(4), 0, dp(4), 0)
        setTextColor(if (filled) Color.WHITE else accent)
        backgroundTintList = ColorStateList.valueOf(if (filled) accent else Color.TRANSPARENT)
        if (!filled) { strokeColor = ColorStateList.valueOf(accent); strokeWidth = dp(1) }
        setOnClickListener { action() }
    }

    private fun dp(value: Int) = (value * density).toInt()
}
