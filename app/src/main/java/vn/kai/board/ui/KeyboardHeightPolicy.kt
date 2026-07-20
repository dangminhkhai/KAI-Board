package vn.kai.board.ui

object KeyboardHeightPolicy {
    private const val LANDSCAPE_MAX_PERCENT = 62

    fun cap(requestedPixels: Int, screenHeightPixels: Int, landscape: Boolean): Int {
        if (!landscape || screenHeightPixels <= 0) return requestedPixels
        val maximum = screenHeightPixels * LANDSCAPE_MAX_PERCENT / 100
        return requestedPixels.coerceAtMost(maximum)
    }
}
