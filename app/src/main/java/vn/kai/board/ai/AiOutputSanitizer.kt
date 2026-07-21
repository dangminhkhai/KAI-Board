package vn.kai.board.ai

/**
 * Cleans common model decoration before paste into the focused editor.
 * Models often wrap titles / short answers in ASCII or smart quotes.
 */
object AiOutputSanitizer {
    private val quotePairs = listOf(
        '"' to '"',
        '\u201C' to '\u201D', // “ ”
        '\u201E' to '\u201C', // „ “
        '\u00AB' to '\u00BB', // « »
        '\u2018' to '\u2019', // ‘ ’
        '\'' to '\'',
    )

    fun sanitize(raw: String): String {
        var text = raw.trim()
        if (text.isEmpty()) return text
        text = stripWrappingCodeFence(text)
        // Title lists first — do not treat the whole multi-line block as one outer quote pair.
        if (looksLikeQuotedLines(text)) {
            text = text.lineSequence()
                .map { line ->
                    val t = line.trim()
                    if (t.isEmpty()) t else stripOuterQuotes(t)
                }
                .joinToString("\n")
                .trim()
        } else {
            text = stripOuterQuotes(text)
        }
        return text
    }

    private fun looksLikeQuotedLines(text: String): Boolean {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 2) return false
        return lines.all { isFullyQuoted(it) }
    }

    private fun isFullyQuoted(text: String): Boolean {
        if (text.length < 2) return false
        val open = text.first()
        val close = text.last()
        return quotePairs.any { (o, c) -> open == o && close == c }
    }

    /** Strip one outer matching quote pair when it wraps the whole string. */
    fun stripOuterQuotes(text: String): String {
        val t = text.trim()
        if (t.length < 2) return t
        val open = t.first()
        val close = t.last()
        val matched = quotePairs.any { (o, c) -> open == o && close == c }
        if (!matched) return t
        // Keep inner content; allow nested quotes (e.g. He said "hi") after outer strip once.
        return t.substring(1, t.length - 1).trim()
    }

    private fun stripWrappingCodeFence(text: String): String {
        val t = text.trim()
        if (!t.startsWith("```")) return t
        val lines = t.lines()
        if (lines.size < 2 || !lines.last().trim().startsWith("```")) return t
        return lines.subList(1, lines.lastIndex).joinToString("\n").trim()
    }
}
