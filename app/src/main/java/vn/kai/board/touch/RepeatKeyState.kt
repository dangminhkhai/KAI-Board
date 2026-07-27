package vn.kai.board.touch

class RepeatKeyState {
    var pointerId: Int? = null
        private set
    private var repeatCount = 0

    fun start(id: Int): Boolean {
        if (pointerId != null) return false
        pointerId = id
        repeatCount = 0
        return true
    }
    /**
     * Interval before the next repeated Backspace while held.
     * Accelerates quickly so long holds clear text at Gboard-like speed.
     */
    fun nextDelayMs(): Long {
        repeatCount++
        // Start ~38ms, step down by 3ms → floor 12ms (was 55→18).
        return (41L - repeatCount * 3L).coerceAtLeast(12L)
    }
    fun isActive(id: Int): Boolean = pointerId == id
    fun stop(id: Int): Boolean {
        if (pointerId != id) return false
        pointerId = null
        repeatCount = 0
        return true
    }
    fun cancel() { pointerId = null; repeatCount = 0 }
}
