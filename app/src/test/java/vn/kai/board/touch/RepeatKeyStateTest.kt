package vn.kai.board.touch

import org.junit.Assert.*
import org.junit.Test

class RepeatKeyStateTest {
    @Test fun secondPointerCannotStealRepeat() {
        val repeat = RepeatKeyState()
        assertTrue(repeat.start(1))
        assertFalse(repeat.start(2))
        assertTrue(repeat.isActive(1))
    }
    @Test fun releasingOwnerStopsImmediately() {
        val repeat = RepeatKeyState()
        repeat.start(7)
        assertTrue(repeat.stop(7))
        assertNull(repeat.pointerId)
    }
    @Test fun unrelatedReleaseDoesNotStopOwner() {
        val repeat = RepeatKeyState()
        repeat.start(7)
        assertFalse(repeat.stop(8))
        assertTrue(repeat.isActive(7))
    }
    @Test fun cancelAlwaysStopsRepeat() {
        val repeat = RepeatKeyState()
        repeat.start(3)
        repeat.cancel()
        assertNull(repeat.pointerId)
    }
}
