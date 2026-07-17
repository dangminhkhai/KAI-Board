package vn.kai.board.ai

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AiDiscovery(val provider: String, val models: List<String>, val freeCount: Int)

object AiProviderClient {
    val providerChoices = listOf("Tự động", "OpenRouter", "Gemini", "OpenAI", "Groq", "NVIDIA NIM")

    fun discover(apiKey: String, providerHint: String = "Tự động"): AiDiscovery {
        val key = apiKey.trim()
        if (providerHint != "Tự động") return discoverForProvider(providerHint, key)
        return when {
            key.startsWith("sk-or-") -> discoverOpenRouter(key)
            key.startsWith("AIza") -> discoverGemini(key)
            key.startsWith("gsk_") -> discoverOpenAiCompatible("Groq", "https://api.groq.com/openai/v1/models", key)
            key.startsWith("nvapi-") -> discoverOpenAiCompatible("NVIDIA NIM", "https://integrate.api.nvidia.com/v1/models", key)
            key.startsWith("sk-") -> discoverOpenAi(key)
            else -> throw IllegalArgumentException("Không nhận diện được key; hãy chọn nhà cung cấp thủ công")
        }
    }

    private fun discoverForProvider(provider: String, key: String) = when (provider) {
        "OpenRouter" -> discoverOpenRouter(key)
        "Gemini" -> discoverGemini(key)
        "OpenAI" -> discoverOpenAi(key)
        "Groq" -> discoverOpenAiCompatible("Groq", "https://api.groq.com/openai/v1/models", key)
        "NVIDIA NIM" -> discoverOpenAiCompatible("NVIDIA NIM", "https://integrate.api.nvidia.com/v1/models", key)
        else -> throw IllegalArgumentException("Nhà cung cấp chưa được hỗ trợ")
    }

    private fun discoverOpenRouter(key: String): AiDiscovery {
        val json = request("https://openrouter.ai/api/v1/models", key)
        val models = json.getJSONArray("data")
        val free = mutableListOf<String>()
        val paid = mutableListOf<String>()
        repeat(models.length()) { index ->
            val model = models.getJSONObject(index)
            val id = model.optString("id")
            val pricing = model.optJSONObject("pricing")
            val isFree = id.endsWith(":free") || (pricing?.optString("prompt")?.toDoubleOrNull() == 0.0 && pricing.optString("completion").toDoubleOrNull() == 0.0)
            if (id.isNotBlank()) (if (isFree) free else paid).add(id)
        }
        return AiDiscovery("OpenRouter", free + paid, free.size)
    }

    private fun discoverOpenAi(key: String): AiDiscovery {
        val json = request("https://api.openai.com/v1/models", key)
        val ids = json.getJSONArray("data").let { array ->
            buildList { repeat(array.length()) { add(array.getJSONObject(it).optString("id")) } }
        }.filter { it.startsWith("gpt-") || it.startsWith("o") }.sorted()
        return AiDiscovery("OpenAI", ids, 0)
    }

    private fun discoverOpenAiCompatible(provider: String, url: String, key: String): AiDiscovery {
        val json = request(url, key)
        val ids = json.getJSONArray("data").let { array ->
            buildList { repeat(array.length()) {
                val id = array.getJSONObject(it).optString("id")
                if (id.isNotBlank()) add(id)
            } }
        }
        return AiDiscovery(provider, ids, 0)
    }

    private fun discoverGemini(key: String): AiDiscovery {
        val json = request("https://generativelanguage.googleapis.com/v1beta/models?key=$key", null)
        val ids = json.getJSONArray("models").let { array ->
            buildList { repeat(array.length()) {
                val model = array.getJSONObject(it)
                val methods = model.optJSONArray("supportedGenerationMethods")?.toString().orEmpty()
                if (methods.contains("generateContent")) add(model.optString("name").removePrefix("models/"))
            } }
        }
        return AiDiscovery("Gemini", ids, ids.size)
    }

    private fun request(url: String, bearer: String?): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 15_000
        connection.requestMethod = "GET"
        if (bearer != null) connection.setRequestProperty("Authorization", "Bearer $bearer")
        connection.setRequestProperty("Accept", "application/json")
        val status = connection.responseCode
        val body = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) throw IllegalStateException("API từ chối key (HTTP $status)")
        return JSONObject(body)
    }

    fun generate(provider: String, apiKey: String, models: List<String>, tone: AiTone, prompt: String): Pair<String, String> {
        if (prompt.isBlank()) throw IllegalArgumentException("Yêu cầu đang trống")
        if (models.isEmpty()) throw IllegalStateException("Chưa quét được model")
        var lastError: Throwable? = null
        models.take(20).forEach { model ->
            try {
                return model to when (provider) {
                    "Gemini" -> generateGemini(apiKey, model, tone, prompt)
                    "OpenRouter" -> generateChat("https://openrouter.ai/api/v1/chat/completions", apiKey, model, tone, prompt)
                    "OpenAI" -> generateChat("https://api.openai.com/v1/chat/completions", apiKey, model, tone, prompt)
                    "Groq" -> generateChat("https://api.groq.com/openai/v1/chat/completions", apiKey, model, tone, prompt)
                    "NVIDIA NIM" -> generateChat("https://integrate.api.nvidia.com/v1/chat/completions", apiKey, model, tone, prompt)
                    else -> throw IllegalStateException("Nhà cung cấp chưa được hỗ trợ")
                }
            } catch (error: RetryableAiException) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("Không model nào xử lý được yêu cầu")
    }

    private fun generateChat(url: String, key: String, model: String, tone: AiTone, prompt: String): String {
        val payload = JSONObject().put("model", model).put("messages", org.json.JSONArray()
            .put(JSONObject().put("role", "system").put("content", "Bạn là trợ lý viết tiếng Việt. ${tone.instruction} Chỉ trả về nội dung hoàn chỉnh, không giải thích."))
            .put(JSONObject().put("role", "user").put("content", prompt)))
        val json = post(url, key, payload)
        return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
    }

    private fun generateGemini(key: String, model: String, tone: AiTone, prompt: String): String {
        val text = "${tone.instruction}\nChỉ trả về nội dung hoàn chỉnh, không giải thích.\n\n$prompt"
        val payload = JSONObject().put("contents", org.json.JSONArray().put(JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", text)))))
        val json = post("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key", null, payload)
        return json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()
    }

    private fun post(url: String, bearer: String?, payload: JSONObject): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000; connection.readTimeout = 45_000
        connection.requestMethod = "POST"; connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        if (bearer != null) connection.setRequestProperty("Authorization", "Bearer $bearer")
        connection.outputStream.use { it.write(payload.toString().toByteArray()) }
        val status = connection.responseCode
        val body = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status in listOf(402, 429, 503)) throw RetryableAiException("Model $status")
        if (status !in 200..299) throw IllegalStateException("AI lỗi HTTP $status")
        return JSONObject(body)
    }

    private class RetryableAiException(message: String) : RuntimeException(message)
}
