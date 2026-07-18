package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserLexiconStoreTest {
    @Test fun recentScoreHalvesAfterFourteenDays() {
        assertEquals(5_000, UserLexiconStore.decayedScoreMilli(10_000, 100, 114))
    }

    @Test fun recentUseCanOutrankOldFrequentWord() {
        val oldFrequent = UserLexiconStore.decayedScoreMilli(100_000, 100, 170)
        val recent = UserLexiconStore.decayedScoreMilli(10_000, 170, 170)
        assertTrue(recent > oldFrequent)
    }

    @Test fun clockMovingBackwardDoesNotIncreaseScore() {
        assertEquals(10_000, UserLexiconStore.decayedScoreMilli(10_000, 100, 90))
    }
}
