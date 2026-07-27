package vn.kai.board.ai

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.CancellationException

class AiRequestCancellation {
    @Volatile private var cancelled = false
    @Volatile private var connection: HttpURLConnection? = null

    fun cancel() { cancelled = true; connection?.disconnect() }
    internal fun attach(value: HttpURLConnection) {
        if (cancelled) throw CancellationException("Đã hủy yêu cầu AI")
        connection = value
    }
    internal fun detach(value: HttpURLConnection) { if (connection === value) connection = null }
    internal fun check() { if (cancelled) throw CancellationException("Đã hủy yêu cầu AI") }
}

data class AiDiscovery(
    val provider: String,
    val models: List<String>,
    val freeCount: Int? = null,
    val freeTierByQuota: Boolean = false,
) {
    fun accessSummary(): String = when {
        freeCount != null -> "$freeCount model miễn phí"
        freeTierByQuota -> "Free tier theo hạn mức"
        else -> "Chưa có thông tin giá"
    }
}

object AiProviderClient {
    val providerChoices = listOf("Tự động", "OpenRouter", "Gemini", "OpenAI", "Groq", "NVIDIA NIM")

    fun detectProvider(apiKey: String): String? = when {
        apiKey.trim().startsWith("sk-or-") -> "OpenRouter"
        isGeminiKey(apiKey) -> "Gemini"
        apiKey.trim().startsWith("gsk_") -> "Groq"
        apiKey.trim().startsWith("nvapi-") -> "NVIDIA NIM"
        apiKey.trim().startsWith("sk-") -> "OpenAI"
        else -> null
    }

    private fun isGeminiKey(apiKey: String): Boolean {
        val key = apiKey.trim()
        return key.startsWith("AIza") || key.startsWith("AQ.")
    }

    fun discover(apiKey: String, providerHint: String = "Tự động"): AiDiscovery {
        val key = apiKey.trim()
        if (providerHint != "Tự động") return discoverForProvider(providerHint, key)
        return when {
            key.startsWith("sk-or-") -> discoverOpenRouter(key)
            isGeminiKey(key) -> discoverGemini(key)
            key.startsWith("gsk_") -> discoverGroq(key)
            key.startsWith("nvapi-") -> discoverNvidia(key)
            key.startsWith("sk-") -> discoverOpenAi(key)
            else -> throw IllegalArgumentException("Không nhận diện được key; hãy chọn nhà cung cấp thủ công")
        }
    }

    private fun discoverForProvider(provider: String, key: String) = when (provider) {
        "OpenRouter" -> discoverOpenRouter(key)
        "Gemini" -> discoverGemini(key)
        "OpenAI" -> discoverOpenAi(key)
        "Groq" -> discoverGroq(key)
        "NVIDIA NIM" -> discoverNvidia(key)
        else -> throw IllegalArgumentException("Nhà cung cấp chưa được hỗ trợ")
    }

    private fun discoverGroq(key: String): AiDiscovery {
        val discovered = discoverOpenAiCompatible(
            "Groq",
            "https://api.groq.com/openai/v1/models",
            key,
            freeTierByQuota = true,
        )
        return discovered.copy(models = prioritizeChatModels("Groq", discovered.models))
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
        return AiDiscovery("OpenAI", ids)
    }

    private fun discoverOpenAiCompatible(
        provider: String,
        url: String,
        key: String,
        freeTierByQuota: Boolean = false,
    ): AiDiscovery {
        val json = request(url, key)
        val ids = json.getJSONArray("data").let { array ->
            buildList { repeat(array.length()) {
                val id = array.getJSONObject(it).optString("id")
                if (id.isNotBlank()) add(id)
            } }
        }
        return AiDiscovery(provider, ids, freeTierByQuota = freeTierByQuota)
    }

    private fun discoverGemini(key: String): AiDiscovery {
        val json = request(
            "https://generativelanguage.googleapis.com/v1beta/models",
            bearer = null,
            googleApiKey = key,
        )
        val ids = json.getJSONArray("models").let { array ->
            buildList { repeat(array.length()) {
                val model = array.getJSONObject(it)
                val methods = model.optJSONArray("supportedGenerationMethods")?.toString().orEmpty()
                if (methods.contains("generateContent")) add(model.optString("name").removePrefix("models/"))
            } }
        }
        return AiDiscovery("Gemini", ids, ids.size)
    }

    private fun discoverNvidia(key: String): AiDiscovery = try {
        val discovered = discoverOpenAiCompatible("NVIDIA NIM", "https://integrate.api.nvidia.com/v1/models", key)
        // Prefer chat-capable IDs only. Do NOT prepend hard-coded models that may already be
        // Gone (HTTP 410) on integrate.api.nvidia.com — that made every request fail first.
        val chat = prioritizeChatModels("NVIDIA NIM", discovered.models)
        discovered.copy(models = chat.ifEmpty { NVIDIA_PREFERRED_CHAT })
    } catch (error: HttpStatusException) {
        if (error.status != 404) throw error
        // NVIDIA may omit an OpenAI-style model list. Probe preferred chat models until one works.
        var accepted: String? = null
        var last: Throwable? = null
        for (model in NVIDIA_PREFERRED_CHAT) {
            try {
                val payload = JSONObject()
                    .put("model", model)
                    .put("max_tokens", 1)
                    .put("messages", org.json.JSONArray().put(
                        JSONObject().put("role", "user").put("content", "Hi"),
                    ))
                post(
                    "https://integrate.api.nvidia.com/v1/chat/completions",
                    key,
                    payload,
                    AiRequestCancellation(),
                )
                accepted = model
                break
            } catch (probe: Throwable) {
                last = probe
            }
        }
        if (accepted == null) throw last ?: error
        AiDiscovery("NVIDIA NIM", listOf(accepted) + NVIDIA_PREFERRED_CHAT.filterNot { it == accepted }, freeTierByQuota = true)
    }

    private fun request(url: String, bearer: String?, googleApiKey: String? = null): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 15_000
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = false
        if (bearer != null) connection.setRequestProperty("Authorization", "Bearer $bearer")
        if (googleApiKey != null) connection.setRequestProperty("x-goog-api-key", googleApiKey)
        connection.setRequestProperty("Accept", "application/json")
        val status = connection.responseCode
        val body = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) throw HttpStatusException(status, "API từ chối key (HTTP $status)")
        return JSONObject(body)
    }

    fun generate(provider: String, apiKeys: List<String>, models: List<String>, tone: AiTone, prompt: String, cancellation: AiRequestCancellation = AiRequestCancellation()): Pair<String, String> {
        if (prompt.isBlank()) throw IllegalArgumentException("Yêu cầu đang trống")
        if (apiKeys.isEmpty()) throw IllegalStateException("Chưa có API key")
        val chatModels = prioritizeChatModels(provider, models)
        if (chatModels.isEmpty()) throw IllegalStateException("Chưa quét được model chat phù hợp")
        var lastError: Throwable? = null
        ApiKeyPool.normalize(apiKeys).forEach { apiKey ->
            for (model in chatModels.take(20)) {
                cancellation.check()
                try {
                    return model to when (provider) {
                        "Gemini" -> generateGemini(apiKey, model, tone, prompt, cancellation)
                        "OpenRouter" -> generateChat("https://openrouter.ai/api/v1/chat/completions", apiKey, model, tone, prompt, cancellation)
                        "OpenAI" -> generateChat("https://api.openai.com/v1/chat/completions", apiKey, model, tone, prompt, cancellation)
                        "Groq" -> generateChat("https://api.groq.com/openai/v1/chat/completions", apiKey, model, tone, prompt, cancellation)
                        "NVIDIA NIM" -> generateChat("https://integrate.api.nvidia.com/v1/chat/completions", apiKey, model, tone, prompt, cancellation)
                        else -> throw IllegalStateException("Nhà cung cấp chưa được hỗ trợ")
                    }
                } catch (error: QuotaAiException) {
                    lastError = error
                    break
                } catch (error: InvalidApiKeyException) {
                    throw error
                } catch (error: RetryableAiException) {
                    lastError = error
                }
            }
        }
        throw lastError ?: IllegalStateException("Không model hoặc API key nào xử lý được yêu cầu")
    }

    fun generate(provider: String, apiKey: String, models: List<String>, tone: AiTone, prompt: String) =
        generate(provider, listOf(apiKey), models, tone, prompt)

    fun generateWithFallback(
        candidates: List<AiProviderCandidate>,
        tone: AiTone,
        prompt: String,
        cancellation: AiRequestCancellation = AiRequestCancellation(),
        onFailure: (AiProviderFailure) -> Unit = {},
    ): AiGenerationResult {
        if (candidates.isEmpty()) throw IllegalStateException("Chưa có API key và model dùng được")
        var lastError: Throwable? = null
        candidates.forEach { candidate ->
            cancellation.check()
            try {
                val (model, output) = generate(
                    candidate.provider,
                    listOf(candidate.apiKey),
                    candidate.models,
                    tone,
                    prompt,
                    cancellation,
                )
                return AiGenerationResult(candidate.apiKey, candidate.provider, model, output)
            } catch (error: CancellationException) {
                throw error
            } catch (error: InvalidApiKeyException) {
                lastError = error
                onFailure(AiProviderFailure(candidate.apiKey, candidate.provider, error.message.orEmpty(), true))
            } catch (error: QuotaAiException) {
                lastError = error
                onFailure(AiProviderFailure(candidate.apiKey, candidate.provider, error.message.orEmpty(), false))
            } catch (error: RetryableAiException) {
                lastError = error
                onFailure(AiProviderFailure(candidate.apiKey, candidate.provider, error.message.orEmpty(), false))
            } catch (error: IllegalStateException) {
                // e.g. unexpected HTTP after all models for this key — still try next key
                lastError = error
                onFailure(AiProviderFailure(candidate.apiKey, candidate.provider, error.message.orEmpty(), false))
            }
        }
        throw lastError ?: IllegalStateException("Không provider nào xử lý được yêu cầu")
    }

    /** Shared system rules for all chat-style providers. */
    internal fun systemInstruction(tone: AiTone): String =
        "Bạn là trợ lý viết tiếng Việt. ${tone.instruction} " +
            "Chỉ trả về nội dung hoàn chỉnh, không giải thích, không mở đầu/kết thúc bằng lời dẫn. " +
            "Không bọc tiêu đề, slogan hay câu trả lời ngắn trong dấu ngoặc kép (\" \" “ ” ' '), " +
            "không bọc cả đoạn trong markdown code fence trừ khi người dùng yêu cầu code."

    private fun generateChat(url: String, key: String, model: String, tone: AiTone, prompt: String, cancellation: AiRequestCancellation): String {
        val payload = JSONObject().put("model", model).put("messages", org.json.JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemInstruction(tone)))
            .put(JSONObject().put("role", "user").put("content", prompt)))
        val json = post(url, key, payload, cancellation)
        val content = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        return AiOutputSanitizer.sanitize(content)
    }

    private fun generateGemini(key: String, model: String, tone: AiTone, prompt: String, cancellation: AiRequestCancellation): String {
        val text = "${systemInstruction(tone)}\n\n$prompt"
        val payload = JSONObject().put("contents", org.json.JSONArray().put(JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", text)))))
        val json = post(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
            bearer = null,
            payload = payload,
            cancellation = cancellation,
            googleApiKey = key,
        )
        val content = json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        return AiOutputSanitizer.sanitize(content)
    }

    private fun post(
        url: String,
        bearer: String?,
        payload: JSONObject,
        cancellation: AiRequestCancellation,
        googleApiKey: String? = null,
    ): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        cancellation.attach(connection)
        connection.connectTimeout = 12_000; connection.readTimeout = 30_000
        connection.requestMethod = "POST"; connection.doOutput = true
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Content-Type", "application/json")
        if (bearer != null) connection.setRequestProperty("Authorization", "Bearer $bearer")
        if (googleApiKey != null) connection.setRequestProperty("x-goog-api-key", googleApiKey)
        val status: Int
        val body: String
        try {
            cancellation.check()
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            status = connection.responseCode
            body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText().take(MAX_RESPONSE_CHARS) }.orEmpty()
        } catch (_: SocketTimeoutException) {
            throw RetryableAiException("AI phản hồi quá lâu, đã thử provider tiếp theo")
        } finally {
            cancellation.detach(connection)
            connection.disconnect()
        }
        if (status in 300..399) throw IllegalStateException("AI từ chối chuyển hướng không an toàn")
        if (status in listOf(401, 403)) throw InvalidApiKeyException("API key không hợp lệ hoặc không có quyền")
        if (status in listOf(402, 429)) throw QuotaAiException("Hạn mức API đã hết hoặc bị giới hạn (HTTP $status)")
        // 400/404/410/413/422: often wrong model (Gone, non-chat, tiny context) — try next model/key
        if (status in listOf(400, 404, 410, 413, 422)) {
            throw RetryableAiException("Model/endpoint không dùng được (HTTP $status)")
        }
        if (status in 500..599) throw RetryableAiException("Provider tạm thời không sẵn sàng (HTTP $status)")
        if (status !in 200..299) throw RetryableAiException("AI lỗi HTTP $status")
        return JSONObject(body)
    }

    /**
     * Keep chat/completion models only and put known-good IDs first.
     * Groq /models lists whisper/tts/guard; NVIDIA lists embed/rerank and retired IDs.
     */
    internal fun prioritizeChatModels(provider: String, models: List<String>): List<String> {
        val filtered = models.map { it.trim() }.filter { it.isNotEmpty() && isLikelyChatModel(provider, it) }
        val preferred = when (provider) {
            "Groq" -> GROQ_PREFERRED_CHAT
            "NVIDIA NIM" -> NVIDIA_PREFERRED_CHAT
            else -> emptyList()
        }
        val head = preferred.filter { want -> filtered.any { it.equals(want, ignoreCase = true) } }
            .map { want -> filtered.first { it.equals(want, ignoreCase = true) } }
        return (head + filtered).distinct()
    }

    internal fun isLikelyChatModel(provider: String, modelId: String): Boolean {
        val m = modelId.lowercase()
        val excluded = listOf(
            "whisper", "tts", "guard", "embed", "rerank", "retrieval", "clip",
            "transcri", "speech", "audio", "moderation", "playai", "distance",
            "nv-embed", "nv-rerank", "ocr", "detect",
        )
        if (excluded.any { m.contains(it) }) return false
        return when (provider) {
            "Groq" -> m.contains("llama") || m.contains("gemma") || m.contains("mixtral") ||
                m.contains("qwen") || m.contains("deepseek") || m.contains("gpt-oss") ||
                m.contains("compound") || m.contains("moonshot") || m.contains("kimi") ||
                m.startsWith("openai/")
            "NVIDIA NIM" -> m.contains("instruct") || m.contains("chat") || m.contains("llama") ||
                m.contains("mistral") || m.contains("gemma") || m.contains("nemotron") ||
                m.contains("qwen") || m.contains("deepseek") || m.contains("phi-") ||
                m.contains("kimi") || m.contains("claude") || m.contains("gpt")
            else -> true
        }
    }

    private class InvalidApiKeyException(message: String) : RuntimeException(message)
    private class RetryableAiException(message: String) : RuntimeException(message)
    private class QuotaAiException(message: String) : RuntimeException(message)
    private class HttpStatusException(val status: Int, message: String) : RuntimeException(message)
    private const val MAX_RESPONSE_CHARS = 1_000_000
    /** Soft preference order — only used when still present in the live catalog. */
    private val GROQ_PREFERRED_CHAT = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "openai/gpt-oss-120b",
        "openai/gpt-oss-20b",
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "gemma2-9b-it",
    )
    private val NVIDIA_PREFERRED_CHAT = listOf(
        "meta/llama-3.1-8b-instruct",
        "meta/llama-3.3-70b-instruct",
        "google/gemma-2-9b-it",
        "mistralai/mistral-7b-instruct-v0.3",
        "nvidia/llama-3.1-nemotron-70b-instruct",
    )
}

data class AiProviderCandidate(
    val apiKey: String,
    val provider: String,
    val models: List<String>,
)

data class AiGenerationResult(
    val apiKey: String,
    val provider: String,
    val model: String,
    val output: String,
)

data class AiProviderFailure(
    val apiKey: String,
    val provider: String,
    val message: String,
    val invalidKey: Boolean,
)
