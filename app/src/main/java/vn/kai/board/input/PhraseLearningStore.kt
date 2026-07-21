package vn.kai.board.input

import android.content.Context
import java.util.Locale

data class PhraseEntry(
    val words: List<String>,
    val count: Int = 1,
    val recentScoreMilli: Int = 1_000,
    val lastUsedEpochDay: Long = 0L,
)

/** UI-facing phrase row with decay applied for sorting/display. */
data class PhraseListItem(
    val words: List<String>,
    val count: Int,
    val decayScoreMilli: Int,
    val lastUsedEpochDay: Long,
    val daysSinceLastUse: Long,
) {
    fun label(): String = words.joinToString(" → ")
}

object PhraseLearningStore {
    private const val FILE = "phrase_learning"
    private const val LEGACY_KEY = "pairs"
    private const val KEY = "pairs_v2"
    private const val ROW = "\u001F"
    private const val FIELD = "\u001E"
    private const val LIMIT = 512
    private const val HALF_LIFE_DAYS = 21.0
    @Volatile private var cachedEntries: List<PhraseEntry>? = null

    fun record(context: Context, first: String?, second: String) {
        record(context, listOfNotNull(first), second)
    }

    fun record(context: Context, history: List<String>, next: String) {
        val contextWords = history.mapNotNull(::clean).takeLast(2)
        val right = clean(next) ?: return
        val left = contextWords.lastOrNull() ?: return
        if (left == right) return
        val values = entries(context).toMutableList()
        val today = epochDay()
        bump(values, listOf(left, right), today)
        if (contextWords.size == 2) bump(values, contextWords + right, today)
        write(
            context,
            values.sortedByDescending {
                decayedScoreMilli(it.recentScoreMilli, it.lastUsedEpochDay, today)
            }.take(LIMIT),
        )
    }

    fun suggest(context: Context, previous: String?, limit: Int = 3): List<String> =
        suggest(context, listOfNotNull(previous), limit)

    fun suggest(context: Context, history: List<String>, limit: Int = 3): List<String> {
        // Rank: personal (decay) → optional downloaded PhrasePack. No APK seed catalog.
        val personal = suggestPersonal(context, history, limit)
        val pack = suggestPack(context, history, limit)
        return SuggestionPriority.merge(emptyList(), personal, pack, limit)
    }

    fun suggestPersonal(context: Context, history: List<String>, limit: Int = 3): List<String> {
        val today = epochDay()
        val ranked = entries(context).map { entry ->
            entry.words to decayedScoreMilli(entry.recentScoreMilli, entry.lastUsedEpochDay, today)
        }.filter { it.second > 0 }
        return rankCandidates(ranked, history, limit)
    }

    /** Offline next-word from PhrasePack only (personal is separate). */
    fun suggestOffline(context: Context, history: List<String>, limit: Int = 3): List<String> =
        suggestPack(context, history, limit)

    /**
     * Next-word candidates from personal + pack that continue [history] and match [prefix]
     * (mid-word blend). Display case follows [prefix].
     */
    fun suggestMatchingPrefix(
        context: Context,
        history: List<String>,
        prefix: String,
        limit: Int = 3,
    ): List<String> {
        val needle = prefix.trim()
        if (needle.isEmpty() || limit <= 0) return emptyList()
        val personal = suggestPersonal(context, history, limit * 3)
        val pack = suggestPack(context, history, limit * 3)
        return (personal + pack)
            .asSequence()
            .filter { it.startsWith(needle, ignoreCase = true) }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .map { matchCase(it, needle) }
            .take(limit)
            .toList()
    }

    fun suggestPack(context: Context, history: List<String>, limit: Int = 3): List<String> {
        if (limit <= 0) return emptyList()
        val last = history.mapNotNull(::clean).lastOrNull() ?: return emptyList()
        return PhrasePack.continuations(context, last, limit)
    }

    fun count(context: Context): Int = entries(context).size

    /**
     * Personal phrases for management UI, sorted by current decay score.
     * [query] matches any word in the phrase (case-insensitive).
     */
    fun listEntries(context: Context, query: String = ""): List<PhraseListItem> {
        val today = epochDay()
        val needle = query.trim().lowercase(Locale.ROOT)
        return entries(context).map { entry ->
            PhraseListItem(
                words = entry.words,
                count = entry.count,
                decayScoreMilli = decayedScoreMilli(entry.recentScoreMilli, entry.lastUsedEpochDay, today),
                lastUsedEpochDay = entry.lastUsedEpochDay,
                daysSinceLastUse = if (entry.lastUsedEpochDay <= 0L) 0L
                else (today - entry.lastUsedEpochDay).coerceAtLeast(0L),
            )
        }.filter { item ->
            needle.isEmpty() || item.words.any { it.contains(needle) }
        }.sortedByDescending { it.decayScoreMilli }
    }

    fun clear(context: Context) {
        cachedEntries = emptyList()
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** Removes every bigram/trigram that contains [word] (any side). */
    fun removeInvolving(context: Context, word: String) {
        val target = clean(word) ?: return
        val remaining = entries(context).filterNot { entry ->
            entry.words.any { it.equals(target, ignoreCase = true) }
        }
        write(context, remaining)
    }

    /** Removes one exact bigram/trigram (words already cleaned or raw). */
    fun removeExact(context: Context, words: List<String>) {
        val key = words.mapNotNull(::clean)
        if (key.size !in 2..3) return
        write(context, entries(context).filterNot { it.words == key })
    }

    fun invalidateCache() {
        cachedEntries = null
    }

    internal fun rankCandidates(
        entries: List<Pair<List<String>, Int>>,
        history: List<String>,
        limit: Int = 3,
    ): List<String> {
        if (limit <= 0) return emptyList()
        val contextWords = history.mapNotNull(::clean).takeLast(2)
        val last = contextWords.lastOrNull() ?: return emptyList()
        val scores = LinkedHashMap<String, Int>()
        entries.forEach { (words, score) ->
            val boost = when {
                words.size == 3 && contextWords.size == 2 && words.take(2) == contextWords ->
                    10_000 + score
                words.size == 2 && words[0] == last ->
                    1_000 + score
                else -> return@forEach
            }
            val candidate = words.last()
            scores[candidate] = maxOf(scores[candidate] ?: Int.MIN_VALUE, boost)
        }
        return scores.entries.sortedByDescending { it.value }.map { it.key }.take(limit)
    }

    internal fun decayedScoreMilli(score: Int, lastDay: Long, currentDay: Long): Int {
        if (score <= 0 || lastDay <= 0L || currentDay <= lastDay) return score.coerceAtLeast(0)
        val age = (currentDay - lastDay).coerceAtMost(3_650L)
        return (score * Math.pow(0.5, age / HALF_LIFE_DAYS)).toInt().coerceAtLeast(0)
    }

    internal fun entries(context: Context): List<PhraseEntry> {
        cachedEntries?.let { return it }
        val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val v2 = preferences.getString(KEY, "").orEmpty()
        if (v2.isNotEmpty()) {
            return decodeV2(v2).also { cachedEntries = it }
        }
        val legacy = preferences.getString(LEGACY_KEY, "").orEmpty()
        val migrated = decodeLegacy(legacy)
        if (migrated.isNotEmpty()) write(context, migrated)
        return migrated.also { cachedEntries = it }
    }

    private fun bump(values: MutableList<PhraseEntry>, words: List<String>, today: Long) {
        val index = values.indexOfFirst { it.words == words }
        val old = values.getOrNull(index) ?: PhraseEntry(words, count = 0, recentScoreMilli = 0, lastUsedEpochDay = today)
        val recent = decayedScoreMilli(old.recentScoreMilli, old.lastUsedEpochDay, today) + 1_000
        val next = old.copy(
            count = old.count + 1,
            recentScoreMilli = recent,
            lastUsedEpochDay = today,
        )
        if (index >= 0) values[index] = next else values += next
    }

    private fun decodeLegacy(encoded: String): List<PhraseEntry> {
        if (encoded.isEmpty()) return emptyList()
        val today = epochDay()
        return encoded.split(ROW).mapNotNull { row ->
            val last = row.lastIndexOf(FIELD)
            if (last <= 0) return@mapNotNull null
            val key = row.substring(0, last)
            val count = row.substring(last + 1).toIntOrNull() ?: return@mapNotNull null
            if (count <= 0) return@mapNotNull null
            val words = key.split(FIELD).filter(String::isNotBlank)
            if (words.size !in 2..3) return@mapNotNull null
            PhraseEntry(words, count, count * 1_000, today)
        }
    }

    private fun decodeV2(encoded: String): List<PhraseEntry> =
        encoded.split(ROW).mapNotNull { row ->
            val parts = row.split(FIELD)
            if (parts.size < 5) return@mapNotNull null
            val count = parts[parts.lastIndex - 2].toIntOrNull() ?: return@mapNotNull null
            val score = parts[parts.lastIndex - 1].toIntOrNull() ?: return@mapNotNull null
            val day = parts.last().toLongOrNull() ?: return@mapNotNull null
            val words = parts.dropLast(3).filter(String::isNotBlank)
            if (words.size !in 2..3 || count <= 0) return@mapNotNull null
            PhraseEntry(words, count, score, day)
        }

    private fun write(context: Context, values: List<PhraseEntry>) {
        cachedEntries = values.toList()
        val encoded = values.joinToString(ROW) { entry ->
            (entry.words + listOf(entry.count.toString(), entry.recentScoreMilli.toString(), entry.lastUsedEpochDay.toString()))
                .joinToString(FIELD)
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY, encoded)
            .remove(LEGACY_KEY)
            .apply()
    }

    private fun clean(value: String?): String? = value?.trim()?.lowercase(Locale.ROOT)
        ?.takeIf { it.matches(Regex("[\\p{L}Đđ]{2,32}")) }

    private fun matchCase(word: String, prefix: String): String {
        if (prefix.isEmpty()) return word
        if (prefix.all { it.isUpperCase() }) return word.uppercase(Locale.ROOT)
        if (prefix.first().isUpperCase()) {
            return word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
        return word
    }

    private fun epochDay(): Long = System.currentTimeMillis() / 86_400_000L
}


