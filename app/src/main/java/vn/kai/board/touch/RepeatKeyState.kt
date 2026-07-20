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
    fun nextDelayMs(): Long {
        repeatCount++
        return (55L - repeatCount * 2L).coerceAtLeast(18L)
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
