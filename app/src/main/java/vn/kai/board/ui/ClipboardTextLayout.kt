package vn.kai.board.ui

import android.graphics.Paint

/**
 * Measure-based wrap/ellipsis for clipboard card labels.
 * Wide Vietnamese / CJK glyphs must never overflow the card width.
 */
object ClipboardTextLayout {
    fun wrapLines(paint: Paint, text: String, maxWidth: Float, maxLines: Int): List<String> =
        wrapLines(
            text = text,
            maxWidth = maxWidth,
            maxLines = maxLines,
            measure = { paint.measureText(it) },
            breakText = { s, w -> paint.breakText(s, true, w, null) },
        )

    fun ellipsize(paint: Paint, text: String, maxWidth: Float): String =
        ellipsize(
            text = text,
            maxWidth = maxWidth,
            measure = { paint.measureText(it) },
            breakText = { s, w -> paint.breakText(s, true, w, null) },
        )

    /** Testable core: inject measure/break (px widths). */
    fun wrapLines(
        text: String,
        maxWidth: Float,
        maxLines: Int,
        measure: (String) -> Float,
        breakText: (String, Float) -> Int,
    ): List<String> {
        if (text.isEmpty() || maxLines <= 0 || maxWidth <= 0f) return emptyList()
        val lines = ArrayList<String>(maxLines)
        var remaining = text
        while (remaining.isNotEmpty() && lines.size < maxLines) {
            val lastLine = lines.size == maxLines - 1
            if (lastLine) {
                lines += ellipsize(remaining, maxWidth, measure, breakText)
                break
            }
            var count = breakText(remaining, maxWidth).coerceAtLeast(1)
            if (count < remaining.length) {
                val wordBreak = remaining.lastIndexOf(' ', count - 1)
                if (wordBreak > 0) count = wordBreak
            }
            lines += remaining.take(count).trimEnd()
            remaining = remaining.drop(count).trimStart()
        }
        return lines
    }

    fun ellipsize(
        text: String,
        maxWidth: Float,
        measure: (String) -> Float,
        breakText: (String, Float) -> Int,
    ): String {
        if (text.isEmpty() || maxWidth <= 0f) return ""
        if (measure(text) <= maxWidth) return text
        val ellipsis = "…"
        val ellW = measure(ellipsis)
        val budget = (maxWidth - ellW).coerceAtLeast(1f)
        val fit = breakText(text, budget).coerceAtLeast(0)
        return text.take(fit).trimEnd() + ellipsis
    }
}
