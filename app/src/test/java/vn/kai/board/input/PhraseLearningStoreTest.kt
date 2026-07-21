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
    fun decayReducesOlderScores() {
        val today = 10_000L
        val fresh = PhraseLearningStore.decayedScoreMilli(10_000, today, today)
        val old = PhraseLearningStore.decayedScoreMilli(10_000, today - 21, today)
        assertEquals(10_000, fresh)
        assertTrue(old < fresh)
        assertTrue(old in 4_000..6_000) // ~half after one half-life
    }

    @Test
    fun removeInvolvingDropsPairsWithThatWord() {
        val entries = listOf(
            listOf("xin", "chào") to 3,
            listOf("cảm", "ơn") to 2,
            listOf("xin", "chào", "bạn") to 1,
        )
        val remaining = entries.filterNot { (words, _) -> words.any { it == "chào" } }
        assertEquals(listOf(listOf("cảm", "ơn") to 2), remaining)
    }

    @Test
    fun mergePersonalBeforePack() {
        val merged = SuggestionPriority.merge(
            emptyList(),
            listOf("personal"),
            listOf("pack"),
            limit = 3,
        )
        assertEquals(listOf("personal", "pack"), merged)
    }

    @Test
    fun removeExactDropsOnlyMatchingPhrase() {
        val entries = listOf(
            PhraseEntry(listOf("xin", "chào"), count = 2, recentScoreMilli = 2000, lastUsedEpochDay = 10),
            PhraseEntry(listOf("cảm", "ơn"), count = 1, recentScoreMilli = 1000, lastUsedEpochDay = 10),
            PhraseEntry(listOf("xin", "chào", "bạn"), count = 1, recentScoreMilli = 1000, lastUsedEpochDay = 10),
        )
        val key = listOf("xin", "chào")
        val remaining = entries.filterNot { it.words == key }
        assertEquals(2, remaining.size)
        assertTrue(remaining.none { it.words == key })
        assertTrue(remaining.any { it.words == listOf("xin", "chào", "bạn") })
    }

    @Test
    fun phraseListItemLabelJoinsWithArrow() {
        val item = PhraseListItem(
            words = listOf("xin", "chào"),
            count = 1,
            decayScoreMilli = 1000,
            lastUsedEpochDay = 1,
            daysSinceLastUse = 0,
        )
        assertEquals("xin → chào", item.label())
    }
}
