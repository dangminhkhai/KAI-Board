package vn.kai.board.touch

import vn.kai.board.input.KeyAction

data class KeyGeometry(
    val id: String,
    val label: String,
    val action: KeyAction,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float, expansion: Float): Boolean =
        x >= left - expansion && x <= right + expansion &&
            y >= top - expansion && y <= bottom + expansion

    fun contains(x: Float, y: Float, leftExpansion: Float, rightExpansion: Float, verticalExpansion: Float): Boolean =
        x >= left - leftExpansion && x <= right + rightExpansion &&
            y >= top - verticalExpansion && y <= bottom + verticalExpansion
}
