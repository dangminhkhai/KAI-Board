package vn.kai.board.settings

import android.content.Context
import org.json.JSONObject
import vn.kai.board.input.UserLexiconStore
import vn.kai.board.input.PhraseLearningStore

object SettingsBackup {
    private const val VERSION = 1
    private val allowedFiles = listOf(
        "keyboard_preferences",
        "translation_preferences",
        "ai_preferences",
        "user_lexicon",
        "phrase_learning",
        "email_suggestions",
        "hashtag_suggestions",
    )

    fun export(context: Context): String {
        val root = JSONObject().put("version", VERSION)
        val settings = JSONObject()
        allowedFiles.forEach { file ->
            val values = JSONObject()
            context.getSharedPreferences(file, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                when (value) {
                    is Boolean, is Int, is Long, is Float, is String -> values.put(key, value)
                }
            }
            settings.put(file, values)
        }
        root.put("settings", settings)
        root.put("themeExtensions", ThemeExtensionStore.exportPackages(context))
        return root.toString(2)
    }

    fun import(context: Context, raw: String) {
        val root = JSONObject(raw)
        require(root.optInt("version") == VERSION) { "Phiên bản sao lưu không được hỗ trợ" }
        val settings = root.getJSONObject("settings")
        allowedFiles.forEach { file ->
            val values = settings.optJSONObject(file) ?: return@forEach
            val editor = context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear()
            values.keys().forEach { key ->
                when (val value = values.get(key)) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                }
            }
            editor.commit()
        }
        root.optJSONArray("themeExtensions")?.let { ThemeExtensionStore.importPackages(context, it) }
        UserLexiconStore.invalidateCache()
        PhraseLearningStore.invalidateCache()
    }

    fun containsSecrets(raw: String): Boolean = raw.contains("ai_secret") || raw.contains("api_key", ignoreCase = true)

    internal fun includedDataFiles(): List<String> = allowedFiles.toList()
    internal fun includesThemeExtensions() = true
}
