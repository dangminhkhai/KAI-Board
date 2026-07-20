package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseLearningStoreTest {
    @Test
    fun trigramBeatsMoreFrequentBigramForMatchingContext() {
        val entries = listOf(
            listOf("xin", "chào") to 50,
            listOf("bạn", "khỏe") to 100,
            listOf("xin", "chào", "bạn") to 1,
            listOf("chào", "bạn", "nhé") to 1,
        )

        assertEquals("bạn", PhraseLearningStore.rankCandidates(entries, listOf("xin", "chào")).first())
        assertEquals("nhé", PhraseLearningStore.rankCandidates(entries, listOf("chào", "bạn")).first())
    }

    @Test
    fun fallsBackToBigramWhenTrigramIsUnknown() {
        val entries = listOf(listOf("cảm", "ơn") to 2)
        assertEquals("ơn", PhraseLearningStore.rankCandidates(entries, listOf("rất", "cảm")).first())
    }

    @Test
    fun emptyPersonalDictionaryHasNoCannedContinuation() {
        assertEquals(emptyList<String>(), PhraseLearningStore.rankCandidates(emptyList(), listOf("xin")))
    }

    @Test
    fun seedContinuesCommonLeftWords() {
        val afterXin = PhraseLearningStore.suggestSeed(listOf("xin"), 3)
        assertTrue(afterXin.contains("chào"))
        assertEquals(listOf("ơn", "thấy"), PhraseLearningStore.suggestSeed(listOf("cảm"), 2))
    }

    @Test
    fun decayReducesOlderScores() {
        val today = 10_000L
        val fresh = PhraseLearningStore.decayedScoreMilli(10_000, today, today)
        val old = PhraseLearningStore.decayedScoreMilli(10_000, today - 21, today)
        assertEquals(10_000, fresh)
        assertTrue(old < fresh)
        assertTrue(old in 4_000..6_000) // ~half after one half-life
    }

    @Test
    fun seedCatalogIsSmallAndStable() {
        val n = PhraseSeedCatalog.size()
        assertTrue("expected ~80–150 pairs, was $n", n in 80..150)
        assertTrue(PhraseSeedCatalog.continuations("xin").contains("chào"))
    }

    @Test
    fun removeInvolvingDropsPairsWithThatWord() {
        // Pure ranking unit: simulate filter used by removeInvolving
        val entries = listOf(
            listOf("xin", "chào") to 3,
            listOf("cảm", "ơn") to 2,
            listOf("xin", "chào", "bạn") to 1,
        )
        val remaining = entries.filterNot { (words, _) -> words.any { it == "chào" } }
        assertEquals(listOf(listOf("cảm", "ơn") to 2), remaining)
    }
}
