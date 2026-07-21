package vn.kai.board.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCommandSuggestionEngineTest {
    @Test
    fun hasNoBuiltInSuggestions() {
        assertTrue(AiCommandSuggestionEngine.suggest("Viế").isEmpty())
    }

    @Test
    fun learnedCommandCompletesPartialAndContext() {
        val learned = AiCommandSuggestionEngine.learnedTransitions("Viết tiêu đề video ngắn")
        assertTrue("Viết" in AiCommandSuggestionEngine.suggest("Viế", learned = learned))
        val suggestions = AiCommandSuggestionEngine.suggest("Viết", learned = learned)
        assertTrue(suggestions.toString(), suggestions.any { it.equals("tiêu", ignoreCase = true) })
        // Never dump multi-word follow-ups like "tiêu đề".
        assertTrue(suggestions.toString(), suggestions.none { ' ' in it.trim() })
    }

    @Test
    fun nextWordOnlyAfterTiêuNotDeNgan() {
        // User: type "Tiêu" → only "đề"; pick "đề" → then "ngắn" (not "đề ngắn" at once).
        val learned = AiCommandSuggestionEngine.learnedTransitions("Viết tiêu đề ngắn")
        val afterTieu = AiCommandSuggestionEngine.suggest("Tiêu", learned = learned)
        assertTrue(afterTieu.toString(), afterTieu.any { it.equals("đề", ignoreCase = true) })
        // Next-word must stay lowercase (not "Đề" from matchCase on "Tiêu").
        assertTrue(afterTieu.toString(), afterTieu.any { it == "đề" })
        assertFalse(afterTieu.toString(), afterTieu.any { it.equals("Tiêu", ignoreCase = true) })
        assertFalse(afterTieu.toString(), afterTieu.any { it.contains(' ') })
        assertFalse(afterTieu.toString(), afterTieu.any { normalizeLoose(it).contains("de ngan") })

        val afterDe = AiCommandSuggestionEngine.suggest("Tiêu đề ", learned = learned)
        assertTrue(afterDe.toString(), afterDe.any { it.equals("ngắn", ignoreCase = true) })
        assertFalse(afterDe.toString(), afterDe.any { it.contains(' ') })
    }

    @Test
    fun contextualNextWordDoesNotMixGlobalStarters() {
        // "Limo" is a frequent starter elsewhere; after "Tiêu" it must not appear unless learned there.
        val learned = AiCommandSuggestionEngine.learnedTransitions("Viết tiêu đề ngắn") +
            AiCommandSuggestionEngine.learnedTransitions("Limo green video")
                .mapValues { it.value * 50 }
        val afterTieu = AiCommandSuggestionEngine.suggest("Tiêu", learned = learned)
        assertTrue(afterTieu.toString(), afterTieu.any { it.equals("đề", ignoreCase = true) })
        assertFalse(afterTieu.toString(), afterTieu.any { it.equals("Limo", ignoreCase = true) })
        assertFalse(afterTieu.toString(), afterTieu.any { it.equals("green", ignoreCase = true) })
    }

    @Test
    fun ignoresLegacyMultiWordCandidates() {
        val single = AiCommandSuggestionEngine.learnedTransitions("tiêu đề ngắn")
        val legacy = single + mapOf(
            AiCommandSuggestionEngine.encode(listOf("tieu"), "đề ngắn") to 999,
        )
        val suggestions = AiCommandSuggestionEngine.suggest("tiêu ", learned = legacy)
        assertTrue(suggestions.toString(), suggestions.any { it.equals("đề", ignoreCase = true) })
        assertFalse(suggestions.toString(), suggestions.any { ' ' in it })
    }

    @Test
    fun selectionReplacesPartialAndAddsSpace() {
        val edit = AiCommandSuggestionEngine.applySuggestion("Viế", 4, "Viết")
        assertEquals("Viết ", edit.prompt)
        assertEquals(5, edit.cursor)
    }

    @Test
    fun phraseSelectionContinuesPrompt() {
        // apply still accepts multi-word if UI ever passes one; suggest no longer offers them.
        val edit = AiCommandSuggestionEngine.applySuggestion("Viết ", 5, "tiêu đề")
        assertEquals("Viết tiêu đề ", edit.prompt)
        assertEquals(edit.prompt.length, edit.cursor)
    }

    @Test
    fun learnedCommandUsesRealUserContext() {
        val learned = AiCommandSuggestionEngine.learnedTransitions("Viết tiêu đề VinFast VF9")
            .mapValues { it.value * 10 }
        val suggestions = AiCommandSuggestionEngine.suggest("Viết tiêu đề ", learned = learned)
        assertTrue(suggestions.toString(), suggestions.first().startsWith("VinFast", ignoreCase = true))
        assertFalse(suggestions.toString(), suggestions.any { ' ' in it.trim() })
    }

    private fun normalizeLoose(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd').replace('Đ', 'D')
            .lowercase()
}
