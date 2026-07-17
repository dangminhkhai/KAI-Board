package vn.kai.board.input

import android.content.Context

object AutoCorrectionStatsStore {
    private const val FILE = "auto_correction_stats"

    fun accepted(context: Context, original: String, corrected: String) = increment(context, "ok", original, corrected)
    fun rejected(context: Context, original: String, corrected: String) = increment(context, "bad", original, corrected)

    fun summary(context: Context): Pair<Int, Int> {
        var accepted = 0
        var rejected = 0
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).all.forEach { (key, value) ->
            val count = value as? Int ?: return@forEach
            if (key.startsWith("ok\u001E")) accepted += count
            if (key.startsWith("bad\u001E")) rejected += count
        }
        return accepted to rejected
    }

    private fun increment(context: Context, kind: String, original: String, corrected: String) {
        val key = "$kind\u001E$original\u001E$corrected"
        val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        preferences.edit().putInt(key, preferences.getInt(key, 0) + 1).apply()
    }
}
