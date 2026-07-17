package vn.kai.board.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiDiscoveryTest {
    @Test
    fun groqReportsFreeTierByQuotaInsteadOfZeroFreeModels() {
        val discovery = AiDiscovery(
            provider = "Groq",
            models = List(17) { "model-$it" },
            freeTierByQuota = true,
        )

        assertEquals("Free tier theo hạn mức", discovery.accessSummary())
    }

    @Test
    fun providerWithPerModelPricingReportsKnownFreeCount() {
        val discovery = AiDiscovery(
            provider = "OpenRouter",
            models = listOf("free", "paid"),
            freeCount = 1,
        )

        assertEquals("1 model miễn phí", discovery.accessSummary())
    }

    @Test
    fun providerWithoutPricingMetadataDoesNotClaimZeroFreeModels() {
        val discovery = AiDiscovery(provider = "OpenAI", models = listOf("model"))

        assertEquals("Chưa có thông tin giá", discovery.accessSummary())
    }
}
