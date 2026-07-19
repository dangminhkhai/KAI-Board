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
        root.addView(TextView(this).apply {
            text = getString(R.string.manage_learned_words)
            textSize = 28f
            setTextColor(primaryText)
            setTypeface(typeface, Typeface.BOLD)
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
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(10), dp(10), dp(10))
            addView(LinearLayout(this@LearnedWordsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@LearnedWordsActivity).apply {
                    text = item.word; textSize = 18f; setTextColor(primaryText); setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(this@LearnedWordsActivity).apply {
                    text = getString(R.string.learned_word_counts, item.typed, item.suggestion, item.autoCorrect)
                    textSize = 12f; setTextColor(secondaryText)
                })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(MaterialButton(this@LearnedWordsActivity).apply {
                text = getString(R.string.edit)
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(accent)
                setOnClickListener { edit(item) }
            })
            addView(MaterialButton(this@LearnedWordsActivity).apply {
                text = getString(R.string.clear)
                setTextColor(accent)
                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                strokeColor = ColorStateList.valueOf(accent)
                strokeWidth = dp(1)
                setOnClickListener { UserLexiconStore.forget(this@LearnedWordsActivity, item.word); render() }
            })
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

    private fun dp(value: Int) = (value * density).toInt()
}
