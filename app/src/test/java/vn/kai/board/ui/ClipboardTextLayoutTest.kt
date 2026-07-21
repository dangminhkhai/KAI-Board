package vn.kai.board.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests use monospace fake metrics: each code unit is 10px wide.
 * Ellipsis "…" is also treated as 10px so budgets are easy to reason about.
 */
class ClipboardTextLayoutTest {
    private val measure: (String) -> Float = { it.length * 10f }
    private val breakText: (String, Float) -> Int = { s, w ->
        (w / 10f).toInt().coerceIn(0, s.length)
    }

    @Test
    fun shortTextFitsWithoutEllipsis() {
        val lines = ClipboardTextLayout.wrapLines(
            text = "hello",
            maxWidth = 100f,
            maxLines = 2,
            measure = measure,
            breakText = breakText,
        )
        assertEquals(listOf("hello"), lines)
    }

    @Test
    fun wrapsAtWordBoundaryThenEllipsizes() {
        // 10px/char, 40px = 4 units. First line: break at 4 ("xin ") → wordBreak → "xin"
        // Second (last) line: budget 30px for body + "…" → "cha…"
        val lines = ClipboardTextLayout.wrapLines(
            text = "xin chao ban rat lau",
            maxWidth = 40f,
            maxLines = 2,
            measure = measure,
            breakText = breakText,
        )
        assertEquals(listOf("xin", "cha…"), lines)
    }

    @Test
    fun longUnbrokenWordSplitsByMeasure() {
        val lines = ClipboardTextLayout.wrapLines(
            text = "abcdefghijklmnopqrstuvwxyz",
            maxWidth = 50f,
            maxLines = 2,
            measure = measure,
            breakText = breakText,
        )
        assertEquals(2, lines.size)
        assertEquals("abcde", lines[0])
        assertTrue(lines[1].endsWith("…"))
        // last line budget = 50 - 10 (ellipsis) = 40 → 4 chars + …
        assertEquals("fghi…", lines[1])
    }

    @Test
    fun ellipsizeKeepsFullWhenFits() {
        val out = ClipboardTextLayout.ellipsize(
            text = "abc",
            maxWidth = 50f,
            measure = measure,
            breakText = breakText,
        )
        assertEquals("abc", out)
    }

    @Test
    fun ellipsizeAddsEllipsisWhenOverflow() {
        val out = ClipboardTextLayout.ellipsize(
            text = "abcdefghij",
            maxWidth = 50f,
            measure = measure,
            breakText = breakText,
        )
        assertEquals("abcd…", out)
        assertFalse(out.contains("e") && out.length > 5)
    }

    @Test
    fun emptyOrInvalidWidthReturnsEmpty() {
        assertTrue(
            ClipboardTextLayout.wrapLines(
                text = "x",
                maxWidth = 0f,
                maxLines = 2,
                measure = measure,
                breakText = breakText,
            ).isEmpty(),
        )
        assertEquals(
            "",
            ClipboardTextLayout.ellipsize(
                text = "x",
                maxWidth = 0f,
                measure = measure,
                breakText = breakText,
            ),
        )
    }
}
