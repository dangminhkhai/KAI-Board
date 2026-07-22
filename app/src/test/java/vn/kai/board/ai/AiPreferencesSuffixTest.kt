package vn.kai.board.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiPreferencesSuffixTest {
    @Test
    fun disabledLeavesOutputUnchanged() {
        assertEquals(
            "Tiêu đề hay",
            AiPreferences.appendSuffixIfEnabled("Tiêu đề hay", enabled = false, suffix = "\n#tag"),
        )
    }

    @Test
    fun enabledAppendsSuffixAsWritten() {
        assertEquals(
            "Tiêu đề hay\n#tag",
            AiPreferences.appendSuffixIfEnabled("Tiêu đề hay", enabled = true, suffix = "\n#tag"),
        )
    }

    @Test
    fun emptySuffixDoesNotChangeOutput() {
        assertEquals(
            "body",
            AiPreferences.appendSuffixIfEnabled("body", enabled = true, suffix = ""),
        )
    }

    @Test
    fun respectsMaxLength() {
        val long = "x".repeat(AiPreferences.SUFFIX_MAX_CHARS + 50)
        val out = AiPreferences.appendSuffixIfEnabled("a", enabled = true, suffix = long)
        assertEquals("a" + "x".repeat(AiPreferences.SUFFIX_MAX_CHARS), out)
    }
}
