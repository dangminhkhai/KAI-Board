package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LongPressSymbolMapTest {
    @Test fun mapsRequestedAKeyToAtSign() = assertEquals('@', LongPressSymbolMap.forKey('a'))
    @Test fun supportsUppercaseKeys() = assertEquals('@', LongPressSymbolMap.forKey('A'))
    @Test fun mapsTopRowFromBacktickToPipe() = assertEquals("`~[]{}<>^|".toList(), "qwertyuiop".map { LongPressSymbolMap.forKey(it) })
    @Test fun ignoresNonLetterKeys() = assertNull(LongPressSymbolMap.forKey(','))
}
