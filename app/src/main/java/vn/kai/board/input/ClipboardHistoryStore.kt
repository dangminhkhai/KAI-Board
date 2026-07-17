package vn.kai.board.input

import android.content.Context
import org.json.JSONArray

object ClipboardHistoryStore {
    private const val FILE = "clipboard_history"
    private const val KEY = "text_items"
    private const val LIMIT = 12
    data class Entry(val text: String, val pinned: Boolean)

    fun add(context: Context, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val old = readEntries(context)
        val updated = listOf(Entry(clean, old.firstOrNull { it.text == clean }?.pinned == true)) + old.filterNot { it.text == clean }
        val array = JSONArray()
        updated.take(LIMIT).forEach { array.put(org.json.JSONObject().put("text", it.text).put("pinned", it.pinned)) }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    fun read(context: Context): List<String> = readEntries(context).map { it.text }

    fun readEntries(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val value = array.get(index)
                if (value is org.json.JSONObject) Entry(value.getString("text"), value.optBoolean("pinned"))
                else Entry(value.toString(), false)
            }.sortedByDescending { it.pinned }
        }.getOrDefault(emptyList())
    }

    fun togglePinned(context: Context, text: String) = update(context) { entries ->
        entries.map { if (it.text == text) it.copy(pinned = !it.pinned) else it }
    }

    fun delete(context: Context, text: String) = update(context) { it.filterNot { entry -> entry.text == text } }

    private fun update(context: Context, transform: (List<Entry>) -> List<Entry>) {
        val array = JSONArray()
        transform(readEntries(context)).forEach { array.put(org.json.JSONObject().put("text", it.text).put("pinned", it.pinned)) }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
