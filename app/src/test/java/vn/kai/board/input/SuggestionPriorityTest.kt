package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionPriorityTest {
    @Test fun aiThenPersonalThenOffline() {
        assertEquals(
            listOf("tiêu đề", "VinFast", "video"),
            SuggestionPriority.merge(
                ai = listOf("tiêu đề"),
                personal = listOf("VinFast", "tiêu đề"),
                offline = listOf("video", "nội dung"),
            ),
        )
    }

    @Test fun phrasePrefixHitsBeforeWordCompletions() {
        assertEquals(
            listOf("chào", "cháo", "chạm"),
            SuggestionPriority.mergePhraseAndCompletions(
                phrasePrefixHits = listOf("chào"),
                wordCompletions = listOf("cháo", "chào", "chạm"),
            ),
        )
    }
}
