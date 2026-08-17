package vn.kai.board.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardColorStyleTest {
    @Test fun storageValuesAreUniqueAndRoundTrip() {
        val values = KeyboardColorStyle.entries.map { it.storageValue }
        assertEquals(values.size, values.distinct().size)
        KeyboardColorStyle.entries.forEach { style ->
            assertEquals(style, KeyboardColorStyle.fromStorage(style.storageValue))
        }
    }

    @Test fun removedBuiltInStylesFallBackToClassic() {
        assertEquals(KeyboardColorStyle.CLASSIC, KeyboardColorStyle.fromStorage("ai_gradient_2026"))
        assertEquals(KeyboardColorStyle.CLASSIC, KeyboardColorStyle.fromStorage("ocean"))
        assertEquals(KeyboardColorStyle.CLASSIC, KeyboardColorStyle.fromStorage("pastel_forest"))
    }

    @Test fun unknownValuesFallBackToClassic() {
        assertEquals(KeyboardColorStyle.CLASSIC, KeyboardColorStyle.fromStorage(null))
        assertEquals(KeyboardColorStyle.CLASSIC, KeyboardColorStyle.fromStorage("unknown_style"))
    }

    @Test fun classicPaletteUsesSpecialKeyColorForEnter() {
        listOf(false, true).forEach { dark ->
            val classic = KeyboardThemePalette.resolve(KeyboardColorStyle.CLASSIC, dark)
            assertNull(classic.gradientColors)
            assertEquals(classic.specialKey, classic.actionKey)
        }
    }

    @Test fun nonIllustratedRegularKeysAreWhiteInLightMode() {
        KeyboardColorStyle.entries.forEach { style ->
            assertEquals(0xFFFFFFFF.toInt(), KeyboardThemePalette.resolve(style, false).key)
        }
        assertEquals(0xFF3C4043.toInt(), KeyboardThemePalette.resolve(KeyboardColorStyle.CLASSIC, true).key)
    }
}
