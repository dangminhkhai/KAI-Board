package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhraseStatsTest {
    @Test
    fun rateIsNullWhenNothingShown() {
        val snap = PhraseStats.Snapshot(0, 0, 0, 0)
        assertNull(snap.personalRatePercent())
        assertNull(snap.packRatePercent())
    }

    @Test
    fun rateRoundsDownToPercent() {
        val snap = PhraseStats.Snapshot(
            personalShown = 10,
            personalAccepted = 3,
            packShown = 4,
            packAccepted = 1,
        )
        assertEquals(30, snap.personalRatePercent())
        assertEquals(25, snap.packRatePercent())
    }
}
