package vn.kai.board.ai

import android.content.Context

/** Stores only short word transitions from commands the user actually sends. */
object AiCommandSuggestionStore {
    private const val FILE = "ai_command_suggestions"
    private const val KEY = "transitions"
    private const val ROW = "\u001F"
    private const val COUNT = "\u001D"
    private const val LIMIT = 1_024
    @Volatile private var cache: Map<String, Int>? = null

    fun read(context: Context): Map<String, Int> {
        cache?.let { return it }
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, "").orEmpty()
            .split(ROW)
            .mapNotNull { row ->
                val split = row.lastIndexOf(COUNT)
                if (split <= 0) null else row.substring(0, split) to (row.substring(split + 1).toIntOrNull() ?: 0)
            }
            .filter { it.second > 0 }
            .toMap()
            .also { cache = it }
    }

    fun record(context: Context, prompt: String) {
        val additions = AiCommandSuggestionEngine.learnedTransitions(prompt)
        if (additions.isEmpty()) return
        val updated = read(context).toMutableMap()
        additions.forEach { (key, count) -> updated[key] = (updated[key] ?: 0) + count }
        val limited = updated.entries.sortedByDescending { it.value }.take(LIMIT).associate { it.toPair() }
        cache = limited
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY, limited.entries.joinToString(ROW) { "${it.key}$COUNT${it.value}" })
            .apply()
    }
}
