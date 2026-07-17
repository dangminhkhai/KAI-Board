package vn.kai.board.input

object SelectionDeletionPolicy {
    fun shouldDeleteSelection(selectionActive: Boolean, selectedText: CharSequence?): Boolean =
        selectionActive || !selectedText.isNullOrEmpty()
}
