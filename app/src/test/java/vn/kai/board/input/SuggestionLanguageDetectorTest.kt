package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionLanguageDetectorTest {
    @Test fun detectsVietnameseMarks() {
        assertEquals(SuggestionLanguage.VIETNAMESE, SuggestionLanguageDetector.detect("tiếng"))
    }

    @Test fun detectsLightweightEnglishHints() {
        assertEquals(SuggestionLanguage.ENGLISH, SuggestionLanguageDetector.detect("hello"))
        assertEquals(SuggestionLanguage.ENGLISH, SuggestionLanguageDetector.detect("typing"))
    }

    @Test fun leavesAmbiguousAsciiNeutral() {
        assertEquals(SuggestionLanguage.UNKNOWN, SuggestionLanguageDetector.detect("ban"))
    }
}
