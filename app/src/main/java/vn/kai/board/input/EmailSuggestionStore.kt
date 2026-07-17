package vn.kai.board.input

import android.content.Context

object EmailSuggestionStore {
    private const val FILE = "email_suggestions"
    private const val KEY = "recent"
    private const val SEPARATOR = "\u001F"
    private const val LIMIT = 12

    fun read(context: Context): List<String> = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        .getString(KEY, "").orEmpty().split(SEPARATOR).filter(String::isNotBlank)

    fun remember(context: Context, value: String) {
        val email = value.trim().lowercase()
        if (!EmailSuggestionEngine.isCompleteEmail(email)) return
        val next = (listOf(email) + read(context).filterNot { it.equals(email, true) }).take(LIMIT)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, next.joinToString(SEPARATOR)).apply()
    }
}
