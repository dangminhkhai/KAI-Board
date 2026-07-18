package vn.kai.board.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiProviderClientTest {
    @Test
    fun detectsProviderWithoutSendingKeyToWrongEndpoint() {
        assertEquals("Groq", AiProviderClient.detectProvider("gsk_example"))
        assertEquals("NVIDIA NIM", AiProviderClient.detectProvider("nvapi-example"))
        assertEquals("OpenRouter", AiProviderClient.detectProvider("sk-or-example"))
        assertEquals("OpenAI", AiProviderClient.detectProvider("sk-example"))
        assertEquals("Gemini", AiProviderClient.detectProvider("AIza-example"))
        assertNull(AiProviderClient.detectProvider("unknown"))
    }
}
