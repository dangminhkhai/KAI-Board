package vn.kai.board.ui

/** Two Laban Key-style symbol pages shown below a permanent phone number row. */
data class LabanSymbolPage(
    val numberRow: String,
    val topRow: String,
    val middleRow: String,
    val actionRow: String,
)

object LabanSymbolLayout {
    private const val NUMBER_ROW = "1234567890"
    private const val COMMON_TOP_ROW = "~`|•√π÷×¶Δ"

    fun page(index: Int): LabanSymbolPage = when (index) {
        0 -> LabanSymbolPage(
            numberRow = NUMBER_ROW,
            topRow = COMMON_TOP_ROW,
            middleRow = "@#\$%&-+()/",
            actionRow = "*\"':;!?",
        )
        else -> LabanSymbolPage(
            numberRow = NUMBER_ROW,
            topRow = COMMON_TOP_ROW,
            middleRow = "£¢€¥^°={}…",
            actionRow = "\\©®™%[]",
        )
    }
}
