package vn.kai.board.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.kai.board.input.LongPressSymbolMap

class GboardSymbolLayoutTest {
    @Test fun firstPageMatchesAlphabetLongPressHints() {
        val page = GboardSymbolLayout.page(0)

        assertEquals("1234567890", page.numberRow)
        assertEquals(symbolsFor("qwertyuiop"), page.topRow)
        assertEquals(symbolsFor("asdfghjkl"), page.middleRow)
        assertEquals(symbolsFor("zxcvbnm"), page.actionRow)
        assertEquals("`~[]{}<>^|", page.topRow)
        assertEquals("@#\$%&-+()", page.middleRow)
        assertEquals("*\"':;!?", page.actionRow)
    }

    @Test fun secondPageChangesSymbolsButKeepsNumberRow() {
        val first = GboardSymbolLayout.page(0)
        val second = GboardSymbolLayout.page(1)

        assertEquals(first.numberRow, second.numberRow)
        assertEquals("1234567890", second.numberRow)
        assertEquals("[]{}#%^*+=", second.topRow)
        assertEquals("_\\|~<>€£¥•", second.middleRow)
        assertEquals(".,?!'", second.actionRow)
    }

    private fun symbolsFor(keys: String): String = keys
        .mapNotNull(LongPressSymbolMap::forKey)
        .joinToString(separator = "")
}
