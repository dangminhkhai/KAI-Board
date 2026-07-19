package vn.kai.board.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardHeightPolicyTest {
    @Test fun portraitKeepsRequestedHeight() {
        assertEquals(900, KeyboardHeightPolicy.cap(900, 1_080, landscape = false))
    }

    @Test fun landscapeCapsHeightAtSixtyTwoPercent() {
        assertEquals(669, KeyboardHeightPolicy.cap(900, 1_080, landscape = true))
    }

    @Test fun landscapeDoesNotEnlargeShortKeyboard() {
        assertEquals(500, KeyboardHeightPolicy.cap(500, 1_080, landscape = true))
    }
}
