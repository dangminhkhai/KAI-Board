package vn.kai.board.input

import android.content.Context
import org.json.JSONArray
import vn.kai.board.settings.KeyboardPreferences

object ClipboardHistoryStore {
    private const val FILE = "clipboard_history"
    private const val KEY = "text_items"
    private const val LIMIT = 12
    data class Entry(val text: String, val pinned: Boolean, val createdAt: Long = System.currentTimeMillis())

    fun add(context: Context, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val old = readEntries(context)
        val updated = listOf(Entry(clean, old.firstOrNull { it.text == clean }?.pinned == true)) + old.filterNot { it.text == clean }
        write(context, updated.take(LIMIT))
    }

    fun read(context: Context): List<String> = readEntries(context).map { it.text }

    fun readEntries(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        var needsMigration = false
        val entries = runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val value = array.get(index)
                if (value is org.json.JSONObject) {
                    if (!value.has("createdAt")) needsMigration = true
                    Entry(value.getString("text"), value.optBoolean("pinned"), value.optLong("createdAt", System.currentTimeMillis()))
                } else {
                    needsMigration = true
                    Entry(value.toString(), false)
                }
            }
        }.getOrDefault(emptyList())
        val duration = KeyboardPreferences.clipboardExpiry(context).durationMillis
        val filtered = if (duration == null) entries else entries.filter { it.pinned || System.currentTimeMillis() - it.createdAt < duration }
        if (needsMigration || filtered.size != entries.size) write(context, filtered)
        return filtered.sortedByDescending { it.pinned }
    }

    fun togglePinned(context: Context, text: String) = update(context) { entries ->
        entries.map { if (it.text == text) it.copy(pinned = !it.pinned) else it }
    }

    fun delete(context: Context, text: String) = update(context) { it.filterNot { entry -> entry.text == text } }

    private fun update(context: Context, transform: (List<Entry>) -> List<Entry>) {
        write(context, transform(readEntries(context)))
    }

    private fun write(context: Context, entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { array.put(org.json.JSONObject().put("text", it.text).put("pinned", it.pinned).put("createdAt", it.createdAt)) }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
