package vn.kai.board.ui

object KeyboardHeightPolicy {
    private const val LANDSCAPE_MAX_PERCENT = 62

    fun cap(requestedPixels: Int, screenHeightPixels: Int, landscape: Boolean): Int {
        if (!landscape || screenHeightPixels <= 0) return requestedPixels
        val maximum = screenHeightPixels * LANDSCAPE_MAX_PERCENT / 100
        return requestedPixels.coerceAtMost(maximum)
    }

    /**
     * Normal layouts already rebuild their key geometry to the measured height, so applying the
     * old requested height again would shift landscape labels above the canvas. Only adjustment
     * mode keeps a larger viewport and needs its preview content bottom-aligned.
     */
    fun contentTranslateY(viewHeight: Float, requestedContentHeight: Float, adjustmentMode: Boolean): Float =
        if (adjustmentMode) viewHeight - requestedContentHeight else 0f
}
