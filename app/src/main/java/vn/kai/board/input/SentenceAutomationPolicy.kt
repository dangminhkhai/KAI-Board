package vn.kai.board.input

object SentenceAutomationPolicy {
    fun shouldCapitalize(textBeforeCursor: CharSequence?): Boolean {
        val text = textBeforeCursor?.toString().orEmpty()
        if (text.isBlank()) return true
        val withoutHorizontalWhitespace = text.trimEnd { it == ' ' || it == '\t' || it == '\r' }
        if (withoutHorizontalWhitespace.endsWith('\n')) return true
        val lastNonWhitespace = text.indexOfLast { !it.isWhitespace() }
        return lastNonWhitespace < 0 || text[lastNonWhitespace] in ".!?"
    }

    fun periodOutput(autoSpace: Boolean): String = if (autoSpace) ". " else "."

    fun alreadyHasAutomaticPeriodSpace(textBeforeCursor: CharSequence?): Boolean =
        textBeforeCursor?.toString()?.endsWith(". ") == true
}
