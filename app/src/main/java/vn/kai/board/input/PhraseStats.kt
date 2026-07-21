package vn.kai.board.input

import android.content.Context

/**
 * Local accept-rate counters for phrase suggestions.
 * **Never stores typed text, words, or phrase content** — only integer counters by source.
 */
object PhraseStats {
    private const val FILE = "phrase_stats_v1"
    private const val PERSONAL_SHOWN = "personal_shown"
    private const val PERSONAL_ACCEPTED = "personal_accepted"
    private const val PACK_SHOWN = "pack_shown"
    private const val PACK_ACCEPTED = "pack_accepted"

    enum class Source { PERSONAL, PACK }

    data class Snapshot(
        val personalShown: Long,
        val personalAccepted: Long,
        val packShown: Long,
        val packAccepted: Long,
    ) {
        fun personalRatePercent(): Int? = rate(personalAccepted, personalShown)
        fun packRatePercent(): Int? = rate(packAccepted, packShown)

        private fun rate(accepted: Long, shown: Long): Int? {
            if (shown <= 0L) return null
            return ((accepted * 100L) / shown).toInt().coerceIn(0, 100)
        }
    }

    fun recordShown(context: Context, personalCount: Int, packCount: Int) {
        if (personalCount <= 0 && packCount <= 0) return
        val prefs = prefs(context)
        prefs.edit()
            .putLong(PERSONAL_SHOWN, prefs.getLong(PERSONAL_SHOWN, 0L) + personalCount.coerceAtLeast(0))
            .putLong(PACK_SHOWN, prefs.getLong(PACK_SHOWN, 0L) + packCount.coerceAtLeast(0))
            .apply()
    }

    fun recordAccepted(context: Context, source: Source) {
        val key = when (source) {
            Source.PERSONAL -> PERSONAL_ACCEPTED
            Source.PACK -> PACK_ACCEPTED
        }
        val prefs = prefs(context)
        prefs.edit().putLong(key, prefs.getLong(key, 0L) + 1L).apply()
    }

    fun snapshot(context: Context): Snapshot {
        val prefs = prefs(context)
        return Snapshot(
            personalShown = prefs.getLong(PERSONAL_SHOWN, 0L),
            personalAccepted = prefs.getLong(PERSONAL_ACCEPTED, 0L),
            packShown = prefs.getLong(PACK_SHOWN, 0L),
            packAccepted = prefs.getLong(PACK_ACCEPTED, 0L),
        )
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
