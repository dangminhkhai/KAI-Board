package vn.kai.board.touch

import org.junit.Assert.*
import org.junit.Test
import vn.kai.board.input.KeyAction

class TouchDispatcherTest {
    private val a = KeyGeometry("a", "a", KeyAction.Character('a'), 0f, 0f, 40f, 50f)
    private val s = KeyGeometry("s", "s", KeyAction.Character('s'), 44f, 0f, 84f, 50f)

    @Test fun rapidAlternatingPointersRemainIndependent() {
        val dispatcher = TouchDispatcher(18f)
        dispatcher.down(1, a, 20f, 25f)
        dispatcher.down(2, s, 64f, 25f)
        assertSame(a, dispatcher.remove(1)?.key)
        assertSame(s, dispatcher.remove(2)?.key)
    }
    @Test fun smallJitterKeepsLockedKey() {
        val dispatcher = TouchDispatcher(18f)
        dispatcher.down(1, a, 20f, 25f)
        assertFalse(dispatcher.crossedSlideThreshold(1, 28f, 30f))
    }
    @Test fun deliberateSlideCrossesThreshold() {
        val dispatcher = TouchDispatcher(18f)
        dispatcher.down(1, a, 20f, 25f)
        assertTrue(dispatcher.crossedSlideThreshold(1, 50f, 25f))
    }
    @Test fun cancelClearsEveryPointerAndLongPressState() {
        val dispatcher = TouchDispatcher(18f)
        dispatcher.down(1, a, 20f, 25f)
        dispatcher[1]!!.longPressed = true
        dispatcher.clear()
        assertTrue(dispatcher.values.isEmpty())
    }
}
