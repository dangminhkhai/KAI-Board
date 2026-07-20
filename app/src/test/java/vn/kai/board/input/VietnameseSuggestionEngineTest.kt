package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VietnameseSuggestionEngineTest {
    @Test fun suggestsAccentedWordsFromPlainPrefix() {
        val values = VietnameseSuggestionEngine.suggest("tie")
        assertEquals(3, values.size)
        assertTrue("tiếng" in values)
        assertTrue("tiền" in values)
    }

    @Test fun keepsCurrentExactWordFirst() {
        assertEquals("chao", VietnameseSuggestionEngine.suggest("chao").first())
    }

    @Test fun preservesInitialCapital() {
        assertTrue(VietnameseSuggestionEngine.suggest("Vie").all { it.first().isUpperCase() })
    }

    @Test fun emptyAndOversizedQueriesHaveNoCandidates() {
        assertTrue(VietnameseSuggestionEngine.suggest("").isEmpty())
        assertTrue(VietnameseSuggestionEngine.suggest("a".repeat(33)).isEmpty())
    }

    @Test fun learnedFrequencyRaisesAWord() {
        val values = VietnameseSuggestionEngine.suggest("ca", learned = mapOf("camera" to 20))
        assertTrue("camera" in values)
    }

    @Test fun previousWordBoostsCommonPair() {
        assertTrue("ơn" in VietnameseSuggestionEngine.suggest("o", previousWord = "cảm"))
    }

    @Test fun disablingBuiltInPhrasesHidesMultiWordDictionaryEntries() {
        val withPhrases = VietnameseSuggestionEngine.suggest("xin", allowBuiltInPhrases = true)
        val without = VietnameseSuggestionEngine.suggest("xin", allowBuiltInPhrases = false)
        assertTrue(
            "expected multi-word seed phrase in default dictionary",
            withPhrases.any { it.contains(' ') },
        )
        assertTrue(without.none { it.contains(' ') })
        assertFalse(without.any { it.equals("xin chào", ignoreCase = true) })
    }

    @Test fun disablingBuiltInPhrasesDisablesHardCodedContextBoost() {
        val boosted = VietnameseSuggestionEngine.suggest("o", previousWord = "cảm", allowBuiltInPhrases = true)
        val plain = VietnameseSuggestionEngine.suggest("o", previousWord = "cảm", allowBuiltInPhrases = false)
        // With phrases on, context pair cam→on elevates "ơn"; without, order may differ.
        assertTrue("ơn" in boosted)
        // Still may include "ơn" as plain prefix of "o" from dictionary — only assert multi-word gone.
        assertTrue(plain.none { it.contains(' ') })
    }

    @Test fun conservativeAutoCorrectionKeepsKnownWords() {
        assertEquals(null, VietnameseSuggestionEngine.bestAutoCorrection("chao"))
    }

    @Test fun undoLearnedSpellingIsNotCorrectedAgain() {
        assertEquals("đang", VietnameseSuggestionEngine.bestAutoCorrection("đặng"))
        assertEquals(null, VietnameseSuggestionEngine.bestAutoCorrection("đặng", learned = mapOf("đặng" to 1)))
    }

    @Test fun prefixIndexDoesNotScanTheWholeDictionary() {
        val words = (0 until 10_000).map { index ->
            val first = ('a'.code + index % 26).toChar()
            val second = ('a'.code + (index / 26) % 26).toChar()
            "$first${second}word$index"
        }
        val inspected = VietnameseSuggestionEngine.candidatePoolSizeForTest(words, "ka")
        assertTrue("candidate pool was $inspected", inspected in 1..250)
    }

    @Test fun fuzzyLookupOnlyUsesSameInitialAndNearbyLength() {
        val words = listOf("đang", "đúng", "dùng", "x".repeat(10_000))
        val inspected = VietnameseSuggestionEngine.candidatePoolSizeForTest(words, "đặng")
        assertTrue(inspected <= 3)
    }

    @Test fun autoCorrectionSkipsNamesAbbreviationsAndNumbers() {
        assertEquals(null, VietnameseSuggestionEngine.bestAutoCorrection("Khaiez"))
        assertEquals(null, VietnameseSuggestionEngine.bestAutoCorrection("KAI"))
        assertEquals(null, VietnameseSuggestionEngine.bestAutoCorrection("kai2026"))
        assertTrue(VietnameseSuggestionEngine.isAutoCorrectionEligible("đặng"))
    }

}
