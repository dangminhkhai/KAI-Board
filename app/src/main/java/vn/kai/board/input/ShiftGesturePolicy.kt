package vn.kai.board.input

object ShiftGesturePolicy {
    const val DOUBLE_TAP_TIMEOUT_MS = 300L

    fun isDoubleTap(previousTapMs: Long, currentTapMs: Long): Boolean =
        previousTapMs >= 0L && currentTapMs >= previousTapMs &&
            currentTapMs - previousTapMs <= DOUBLE_TAP_TIMEOUT_MS
}
