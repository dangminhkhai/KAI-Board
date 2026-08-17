package vn.kai.board.ui

object KeyboardOrientationPolicy {
    const val DEFAULT_HEIGHT_DP = 220

    fun heightDp(saved: Int, landscape: Boolean): Int =
        if (landscape) DEFAULT_HEIGHT_DP else saved

    fun bottomOffsetDp(saved: Int, landscape: Boolean): Int =
        if (landscape) 0 else saved

    fun widthPercent(saved: Int, landscape: Boolean): Int =
        if (landscape) 100 else saved

    fun leftOffsetDp(saved: Int, landscape: Boolean): Int =
        if (landscape) 0 else saved

    fun adjustmentEnabled(saved: Boolean, landscape: Boolean): Boolean =
        saved && !landscape
}
