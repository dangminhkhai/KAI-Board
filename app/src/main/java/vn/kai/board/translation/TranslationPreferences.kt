package vn.kai.board.translation

import android.content.Context

object TranslationPreferences {
    private const val FILE = "translation_preferences"
    fun source(context: Context) = prefs(context).getString("source", "vi") ?: "vi"
    fun target(context: Context) = prefs(context).getString("target", "en") ?: "en"
    fun autoDownload(context: Context) = prefs(context).getBoolean("auto_download", true)
    fun save(context: Context, source: String, target: String, auto: Boolean) {
        prefs(context).edit().putString("source", source).putString("target", target).putBoolean("auto_download", auto).apply()
    }
    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
