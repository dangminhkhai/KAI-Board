package vn.kai.board.input

import android.content.Context
import org.json.JSONArray

object NoteStore {
    private const val FILE = "notes"
    private const val KEY = "items"
    fun read(context: Context): List<String> = runCatching {
        val array = JSONArray(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "[]"))
        List(array.length()) { array.getString(it) }
    }.getOrDefault(emptyList())
    fun save(context: Context, values: List<String>) {
        val array = JSONArray(); values.filter { it.isNotBlank() }.forEach(array::put)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
