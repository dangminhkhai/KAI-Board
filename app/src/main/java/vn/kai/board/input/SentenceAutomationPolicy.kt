package vn.kai.board.input

object SentenceAutomationPolicy {
    fun shouldCapitalize(textBeforeCursor: CharSequence?): Boolean {
        val text = textBeforeCursor?.toString().orEmpty()
        if (text.isBlank()) return true
        val withoutHorizontalWhitespace = text.trimEnd { it == ' ' || it == '\t' || it == '\r' }
        return withoutHorizontalWhitespace.endsWith('\n')
    }
}
