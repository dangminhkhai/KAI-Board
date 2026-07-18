package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HashtagSuggestionEngineTest {
    @Test fun extractsCurrentHashtag() {
        assertEquals("#KAI_Board", HashtagSuggestionEngine.currentToken("dùng #KAI_Board"))
        assertEquals("", HashtagSuggestionEngine.currentToken("không có hashtag"))
    }

    @Test fun suggestsLearnedHashtagsInStoredFrequencyOrder() {
        val learned = listOf("#KAIBoard", "#KAI2026", "#Android")
        assertEquals(listOf("#KAIBoard", "#KAI2026"), HashtagSuggestionEngine.suggest("#kai", learned))
    }

    @Test fun validatesCompleteHashtags() {
        assertTrue(HashtagSuggestionEngine.isComplete("#KAI_Board2026"))
        assertFalse(HashtagSuggestionEngine.isComplete("#a"))
        assertFalse(HashtagSuggestionEngine.isComplete("#KAI-Board"))
    }
}
