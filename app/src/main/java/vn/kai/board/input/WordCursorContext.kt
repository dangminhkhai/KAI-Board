package vn.kai.board.input

data class WordCursorContext(
    val before: String,
    val after: String,
) {
    val word: String get() = before + after

    companion object {
        fun read(textBeforeCursor: CharSequence, textAfterCursor: CharSequence): WordCursorContext? {
            val before = textBeforeCursor.takeLastWhile(Char::isLetter).toString()
            val after = textAfterCursor.takeWhile(Char::isLetter).toString()
            if (before.isEmpty() && after.isEmpty()) return null
            return WordCursorContext(before, after)
        }
    }
}
