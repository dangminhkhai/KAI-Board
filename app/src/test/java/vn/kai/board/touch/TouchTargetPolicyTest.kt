package vn.kai.board.touch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import vn.kai.board.input.KeyAction

class TouchTargetPolicyTest {
    private val left = KeyGeometry("a", "a", KeyAction.Character('a'), 0f, 0f, 40f, 50f)
    private val right = KeyGeometry("s", "s", KeyAction.Character('s'), 46f, 0f, 86f, 50f)
    private val keys = listOf(left, right)

    @Test fun resolvesKeyCenter() {
        assertEquals("a", TouchTargetPolicy(6f).resolve(keys, 20f, 25f)?.id)
    }

    @Test fun resolvesExpandedGapByNearestCenter() {
        assertEquals("a", TouchTargetPolicy(6f).resolve(keys, 42f, 25f)?.id)
        assertEquals("s", TouchTargetPolicy(6f).resolve(keys, 44f, 25f)?.id)
    }

    @Test fun rejectsPointOutsideExpandedTargets() {
        assertNull(TouchTargetPolicy(6f).resolve(keys, 100f, 25f))
    }

    @Test fun acceptsOuterEdgeInsideExpansion() {
        assertEquals("a", TouchTargetPolicy(6f).resolve(keys, -5f, 25f)?.id)
    }

    @Test fun nearestCenterWinsWhenExpandedTargetsOverlap() {
        assertEquals("s", TouchTargetPolicy(10f).resolve(keys, 45f, 25f)?.id)
    }

    @Test fun outerEdgeCanBeWiderThanInnerGaps() {
        assertEquals("a", TouchTargetPolicy(2f, 14f).resolve(keys, -12f, 25f)?.id)
        assertNull(TouchTargetPolicy(2f, 14f).resolve(keys, 43f, 25f))
    }

    @Test fun adaptiveBiasCanPreferOffCenterKey() {
        val policy = TouchTargetPolicy(10f)
        assertEquals("s", policy.resolve(keys, 45f, 25f)?.id)
        policy.setCenterBiasPx(mapOf("a" to (14f to 0f)))
        // Geometric lean to s; biased a-center pulls the decision back.
        assertEquals("a", policy.resolve(keys, 45f, 25f)?.id)
    }
}
