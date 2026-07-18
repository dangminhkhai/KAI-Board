package vn.kai.board.input

import android.content.Context
import org.json.JSONObject

object EmailSuggestionStore {
    private const val FILE = "email_suggestions"
    private const val LEGACY_KEY = "recent"
    private const val COUNTS_KEY = "frequency"
    private const val SEPARATOR = "\u001F"
    private const val LIMIT = 24

    fun read(context: Context): List<String> = readCounts(context).entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }

    fun remember(context: Context, value: String) {
        val email = value.trim().lowercase()
        if (!EmailSuggestionEngine.isCompleteEmail(email)) return
        val counts = readCounts(context).toMutableMap()
        counts[email] = (counts[email] ?: 0) + 1
        val limited = counts.entries.sortedByDescending { it.value }.take(LIMIT)
        val json = JSONObject().apply { limited.forEach { put(it.key, it.value) } }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(COUNTS_KEY, json.toString())
            .remove(LEGACY_KEY)
            .apply()
    }

    fun clear(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()

    fun count(context: Context) = readCounts(context).size
    fun totalUsage(context: Context) = readCounts(context).values.sum()

    private fun readCounts(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val stored = prefs.getString(COUNTS_KEY, null)
        if (!stored.isNullOrBlank()) return runCatching {
            val json = JSONObject(stored)
            buildMap { json.keys().forEach { key -> put(key, json.optInt(key, 1).coerceAtLeast(1)) } }
        }.getOrDefault(emptyMap())
        return prefs.getString(LEGACY_KEY, "").orEmpty().split(SEPARATOR)
            .filter(String::isNotBlank).associateWith { 1 }
    }
}
