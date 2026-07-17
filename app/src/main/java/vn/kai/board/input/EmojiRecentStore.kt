package vn.kai.board.input

import android.content.Context
import org.json.JSONArray

object EmojiRecentStore {
    private const val FILE = "emoji_recent"
    private const val KEY = "items"
    private const val LIMIT = 30
    fun read(context: Context): List<String> = runCatching {
        val array = JSONArray(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "[]"))
        List(array.length()) { array.getString(it) }
    }.getOrDefault(emptyList())
    fun add(context: Context, emoji: String) {
        val values = listOf(emoji) + read(context).filterNot { it == emoji }
        val array = JSONArray(); values.take(LIMIT).forEach(array::put)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
