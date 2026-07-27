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
    @Test fun repeatAcceleratesAndStopsAtSafeMinimum() {
        val repeat = RepeatKeyState()
        repeat.start(3)
        val delays = List(30) { repeat.nextDelayMs() }
        assertTrue(delays.zipWithNext().all { (first, second) -> second <= first })
        assertTrue(delays.last() < delays.first())
        assertEquals(12L, delays.last())
        // First step should already be faster than the old 55ms start.
        assertTrue(delays.first() <= 40L)
    }
}
