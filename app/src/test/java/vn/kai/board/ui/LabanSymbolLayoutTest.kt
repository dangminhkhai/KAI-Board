package vn.kai.board.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LabanSymbolLayoutTest {
    @Test fun primaryPageMatchesLabanCommonSymbols() {
        val page = LabanSymbolLayout.page(0)

        assertEquals("1234567890", page.numberRow)
        assertEquals("~`|•√π÷×¶Δ", page.topRow)
        assertEquals("@#\$%&-+()/", page.middleRow)
        assertEquals("*\"':;!?", page.actionRow)
    }

    @Test fun secondaryPageKeepsNumbersAndTopRowWhileChangingLowerSymbols() {
        val primary = LabanSymbolLayout.page(0)
        val secondary = LabanSymbolLayout.page(1)

        assertEquals(primary.numberRow, secondary.numberRow)
        assertEquals(primary.topRow, secondary.topRow)
        assertEquals("£¢€¥^°={}…", secondary.middleRow)
        assertEquals("\\©®™%[]", secondary.actionRow)
    }
}
