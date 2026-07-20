package vn.kai.board.input

import android.content.Context
import java.util.Locale

object PhraseLearningStore {
    private const val FILE = "phrase_learning"
    private const val KEY = "pairs"
    private const val ROW = "\u001F"
    private const val FIELD = "\u001E"
    private const val LIMIT = 512
    @Volatile private var cachedPairs: Map<String, Int>? = null

    fun record(context: Context, first: String?, second: String) {
        record(context, listOfNotNull(first), second)
    }

    fun record(context: Context, history: List<String>, next: String) {
        val contextWords = history.mapNotNull(::clean).takeLast(2)
        val right = clean(next) ?: return
        val left = contextWords.lastOrNull() ?: return
        if (left == right) return
        val pairs = read(context).toMutableMap()
        increment(pairs, listOf(left, right))
        if (contextWords.size == 2) increment(pairs, contextWords + right)
        write(context, pairs.entries.sortedByDescending { it.value }.take(LIMIT).associate { it.toPair() })
    }

    fun suggest(context: Context, previous: String?, limit: Int = 3): List<String> {
        return suggest(context, listOfNotNull(previous), limit)
    }

    fun suggest(context: Context, history: List<String>, limit: Int = 3): List<String> =
        suggestPersonal(context, history, limit)

    fun suggestPersonal(context: Context, history: List<String>, limit: Int = 3): List<String> = rankCandidates(
        read(context).map { (key, count) -> key.split(FIELD) to count }, history, limit,
    )

    fun suggestOffline(context: Context, history: List<String>, limit: Int = 3): List<String> = emptyList()

    internal fun rankCandidates(
        entries: List<Pair<List<String>, Int>>,
        history: List<String>,
        limit: Int = 3,
    ): List<String> {
        if (limit <= 0) return emptyList()
        val contextWords = history.mapNotNull(::clean).takeLast(2)
        val last = contextWords.lastOrNull() ?: return emptyList()
        val scores = LinkedHashMap<String, Int>()
        entries.forEach { (words, count) ->
            val score = when {
                words.size == 3 && contextWords.size == 2 && words.take(2) == contextWords -> 10_000 + count * 10
                words.size == 2 && words[0] == last -> 1_000 + count * 10
                else -> return@forEach
            }
            val candidate = words.last()
            scores[candidate] = maxOf(scores[candidate] ?: Int.MIN_VALUE, score)
        }
        return scores.entries.sortedByDescending { it.value }.map { it.key }.take(limit)
    }

    fun invalidateCache() { cachedPairs = null }

    private fun clean(value: String?): String? = value?.trim()?.lowercase(Locale.ROOT)
        ?.takeIf { it.matches(Regex("[\\p{L}Đđ]{2,32}")) }

    private fun increment(values: MutableMap<String, Int>, words: List<String>) {
        val key = words.joinToString(FIELD)
        values[key] = (values[key] ?: 0) + 1
    }

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
