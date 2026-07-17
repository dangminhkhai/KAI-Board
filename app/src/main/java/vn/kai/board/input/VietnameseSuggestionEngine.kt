package vn.kai.board.input

import java.io.BufferedReader
import java.io.Reader
import java.text.Normalizer
import java.util.Locale

/** Offline frequency dictionary. Asset loading happens once, never in the key-dispatch path. */
object VietnameseSuggestionEngine {
    private val fallbackWords = listOf(
        "và", "là", "có", "không", "được", "một", "cho", "của", "trong", "người",
        "này", "với", "tôi", "bạn", "đã", "đang", "sẽ", "rất", "nhưng", "khi",
        "xin", "xinh", "xin chào", "chào", "cháo", "chao", "cảm", "cảm ơn", "ơn",
        "tiếng", "tiền", "tiếp", "tiện", "tiến", "Việt", "Việt Nam", "Nam",
        "hôm", "hôm nay", "ngày", "mai", "bây giờ", "thời gian", "công", "cộng", "cong",
        "chúng", "chúng ta", "mình", "muốn", "cần", "làm", "thêm", "sửa", "kiểm tra",
        "nhanh", "chậm", "đúng", "sai", "tốt", "ổn", "cảm giác", "bàn phím", "điện thoại",
        "ứng dụng", "Android", "email", "tin nhắn", "ghi chú", "cài đặt", "giao diện",
        "học", "gõ", "từ", "gợi ý", "tiếng Anh", "English", "hello", "thank", "thanks",
        "the", "this", "that", "with", "from", "android", "application", "keyboard",
    )

    @Volatile private var dictionary = DictionaryIndex(fallbackWords)
    private val contextPairs = mapOf("cam" to setOf("on"), "xin" to setOf("chao"), "viet" to setOf("nam"))
    private val autoCorrectOverrides = mapOf("dang" to "đang")

    /** Replaces the fallback with the ranked asset plus the useful built-in phrases. */
    fun load(reader: Reader): Int {
        val loaded = ArrayList<String>(20_000)
        BufferedReader(reader).useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank() && !line.startsWith('#')) {
                    val word = line.substringAfter('\t', "").trim()
                    if (word.isNotEmpty()) loaded += word
                }
            }
        }
        if (loaded.isEmpty()) return 0
        dictionary = DictionaryIndex(loaded + fallbackWords)
        return loaded.size
    }

    fun suggest(
        query: String,
        limit: Int = 3,
        learned: Map<String, Int> = emptyMap(),
        previousWord: String? = null,
    ): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || limit <= 0 || trimmed.length > 32) return emptyList()
        val needle = normalized(trimmed)
        if (needle.isEmpty()) return emptyList()
        val previous = previousWord?.let(::normalized)
        val learnedEntries = learned.entries.mapIndexed { rank, item ->
            Entry(item.key, normalized(item.key), -rank, item.value)
        }
        val candidates = dictionary.candidates(needle) + learnedEntries
        val matches = candidates.asSequence()
            .filter { it.normalized.startsWith(needle) || (needle.length >= 4 && boundedEditDistance(it.normalized, needle, 1) <= 1) }
            .distinctBy { it.word.lowercase(Locale.ROOT) }
            .sortedBy { entry ->
                val match = when {
                    entry.word.equals(trimmed, ignoreCase = true) -> 0
                    entry.normalized == needle -> 100
                    entry.normalized.startsWith(needle) -> 500
                    else -> 2_500
                }
                val contextBoost = if (previous != null && entry.normalized in contextPairs[previous].orEmpty()) 400 else 0
                match + entry.rank - entry.frequency * 120 - contextBoost
            }
            .map { matchCase(it.word, trimmed) }
            .take(limit)
            .toMutableList()
        if (matches.none { it == trimmed }) matches.add(0, trimmed)
        return matches.distinct().take(limit)
    }

    fun bestAutoCorrection(
        query: String,
        learned: Map<String, Int> = emptyMap(),
        previousWord: String? = null,
    ): String? {
        if (!isAutoCorrectionEligible(query)) return null
        if (learned.keys.any { it.equals(query, true) }) return null
        val normalizedQuery = normalized(query)
        autoCorrectOverrides[normalizedQuery]?.let { forced ->
            if (!forced.equals(query, false)) return matchCase(forced, query)
        }
        if (dictionary.containsExact(query)) return null
        return suggest(query, 6, learned, previousWord).firstOrNull { candidate ->
            !candidate.equals(query, false) && normalized(candidate).let {
                it == normalizedQuery || boundedEditDistance(it, normalizedQuery, 1) <= 1
            }
        }
    }

    internal fun isAutoCorrectionEligible(query: String): Boolean {
        if (query.any(Char::isDigit)) return false
        if (query.length < 2 || query.any { !it.isLetter() }) return false
        // Capitalized words and all-caps abbreviations are intentionally left untouched.
        if (query.first().isUpperCase() || query.count(Char::isUpperCase) >= 2) return false
        return true
    }

    internal fun candidatePoolSizeForTest(words: List<String>, query: String): Int =
        DictionaryIndex(words).candidates(normalized(query)).size

    private class DictionaryIndex(words: List<String>) {
        private val exact: Set<String>
        private val prefixes: Map<String, List<Entry>>
        private val fuzzy: Map<String, List<Entry>>

        init {
            val unique = LinkedHashMap<String, Entry>(words.size)
            words.forEachIndexed { rank, word ->
                val normalized = normalized(word)
                if (normalized.isNotEmpty()) unique.putIfAbsent(word.lowercase(Locale.ROOT), Entry(word, normalized, rank))
            }
            val entries = unique.values.toList()
            exact = entries.mapTo(HashSet(entries.size)) { it.word.lowercase(Locale.ROOT) }
            val prefixBuilder = HashMap<String, MutableList<Entry>>()
            entries.forEach { entry ->
                prefixBuilder.getOrPut(entry.normalized.take(1)) { ArrayList() }.add(entry)
                if (entry.normalized.length >= 2) {
                    prefixBuilder.getOrPut(entry.normalized.take(2)) { ArrayList() }.add(entry)
                }
            }
            prefixes = prefixBuilder
            fuzzy = entries.groupBy { fuzzyKey(it.normalized.first(), it.normalized.length) }
        }

        fun containsExact(word: String): Boolean = word.lowercase(Locale.ROOT) in exact

        fun candidates(needle: String): List<Entry> {
            if (needle.isEmpty()) return emptyList()
            val result = LinkedHashMap<String, Entry>()
            prefixes[prefixKey(needle)].orEmpty().forEach { result.putIfAbsent(it.word, it) }
            if (needle.length >= 4) {
                for (length in needle.length - 1..needle.length + 1) {
                    fuzzy[fuzzyKey(needle.first(), length)].orEmpty().forEach { result.putIfAbsent(it.word, it) }
                }
            }
            return result.values.toList()
        }
    }

    private fun prefixKey(value: String): String = value.take(2)
    private fun fuzzyKey(first: Char, length: Int): String = "$first:$length"

    /** Stops as soon as a row proves the distance is above [limit]. */
    private fun boundedEditDistance(first: String, second: String, limit: Int): Int {
        if (kotlin.math.abs(first.length - second.length) > limit) return limit + 1
        var previous = IntArray(second.length + 1) { it }
        first.forEachIndexed { i, a ->
            val current = IntArray(second.length + 1)
            current[0] = i + 1
            var rowMinimum = current[0]
            second.forEachIndexed { j, b ->
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + if (a == b) 0 else 1)
                rowMinimum = minOf(rowMinimum, current[j + 1])
            }
            if (rowMinimum > limit) return limit + 1
            previous = current
        }
        return previous.last()
    }

    private fun normalized(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase(Locale.ROOT)

    private fun matchCase(value: String, query: String): String = when {
        query.all(Char::isUpperCase) -> value.uppercase(Locale.ROOT)
        query.firstOrNull()?.isUpperCase() == true -> value.replaceFirstChar { it.titlecase(Locale.ROOT) }
        else -> value
    }

    private data class Entry(val word: String, val normalized: String, val rank: Int, val frequency: Int = 0)
}
