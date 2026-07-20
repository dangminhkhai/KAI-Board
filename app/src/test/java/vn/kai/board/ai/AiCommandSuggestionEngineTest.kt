package vn.kai.board.ai

import org.junit.Assert.assertEquals
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
        assertTrue(suggestions.toString(), suggestions.any { it.startsWith("tiêu", ignoreCase = true) })
    }

    @Test
    fun selectionReplacesPartialAndAddsSpace() {
        val edit = AiCommandSuggestionEngine.applySuggestion("Viế", 4, "Viết")
        assertEquals("Viết ", edit.prompt)
        assertEquals(5, edit.cursor)
    }

    @Test
    fun phraseSelectionContinuesPrompt() {
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
    }
}
