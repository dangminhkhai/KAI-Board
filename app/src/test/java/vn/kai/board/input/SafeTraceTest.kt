package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** End-to-end Telex state for the Vivo Safe→Saff regression (logic side). */
class SafeTraceTest {
    @Test fun safeBackspaceIsPrefixShrinkToSaf() {
        var state = TelexComposeResult("", 0, "")
        for (c in "Safe") {
            state = TelexWordComposer.append(state.text, c, state.literalLockLength, state.rawText)
        }
        assertEquals("Safe", state.text)
        val after = TelexWordComposer.backspace(state.text, state.rawText, state.literalLockLength)
        assertEquals("Saf", after.text)
        assertTrue(ComposingEditorSync.isPrefixShrink(state.text, after.text))
        assertEquals(1, ComposingEditorSync.prefixShrinkDeleteLength(state.text, after.text))
    }

    @Test fun cafeBackspaceIsPrefixShrinkToCaf() {
        var state = TelexComposeResult("", 0, "")
        for (c in "Cafe") {
            state = TelexWordComposer.append(state.text, c, state.literalLockLength, state.rawText)
        }
        val after = TelexWordComposer.backspace(state.text, state.rawText, state.literalLockLength)
        assertEquals("Caf", after.text)
        assertTrue(ComposingEditorSync.isPrefixShrink("Cafe", after.text))
    }
}
