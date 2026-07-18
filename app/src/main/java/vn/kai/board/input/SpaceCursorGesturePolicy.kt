package vn.kai.board.input

object SpaceCursorGesturePolicy {
    fun steps(deltaX: Float, stepWidth: Float): Int =
        if (stepWidth <= 0f) 0 else (deltaX / stepWidth).toInt()
}
