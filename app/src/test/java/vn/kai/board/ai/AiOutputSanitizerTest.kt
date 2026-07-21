package vn.kai.board.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiOutputSanitizerTest {
    @Test
    fun stripsAsciiQuotesAroundTitle() {
        assertEquals(
            "5 cách ngủ ngon hơn",
            AiOutputSanitizer.sanitize("\"5 cách ngủ ngon hơn\""),
        )
    }

    @Test
    fun stripsSmartQuotes() {
        assertEquals(
            "Tiêu đề hay",
            AiOutputSanitizer.sanitize("“Tiêu đề hay”"),
        )
    }

    @Test
    fun stripsQuotedTitleList() {
        val raw = "\"Tiêu đề 1\"\n\"Tiêu đề 2\"\n\"Tiêu đề 3\""
        assertEquals(
            "Tiêu đề 1\nTiêu đề 2\nTiêu đề 3",
            AiOutputSanitizer.sanitize(raw),
        )
    }

    @Test
    fun keepsInnerQuotesWhenNotFullyWrapped() {
        assertEquals(
            "Anh ấy nói \"xin chào\" với tôi",
            AiOutputSanitizer.sanitize("Anh ấy nói \"xin chào\" với tôi"),
        )
    }

    @Test
    fun stripsOuterQuotesLeavingInnerPhrase() {
        // Model returned: "Slogan: làm ngay"
        assertEquals(
            "Slogan: làm ngay",
            AiOutputSanitizer.sanitize("\"Slogan: làm ngay\""),
        )
    }

    @Test
    fun stripsCodeFenceWrapper() {
        assertEquals(
            "plain title",
            AiOutputSanitizer.sanitize("```\nplain title\n```"),
        )
    }

    @Test
    fun leavesUnquotedText() {
        assertEquals("Tiêu đề không quote", AiOutputSanitizer.sanitize("Tiêu đề không quote"))
    }
}
