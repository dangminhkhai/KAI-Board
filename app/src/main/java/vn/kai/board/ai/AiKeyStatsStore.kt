package vn.kai.board.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

data class AiKeyStats(
    val provider: String,
    val modelCount: Int,
    val accessSummary: String,
    val models: List<String> = emptyList(),
    val lastFailure: String = "",
)

object AiKeyStatsStore {
    private const val FILE = "ai_key_stats"
    private const val STATS = "stats"

    fun get(context: Context, apiKey: String): AiKeyStats? = runCatching {
        val root = JSONObject(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(STATS, "{}").orEmpty())
        val item = root.optJSONObject(id(apiKey)) ?: return null
        val models = item.optJSONArray("models") ?: JSONArray()
        AiKeyStats(
            provider = item.getString("provider"),
            modelCount = item.getInt("modelCount"),
            accessSummary = item.optString("accessSummary"),
            models = List(models.length()) { models.getString(it) },
            lastFailure = item.optString("lastFailure"),
        )
    }.getOrNull()

    fun save(context: Context, apiKey: String, discovery: AiDiscovery) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val root = runCatching { JSONObject(prefs.getString(STATS, "{}").orEmpty()) }.getOrElse { JSONObject() }
        root.put(id(apiKey), JSONObject()
            .put("provider", discovery.provider)
            .put("modelCount", discovery.models.size)
            .put("accessSummary", discovery.accessSummary())
            .put("models", JSONArray(discovery.models))
            .put("lastFailure", ""))
        prefs.edit().putString(STATS, root.toString()).apply()
    }

    fun markFailure(context: Context, apiKey: String, message: String) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val root = runCatching { JSONObject(prefs.getString(STATS, "{}").orEmpty()) }.getOrElse { JSONObject() }
        val item = root.optJSONObject(id(apiKey)) ?: JSONObject()
            .put("provider", AiProviderClient.detectProvider(apiKey).orEmpty())
            .put("modelCount", 0)
            .put("accessSummary", "")
            .put("models", JSONArray())
        item.put("lastFailure", message.take(160))
        root.put(id(apiKey), item)
        prefs.edit().putString(STATS, root.toString()).apply()
    }

    fun clearFailure(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val root = runCatching { JSONObject(prefs.getString(STATS, "{}").orEmpty()) }.getOrElse { JSONObject() }
        root.optJSONObject(id(apiKey))?.put("lastFailure", "") ?: return
        prefs.edit().putString(STATS, root.toString()).apply()
    }

    fun remove(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val root = runCatching { JSONObject(prefs.getString(STATS, "{}").orEmpty()) }.getOrElse { JSONObject() }
        root.remove(id(apiKey))
        prefs.edit().putString(STATS, root.toString()).apply()
    }

    private fun id(apiKey: String): String = MessageDigest.getInstance("SHA-256")
        .digest(apiKey.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
