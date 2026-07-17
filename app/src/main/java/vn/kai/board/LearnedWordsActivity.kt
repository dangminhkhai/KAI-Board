package vn.kai.board

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.kai.board.input.UserLexiconStore
import vn.kai.board.input.UserWord
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.ThemeMode

class LearnedWordsActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        when (KeyboardPreferences.theme(this)) {
            ThemeMode.LIGHT -> setTheme(R.style.Theme_KAIBoard_Light)
            ThemeMode.DARK -> setTheme(R.style.Theme_KAIBoard_Dark)
            ThemeMode.SYSTEM -> setTheme(R.style.Theme_KAIBoard)
        }
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(20))
        }
        root.addView(TextView(this).apply { text = getString(R.string.manage_learned_words); textSize = 26f })
        root.addView(EditText(this).apply {
            hint = getString(R.string.search_learned_words)
            setSingleLine()
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { query = s.toString(); render() }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12); bottomMargin = dp(8) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        empty = TextView(this).apply { text = getString(R.string.no_learned_words); textSize = 15f; visibility = View.GONE }
        list.addView(empty)
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
        values.forEach { item -> list.addView(wordRow(item)) }
    }

    private fun wordRow(item: UserWord): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, 12, 0, 12)
        addView(LinearLayout(this@LearnedWordsActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@LearnedWordsActivity).apply { text = item.word; textSize = 18f })
            addView(TextView(this@LearnedWordsActivity).apply {
                text = getString(R.string.learned_word_counts, item.typed, item.suggestion, item.autoCorrect)
                textSize = 12f
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(MaterialButton(this@LearnedWordsActivity).apply {
            text = getString(R.string.edit)
            setOnClickListener { edit(item) }
        })
        addView(MaterialButton(this@LearnedWordsActivity).apply {
            text = getString(R.string.clear)
            setOnClickListener { UserLexiconStore.forget(this@LearnedWordsActivity, item.word); render() }
        })
    }

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
}
