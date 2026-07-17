package vn.kai.board.input

import android.content.Context
import java.util.Locale

object PhraseLearningStore {
    private const val FILE = "phrase_learning"
    private const val KEY = "pairs"
    private const val ROW = "\u001F"
    private const val FIELD = "\u001E"
    private const val LIMIT = 256
    @Volatile private var cachedPairs: Map<String, Int>? = null
    private val defaults = mapOf(
        "cảm" to listOf("ơn"),
        "xin" to listOf("chào"),
        "việt" to listOf("nam"),
    )

    fun record(context: Context, first: String?, second: String) {
        val left = clean(first) ?: return
        val right = clean(second) ?: return
        if (left == right) return
        val pairs = read(context).toMutableMap()
        val key = "$left$FIELD$right"
        pairs[key] = (pairs[key] ?: 0) + 1
        write(context, pairs.entries.sortedByDescending { it.value }.take(LIMIT).associate { it.toPair() })
    }

    fun suggest(context: Context, previous: String?, limit: Int = 3): List<String> {
        val left = clean(previous) ?: return emptyList()
        val learned = read(context).entries.asSequence().mapNotNull { (key, count) ->
            val parts = key.split(FIELD, limit = 2)
            if (parts.size == 2 && parts[0] == left) parts[1] to count else null
        }.sortedByDescending { it.second }.map { it.first }.toList()
        return (learned + defaults[left].orEmpty()).distinct().take(limit)
    }

    private fun clean(value: String?): String? = value?.trim()?.lowercase(Locale.ROOT)
        ?.takeIf { it.matches(Regex("[\\p{L}Đđ]{2,32}")) }

    private fun read(context: Context): Map<String, Int> {
        cachedPairs?.let { return it }
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        .getString(KEY, "").orEmpty().split(ROW).mapNotNull { row ->
            val last = row.lastIndexOf(FIELD)
            if (last <= 0) null else row.substring(0, last) to (row.substring(last + 1).toIntOrNull() ?: 0)
        }.filter { it.second > 0 }.toMap().also { cachedPairs = it }
    }

    private fun write(context: Context, values: Map<String, Int>) {
        cachedPairs = values.toMap()
        val encoded = values.entries.joinToString(ROW) { "${it.key}$FIELD${it.value}" }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, encoded).apply()
    }
}
