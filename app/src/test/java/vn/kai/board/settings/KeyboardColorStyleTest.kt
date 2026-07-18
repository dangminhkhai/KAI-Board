package vn.kai.board.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Test fun legacyGradientIdsMapToAiGradient() {
        assertEquals(KeyboardColorStyle.AI_GRADIENT_2026, KeyboardColorStyle.fromStorage("ai_gradient_2026"))
        assertEquals(KeyboardColorStyle.AI_GRADIENT_2026, KeyboardColorStyle.fromStorage("gemini_ai_gradient"))
    }

    @Test fun unknownValuesFallBackToClassic() {
        assertEquals(KeyboardColorStyle.CLASSIC, KeyboardColorStyle.fromStorage(null))
        assertEquals(KeyboardColorStyle.CLASSIC, KeyboardColorStyle.fromStorage("unknown_style"))
    }

    @Test fun palettesProvideAccentAndOptionalGradient() {
        val classic = KeyboardThemePalette.resolve(KeyboardColorStyle.CLASSIC, false)
        val gradient = KeyboardThemePalette.resolve(KeyboardColorStyle.AI_GRADIENT_2026, false)
        val ocean = KeyboardThemePalette.resolve(KeyboardColorStyle.OCEAN, true)
        assertNull(classic.gradientColors)
        assertNotNull(gradient.gradientColors)
        assertEquals(3, gradient.gradientColors!!.size)
        assertNull(ocean.gradientColors)
        assertEquals(0xFF1F7F70.toInt(), classic.accent)
        assertEquals(0xFF4F46E5.toInt(), gradient.accent)
    }

    @Test fun regularKeysAreWhiteInLightModeAndBlackInDarkMode() {
        KeyboardColorStyle.entries.forEach { style ->
            assertEquals(0xFFFFFFFF.toInt(), KeyboardThemePalette.resolve(style, false).key)
            assertEquals(0xFF000000.toInt(), KeyboardThemePalette.resolve(style, true).key)
        }
    }
}
