package vn.kai.board.input

/** Ends IME composition when the user moves the cursor away from its trailing edge. */
object ComposingCursorPolicy {
    fun shouldFinish(
        hasComposingText: Boolean,
        directCommit: Boolean,
        newSelectionStart: Int,
        newSelectionEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ): Boolean {
        if (!hasComposingText || directCommit || candidatesStart < 0 || candidatesEnd < 0) return false
        val hasSelection = newSelectionStart != newSelectionEnd
        val cursorOutside = newSelectionStart < candidatesStart || newSelectionEnd > candidatesEnd
        val cursorInsideWord = !hasSelection && newSelectionEnd != candidatesEnd
        return hasSelection || cursorOutside || cursorInsideWord
    }
}
