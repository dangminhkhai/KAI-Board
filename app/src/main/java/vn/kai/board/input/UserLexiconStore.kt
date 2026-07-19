package vn.kai.board.input

import android.content.Context

enum class LearnSource { TYPED, SUGGESTION, AUTO_CORRECT }

data class UserWord(
    val word: String,
    val typed: Int = 0,
    val suggestion: Int = 0,
    val autoCorrect: Int = 0,
    val recentScoreMilli: Int = 0,
    val lastUsedEpochDay: Long = 0L,
) {
    val total: Int get() = typed + suggestion + autoCorrect
}

object UserLexiconStore {
    private const val FILE = "user_lexicon"
    private const val LEGACY_KEY = "words"
    private const val LEGACY_DETAIL_KEY = "word_details_v2"
    private const val KEY = "word_details_v3"
    private const val ENTRY_SEPARATOR = "\u001F"
    private const val FIELD_SEPARATOR = "\u001E"
    private const val LIMIT = 256
    private const val HALF_LIFE_DAYS = 14.0
    @Volatile private var cachedEntries: List<UserWord>? = null
    @Volatile private var cachedScores: Map<String, Int>? = null
    @Volatile private var cachedScoreDay: Long = Long.MIN_VALUE

    fun entries(context: Context): List<UserWord> {
        cachedEntries?.let { return it }
        val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val encoded = preferences.getString(KEY, "").orEmpty()
        if (encoded.isNotEmpty()) return decode(encoded).also { cachedEntries = it }
        val oldDetails = preferences.getString(LEGACY_DETAIL_KEY, "").orEmpty()
        if (oldDetails.isNotEmpty()) {
            val today = epochDay()
            val migrated = oldDetails.split(ENTRY_SEPARATOR).mapNotNull { row ->
                val p = row.split(FIELD_SEPARATOR)
                if (p.size != 4 || p[0].isBlank()) null else UserWord(
                    p[0], p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0,
                ).let { it.copy(recentScoreMilli = it.total * 1_000, lastUsedEpochDay = today) }
            }
            write(context, migrated)
            return migrated.also { cachedEntries = it }
        }
        val legacy = preferences.getString(LEGACY_KEY, "").orEmpty()
        val today = epochDay()
        val migrated = legacy.split(ENTRY_SEPARATOR).mapNotNull { row ->
            val parts = row.split(FIELD_SEPARATOR, limit = 2)
            val count = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            parts.firstOrNull()?.takeIf(String::isNotBlank)?.let {
                UserWord(it, typed = count, recentScoreMilli = count * 1_000, lastUsedEpochDay = today)
            }
        }
        if (migrated.isNotEmpty()) write(context, migrated)
        return migrated.also { cachedEntries = it }
    }

    fun read(context: Context): Map<String, Int> {
        val today = epochDay()
        cachedScores?.takeIf { cachedScoreDay == today }?.let { return it }
        return entries(context).associate { word ->
            word.word to (decayedScoreMilli(word.recentScoreMilli, word.lastUsedEpochDay, today) / 1_000)
        }.also { cachedScores = it; cachedScoreDay = today }
    }
    fun learn(context: Context, value: String) = record(context, value, LearnSource.TYPED, 1)

    fun record(context: Context, value: String, weight: Int) = record(context, value, LearnSource.TYPED, weight)

    fun record(context: Context, value: String, source: LearnSource, weight: Int = 1) {
        val word = value.trim()
        if (weight <= 0 || !word.matches(Regex("[\\p{L}Đđ]{2,32}"))) return
        val values = entries(context).toMutableList()
        val index = values.indexOfFirst { it.word.equals(word, true) }
        val old = values.getOrNull(index) ?: UserWord(word)
        val today = epochDay()
        val recent = decayedScoreMilli(old.recentScoreMilli, old.lastUsedEpochDay, today) + weight * 1_000
        val next = when (source) {
            LearnSource.TYPED -> old.copy(typed = old.typed + weight, recentScoreMilli = recent, lastUsedEpochDay = today)
            LearnSource.SUGGESTION -> old.copy(suggestion = old.suggestion + weight, recentScoreMilli = recent, lastUsedEpochDay = today)
            LearnSource.AUTO_CORRECT -> old.copy(autoCorrect = old.autoCorrect + weight, recentScoreMilli = recent, lastUsedEpochDay = today)
        }
        if (index >= 0) values[index] = next else values += next
        write(context, values.sortedByDescending {
            decayedScoreMilli(it.recentScoreMilli, it.lastUsedEpochDay, today)
        }.take(LIMIT))
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
            recentScoreMilli = current.recentScoreMilli + existing.recentScoreMilli,
            lastUsedEpochDay = maxOf(current.lastUsedEpochDay, existing.lastUsedEpochDay),
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
        val next = old.copy(
            autoCorrect = nextAuto,
            typed = (old.typed - remainder).coerceAtLeast(0),
            recentScoreMilli = (decayedScoreMilli(old.recentScoreMilli, old.lastUsedEpochDay, epochDay()) - weight * 1_000).coerceAtLeast(0),
            lastUsedEpochDay = epochDay(),
        )
        if (next.total == 0) values.removeAt(index) else values[index] = next
        write(context, values)
    }

    fun forget(context: Context, value: String) = write(context, entries(context).filterNot { it.word.equals(value, true) })
    fun clear(context: Context) {
        cachedEntries = emptyList()
        cachedScores = emptyMap()
        cachedScoreDay = epochDay()
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
    }
    fun count(context: Context): Int = entries(context).size
    fun totalUsage(context: Context): Int = entries(context).sumOf(UserWord::total)
    fun priorityScore(word: UserWord): Int = decayedScoreMilli(word.recentScoreMilli, word.lastUsedEpochDay, epochDay())
    fun daysSinceLastUse(word: UserWord): Long = (epochDay() - word.lastUsedEpochDay).coerceAtLeast(0L)
    fun resetPriority(context: Context, value: String) {
        val values = entries(context).toMutableList()
        val index = values.indexOfFirst { it.word.equals(value, true) }
        if (index < 0) return
        values[index] = values[index].copy(recentScoreMilli = 1_000, lastUsedEpochDay = epochDay())
        write(context, values)
    }
    fun invalidateCache() { cachedEntries = null; cachedScores = null; cachedScoreDay = Long.MIN_VALUE }

    private fun decode(encoded: String): List<UserWord> = encoded.split(ENTRY_SEPARATOR).mapNotNull { row ->
        val p = row.split(FIELD_SEPARATOR)
        if (p.size != 6 || p[0].isBlank()) null else UserWord(
            p[0], p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0,
            p[4].toIntOrNull() ?: 0, p[5].toLongOrNull() ?: 0L,
        )
    }

    private fun write(context: Context, values: List<UserWord>) {
        cachedEntries = values.toList()
        cachedScores = null
        cachedScoreDay = Long.MIN_VALUE
        val encoded = values.joinToString(ENTRY_SEPARATOR) {
            listOf(it.word, it.typed, it.suggestion, it.autoCorrect, it.recentScoreMilli, it.lastUsedEpochDay).joinToString(FIELD_SEPARATOR)
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY, encoded).remove(LEGACY_KEY).remove(LEGACY_DETAIL_KEY).apply()
    }

    internal fun decayedScoreMilli(score: Int, lastDay: Long, currentDay: Long): Int {
        if (score <= 0 || lastDay <= 0L || currentDay <= lastDay) return score.coerceAtLeast(0)
        val age = (currentDay - lastDay).coerceAtMost(3_650L)
        return (score * Math.pow(0.5, age / HALF_LIFE_DAYS)).toInt().coerceAtLeast(0)
    }

    private fun epochDay(): Long = System.currentTimeMillis() / 86_400_000L
}
