package vn.kai.board.input

object WordRecomposer {
    fun beforeSingleWhitespace(textBeforeCursor: CharSequence): String? {
        if (textBeforeCursor.isEmpty() || !textBeforeCursor.last().isWhitespace()) return null
        if (textBeforeCursor.length >= 2 && textBeforeCursor[textBeforeCursor.lastIndex - 1].isWhitespace()) return null

        val wordEnd = textBeforeCursor.lastIndex
        var wordStart = wordEnd
        while (wordStart > 0 && textBeforeCursor[wordStart - 1].isLetter()) wordStart--
        return textBeforeCursor.substring(wordStart, wordEnd).takeIf { it.isNotEmpty() }
    }
}
