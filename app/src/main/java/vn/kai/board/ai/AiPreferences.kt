package vn.kai.board.ai

import android.content.Context
import org.json.JSONArray

enum class AiTone(val label: String, val instruction: String) {
    BOLD("Hầm hố giật gân", "Viết mạnh mẽ, dồn dập, gây ấn tượng nhưng không bịa đặt."),
    FUNNY("Hài hước", "Viết hài hước, tự nhiên và duyên dáng."),
    MYSTERIOUS("Tò mò bí ẩn", "Viết gợi tò mò, bí ẩn và cuốn hút."),
    NEWS("Thời sự tin tức", "Viết khách quan, rõ ràng theo phong cách tin tức."),
    MOTIVATIONAL("Truyền động lực", "Viết tích cực, truyền cảm hứng và thúc đẩy hành động."),
    MINIMAL("Tối giản tinh tế", "Viết ngắn gọn, tinh tế, bỏ chi tiết thừa."),
    RANDOM("Tự động ngẫu nhiên", "Tự chọn giọng văn phù hợp nhất với nội dung."),
}

object AiPreferences {
    private const val FILE = "ai_preferences"
    private const val PROVIDER = "provider"
    private const val PROVIDER_HINT = "provider_hint"
    private const val MODELS = "models"
    private const val TONE = "tone"
    private const val DELAY_SECONDS = "delay_seconds"
    private const val AUTO_SEND = "auto_send"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    fun provider(context: Context) = prefs(context).getString(PROVIDER, "").orEmpty()
    fun providerHint(context: Context) = prefs(context).getString(PROVIDER_HINT, "Tự động").orEmpty()
    fun models(context: Context): List<String> = runCatching {
        val array = JSONArray(prefs(context).getString(MODELS, "[]"))
        List(array.length()) { array.getString(it) }
    }.getOrDefault(emptyList())
    fun tone(context: Context) = AiTone.entries.firstOrNull { it.name == prefs(context).getString(TONE, null) } ?: AiTone.RANDOM
    fun delaySeconds(context: Context) = prefs(context).getInt(DELAY_SECONDS, 2).coerceIn(1, 10)
    fun autoSend(context: Context) = prefs(context).getBoolean(AUTO_SEND, true)
    fun saveDiscovery(context: Context, provider: String, models: List<String>) = prefs(context).edit()
        .putString(PROVIDER, provider)
        .putString(MODELS, JSONArray(models).toString())
        .apply()
    fun setTone(context: Context, tone: AiTone) = prefs(context).edit().putString(TONE, tone.name).apply()
    fun setProviderHint(context: Context, provider: String) = prefs(context).edit().putString(PROVIDER_HINT, provider).apply()
    fun setDelay(context: Context, seconds: Int) = prefs(context).edit().putInt(DELAY_SECONDS, seconds.coerceIn(1, 10)).apply()
    fun setAutoSend(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(AUTO_SEND, enabled).apply()
}
