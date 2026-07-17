package vn.kai.board.touch

class RepeatKeyState {
    var pointerId: Int? = null
        private set

    fun start(id: Int): Boolean {
        if (pointerId != null) return false
        pointerId = id
        return true
    }
    fun isActive(id: Int): Boolean = pointerId == id
    fun stop(id: Int): Boolean {
        if (pointerId != id) return false
        pointerId = null
        return true
    }
    fun cancel() { pointerId = null }
}
