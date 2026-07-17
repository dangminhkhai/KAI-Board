package vn.kai.board.touch

class TouchDispatcher(private val slideThreshold: Float) {
    data class PointerState(
        var key: KeyGeometry?,
        val downX: Float,
        val downY: Float,
        var longPressed: Boolean = false,
    )

    private val states = linkedMapOf<Int, PointerState>()
    val values: Collection<PointerState> get() = states.values

    operator fun get(pointerId: Int): PointerState? = states[pointerId]
    fun down(pointerId: Int, key: KeyGeometry?, x: Float, y: Float) {
        states[pointerId] = PointerState(key, x, y)
    }
    fun crossedSlideThreshold(pointerId: Int, x: Float, y: Float): Boolean {
        val state = states[pointerId] ?: return false
        val dx = x - state.downX
        val dy = y - state.downY
        return dx * dx + dy * dy >= slideThreshold * slideThreshold
    }
    fun remove(pointerId: Int): PointerState? = states.remove(pointerId)
    fun clear() = states.clear()
}
