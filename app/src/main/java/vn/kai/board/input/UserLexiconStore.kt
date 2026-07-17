package vn.kai.board.input

import android.content.Context

enum class LearnSource { TYPED, SUGGESTION, AUTO_CORRECT }

data class UserWord(
    val word: String,
    val typed: Int = 0,
    val suggestion: Int = 0,
    val autoCorrect: Int = 0,
) {
    val total: Int get() = typed + suggestion + autoCorrect
}

object UserLexiconStore {
    private const val FILE = "user_lexicon"
    private const val LEGACY_KEY = "words"
    private const val KEY = "word_details_v2"
    private const val ENTRY_SEPARATOR = "\u001F"
    private const val FIELD_SEPARATOR = "\u001E"
    private const val LIMIT = 256
    @Volatile private var cachedEntries: List<UserWord>? = null

    fun entries(context: Context): List<UserWord> {
        cachedEntries?.let { return it }
        val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val encoded = preferences.getString(KEY, "").orEmpty()
        if (encoded.isNotEmpty()) return decode(encoded).also { cachedEntries = it }
        val legacy = preferences.getString(LEGACY_KEY, "").orEmpty()
        val migrated = legacy.split(ENTRY_SEPARATOR).mapNotNull { row ->
            val parts = row.split(FIELD_SEPARATOR, limit = 2)
            val count = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            parts.firstOrNull()?.takeIf(String::isNotBlank)?.let { UserWord(it, typed = count) }
        }
        if (migrated.isNotEmpty()) write(context, migrated)
        return migrated.also { cachedEntries = it }
    }

    fun read(context: Context): Map<String, Int> = entries(context).associate { it.word to it.total }
    fun learn(context: Context, value: String) = record(context, value, LearnSource.TYPED, 1)

    fun record(context: Context, value: String, weight: Int) = record(context, value, LearnSource.TYPED, weight)

    fun record(context: Context, value: String, source: LearnSource, weight: Int = 1) {
        val word = value.trim()
        if (weight <= 0 || !word.matches(Regex("[\\p{L}Đđ]{2,32}"))) return
        val values = entries(context).toMutableList()
        val index = values.indexOfFirst { it.word.equals(word, true) }
        val old = values.getOrNull(index) ?: UserWord(word)
        val next = when (source) {
            LearnSource.TYPED -> old.copy(typed = old.typed + weight)
            LearnSource.SUGGESTION -> old.copy(suggestion = old.suggestion + weight)
            LearnSource.AUTO_CORRECT -> old.copy(autoCorrect = old.autoCorrect + weight)
        }
        if (index >= 0) values[index] = next else values += next
        write(context, values.sortedByDescending(UserWord::total).take(LIMIT))
    }

    fun update(context: Context, original: String, replacement: String) {
        val existing = entries(context).firstOrNull { it.word.equals(original, true) } ?: return
        forget(context, original)
        val merged = entries(context).toMutableList()
        val target = merged.indexOfFirst { it.word.equals(replacement.trim(), true) }
        val current = merged.getOrNull(target) ?: UserWord(replacement.trim())
        val next = current.copy(
            typed = current.typed + existing.typed,
            suggestion = current.suggestion + existing.suggestion,
            autoCorrect = current.autoCorrect + existing.autoCorrect,
        )
        if (target >= 0) merged[target] = next else merged += next
        write(context, merged)
    }

    fun decrement(context: Context, value: String, weight: Int) {
        if (weight <= 0) return
        val values = entries(context).toMutableList()
        val index = values.indexOfFirst { it.word.equals(value.trim(), true) }
        if (index < 0) return
        val old = values[index]
        val nextAuto = (old.autoCorrect - weight).coerceAtLeast(0)
        val remainder = (weight - old.autoCorrect).coerceAtLeast(0)
        val next = old.copy(autoCorrect = nextAuto, typed = (old.typed - remainder).coerceAtLeast(0))
        if (next.total == 0) values.removeAt(index) else values[index] = next
        write(context, values)
    }

    fun forget(context: Context, value: String) = write(context, entries(context).filterNot { it.word.equals(value, true) })
    fun clear(context: Context) {
        cachedEntries = emptyList()
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
    }
    fun count(context: Context): Int = entries(context).size
    fun totalUsage(context: Context): Int = entries(context).sumOf(UserWord::total)
    fun invalidateCache() { cachedEntries = null }

    private fun decode(encoded: String): List<UserWord> = encoded.split(ENTRY_SEPARATOR).mapNotNull { row ->
        val p = row.split(FIELD_SEPARATOR)
        if (p.size != 4 || p[0].isBlank()) null else UserWord(
            p[0], p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0,
        )
    }

    private fun write(context: Context, values: List<UserWord>) {
        cachedEntries = values.toList()
        val encoded = values.joinToString(ENTRY_SEPARATOR) {
            listOf(it.word, it.typed, it.suggestion, it.autoCorrect).joinToString(FIELD_SEPARATOR)
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, encoded).remove(LEGACY_KEY).apply()
    }
}
