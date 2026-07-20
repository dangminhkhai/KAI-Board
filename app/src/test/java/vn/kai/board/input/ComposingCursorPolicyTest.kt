package vn.kai.board.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposingCursorPolicyTest {
    @Test fun movingInsideComposingWordFinishesComposition() {
        assertTrue(ComposingCursorPolicy.shouldFinish(true, false, 2, 2, 0, 4))
    }

    @Test fun cursorAtTrailingEdgeKeepsComposition() {
        assertFalse(ComposingCursorPolicy.shouldFinish(true, false, 4, 4, 0, 4))
    }

    @Test fun selectionAndOutsideCursorFinishComposition() {
        assertTrue(ComposingCursorPolicy.shouldFinish(true, false, 1, 3, 0, 4))
        assertTrue(ComposingCursorPolicy.shouldFinish(true, false, 5, 5, 0, 4))
    }
}
