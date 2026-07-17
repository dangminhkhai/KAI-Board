package vn.kai.board.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardColorStyleTest {
    @Test fun storageValuesAreUniqueAndRoundTrip() {
        val values = KeyboardColorStyle.entries.map { it.storageValue }
        assertEquals(values.size, values.distinct().size)
        KeyboardColorStyle.entries.forEach { style ->
            assertEquals(style, KeyboardColorStyle.fromStorage(style.storageValue))
        }
    }

    @Test fun oldPresetsFallBackToClassic() {
        assertEquals(KeyboardColorStyle.CLASSIC, KeyboardColorStyle.fromStorage("ai_gradient_2026"))
        assertEquals(KeyboardColorStyle.CLASSIC, KeyboardColorStyle.fromStorage("gemini_ai_gradient"))
    }
}
