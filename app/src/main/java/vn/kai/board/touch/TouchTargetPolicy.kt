package vn.kai.board.touch

/**
 * Hit-test keys with expanded targets and nearest-center scoring.
 *
 * Optional [centerBiasPx] shifts the scored center per key id (from [TouchAdaptationStore])
 * so habitual off-center taps still land on the intended key without redrawing the layout.
 */
class TouchTargetPolicy(
    private val expansionPx: Float,
    private val outerEdgeExpansionPx: Float = expansionPx,
) {
    /** key geometry id → (dx, dy) added to the geometric center for distance scoring. */
    @Volatile
    private var centerBiasPx: Map<String, Pair<Float, Float>> = emptyMap()

    fun setCenterBiasPx(bias: Map<String, Pair<Float, Float>>) {
        centerBiasPx = bias
    }

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
                val bias = centerBiasPx[key.id]
                val cx = key.centerX + (bias?.first ?: 0f)
                val cy = key.centerY + (bias?.second ?: 0f)
                val dx = x - cx
                val dy = y - cy
                dx * dx + dy * dy
            }
}
