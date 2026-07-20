package vn.kai.board.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftGesturePolicyTest {
    @Test fun secondQuickTapLocksCapitalization() {
        assertTrue(ShiftGesturePolicy.isDoubleTap(1_000L, 1_280L))
    }

    @Test fun slowOrInvalidTapDoesNotLock() {
        assertFalse(ShiftGesturePolicy.isDoubleTap(1_000L, 1_301L))
        assertFalse(ShiftGesturePolicy.isDoubleTap(-1L, 100L))
        assertFalse(ShiftGesturePolicy.isDoubleTap(200L, 100L))
    }
}
