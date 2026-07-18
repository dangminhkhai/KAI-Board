package vn.kai.board.input

import org.junit.Assert.assertEquals
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
    fun builtInPhraseContinuesWithoutPriorLearning() {
        assertEquals("chào", PhraseLearningStore.rankCandidates(emptyList(), listOf("xin")).first())
        assertEquals("bạn", PhraseLearningStore.rankCandidates(emptyList(), listOf("xin", "chào")).first())
        assertEquals("nhé", PhraseLearningStore.rankCandidates(emptyList(), listOf("chào", "bạn")).first())
    }
}
