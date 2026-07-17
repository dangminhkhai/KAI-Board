package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordRecomposerTest {
    @Test fun findsPlainVietnameseWordBeforeSpace() {
        assertEquals("chao", WordRecomposer.beforeSingleWhitespace("toi chao "))
    }

    @Test fun findsAlreadyMarkedVietnameseWord() {
        assertEquals("tiếng", WordRecomposer.beforeSingleWhitespace("gõ tiếng "))
    }

    @Test fun worksAfterNewerWordWasDeleted() {
        assertEquals("chao", WordRecomposer.beforeSingleWhitespace("thu chao "))
    }

    @Test fun doesNotJumpAcrossMultipleSpaces() {
        assertNull(WordRecomposer.beforeSingleWhitespace("chao  "))
    }

    @Test fun punctuationIsAWordBoundary() {
        assertNull(WordRecomposer.beforeSingleWhitespace("chao, "))
    }
}
