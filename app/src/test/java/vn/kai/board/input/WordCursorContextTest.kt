package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordCursorContextTest {
    @Test fun readsWordAtEndOfCursor() {
        assertEquals("tieng", WordCursorContext.read("toi tieng", "")?.word)
    }

    @Test fun joinsBothSidesWhenCursorIsInsideWord() {
        assertEquals("tieng", WordCursorContext.read("toi ti", "eng nua")?.word)
    }

    @Test fun stopsAtPunctuationAndWhitespace() {
        assertEquals("tieng", WordCursorContext.read("xin, tieng", ". sau")?.word)
    }

    @Test fun returnsNullOutsideAWord() {
        assertNull(WordCursorContext.read("xin ", " chao"))
    }
}
