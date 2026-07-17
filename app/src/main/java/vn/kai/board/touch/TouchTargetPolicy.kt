package vn.kai.board.touch

class TouchTargetPolicy(
    private val expansionPx: Float,
    private val outerEdgeExpansionPx: Float = expansionPx,
) {
    fun resolve(keys: List<KeyGeometry>, x: Float, y: Float): KeyGeometry? =
        keys.asSequence()
            .filter { key ->
                val row = keys.filter { it.top == key.top && it.bottom == key.bottom }
                val leftEdge = row.minOfOrNull { it.left } == key.left
                val rightEdge = row.maxOfOrNull { it.right } == key.right
                key.contains(
                    x,
                    y,
                    if (leftEdge) outerEdgeExpansionPx else expansionPx,
                    if (rightEdge) outerEdgeExpansionPx else expansionPx,
                    expansionPx,
                )
            }
            .minByOrNull { key ->
                val dx = x - key.centerX
                val dy = y - key.centerY
                dx * dx + dy * dy
            }
}
