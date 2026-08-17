package vn.kai.board.ui

import vn.kai.board.input.LongPressSymbolMap

/** Symbol pages shown below a permanent phone-style number row. */
data class GboardSymbolPage(
    val numberRow: String,
    val topRow: String,
    val middleRow: String,
    val actionRow: String,
)

object GboardSymbolLayout {
    private const val NUMBER_ROW = "1234567890"

    fun page(index: Int): GboardSymbolPage = when (index) {
        0 -> GboardSymbolPage(
            numberRow = NUMBER_ROW,
            topRow = symbolsFor("qwertyuiop"),
            middleRow = symbolsFor("asdfghjkl"),
            actionRow = symbolsFor("zxcvbnm"),
        )
        else -> GboardSymbolPage(
            numberRow = NUMBER_ROW,
            topRow = "[]{}#%^*+=",
            middleRow = "_\\|~<>€£¥•",
            actionRow = ".,?!'",
        )
    }

    private fun symbolsFor(keys: String): String = keys
        .mapNotNull(LongPressSymbolMap::forKey)
        .joinToString(separator = "")
}
