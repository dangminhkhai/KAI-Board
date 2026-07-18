package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceCursorGesturePolicyTest {
    @Test fun convertsHorizontalDistanceToSignedCursorSteps() {
        assertEquals(0, SpaceCursorGesturePolicy.steps(11f, 12f))
        assertEquals(2, SpaceCursorGesturePolicy.steps(25f, 12f))
        assertEquals(-2, SpaceCursorGesturePolicy.steps(-25f, 12f))
    }
}
