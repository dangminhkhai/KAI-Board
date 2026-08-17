package vn.kai.board.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderClientTest {
    @Test
    fun detectsProviderWithoutSendingKeyToWrongEndpoint() {
        assertEquals("TokenRouter", AiProviderClient.detectProvider("tr_example"))
        assertEquals("Groq", AiProviderClient.detectProvider("gsk_example"))
        assertEquals("NVIDIA NIM", AiProviderClient.detectProvider("nvapi-example"))
        assertEquals("OpenRouter", AiProviderClient.detectProvider("sk-or-example"))
        assertEquals("OpenAI", AiProviderClient.detectProvider("sk-example"))
        assertEquals("Gemini", AiProviderClient.detectProvider("AIza-example"))
        assertEquals("Gemini", AiProviderClient.detectProvider("AQ.example"))
        assertEquals("Gemini", AiProviderClient.detectProvider("  AQ.example  "))
        assertNull(AiProviderClient.detectProvider("AQ-example"))
        assertNull(AiProviderClient.detectProvider("unknown"))
    }

    @Test
    fun tokenRouterUsesEndpointMatchingKeyFamily() {
        assertEquals("https://api.tokenrouter.io/v1", AiProviderClient.tokenRouterBaseUrl("tr_example"))
        assertEquals("https://api.tokenrouter.com/v1", AiProviderClient.tokenRouterBaseUrl("sk-example"))
    }

    @Test
    fun groqFiltersNonChatModels() {
        val raw = listOf(
            "whisper-large-v3",
            "llama-3.1-8b-instant",
            "llama-guard-3-8b",
            "playai-tts",
            "llama-3.3-70b-versatile",
        )
        val ordered = AiProviderClient.prioritizeChatModels("Groq", raw)
        assertEquals(listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"), ordered)
        assertFalse(ordered.any { it.contains("whisper") || it.contains("guard") || it.contains("tts") })
    }

    @Test
    fun nvidiaFiltersEmbedAndKeepsInstruct() {
        val raw = listOf(
            "nvidia/nv-embedqa-e5-v5",
            "meta/llama-3.1-8b-instruct",
            "nvidia/nv-rerankqa-mistral-4b-v3",
            "google/gemma-2-9b-it",
        )
        val ordered = AiProviderClient.prioritizeChatModels("NVIDIA NIM", raw)
        assertTrue(ordered.first().contains("llama-3.1-8b-instruct"))
        assertTrue("google/gemma-2-9b-it" in ordered)
        assertFalse(ordered.any { it.contains("embed") || it.contains("rerank") })
    }

    @Test
    fun geminiPrefersStableFlashTextModels() {
        val raw = listOf(
            "gemini-3.1-pro-preview",
            "gemini-3.1-flash-image",
            "gemini-2.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-live-preview",
        )
        val ordered = AiProviderClient.prioritizeChatModels("Gemini", raw)
        assertEquals(
            listOf("gemini-3.5-flash-lite", "gemini-2.5-flash", "gemini-3.1-pro-preview"),
            ordered,
        )
        assertFalse(ordered.any { it.contains("image") || it.contains("live") })
    }

    @Test
    fun isLikelyChatModelRejectsAudioAndGuard() {
        assertFalse(AiProviderClient.isLikelyChatModel("Groq", "whisper-large-v3-turbo"))
        assertFalse(AiProviderClient.isLikelyChatModel("Groq", "meta-llama/llama-guard-4-12b"))
        assertTrue(AiProviderClient.isLikelyChatModel("Groq", "llama-3.1-8b-instant"))
        assertFalse(AiProviderClient.isLikelyChatModel("NVIDIA NIM", "nvidia/nv-embedqa-e5-v5"))
        assertTrue(AiProviderClient.isLikelyChatModel("NVIDIA NIM", "meta/llama-3.3-70b-instruct"))
    }
}
