package vn.kai.board.input

import android.content.Context
import org.json.JSONObject

object HashtagSuggestionStore {
    private const val FILE = "hashtag_suggestions"
    private const val KEY = "frequency"
    private const val LIMIT = 48

    fun read(context: Context): List<String> = readCounts(context).entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }

    fun remember(context: Context, value: String) {
        val hashtag = value.trim()
        if (!HashtagSuggestionEngine.isComplete(hashtag)) return
        val counts = readCounts(context).toMutableMap()
        val existing = counts.keys.firstOrNull { it.equals(hashtag, true) }
        val previousCount = existing?.let { counts[it] } ?: counts[hashtag] ?: 0
        if (existing != null && existing != hashtag) counts.remove(existing)
        counts[hashtag] = previousCount + 1
        val json = JSONObject().apply {
            counts.entries.sortedByDescending { it.value }.take(LIMIT).forEach { put(it.key, it.value) }
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, json.toString()).apply()
    }

    fun clear(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()

    fun count(context: Context) = readCounts(context).size
    fun totalUsage(context: Context) = readCounts(context).values.sum()

    private fun readCounts(context: Context): Map<String, Int> {
        val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyMap()
        return runCatching {
            val json = JSONObject(stored)
            buildMap { json.keys().forEach { key -> put(key, json.optInt(key, 1).coerceAtLeast(1)) } }
        }.getOrDefault(emptyMap())
    }
}
