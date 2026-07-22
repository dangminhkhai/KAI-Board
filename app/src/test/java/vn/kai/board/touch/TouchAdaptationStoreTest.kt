package vn.kai.board.touch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.kai.board.input.KeyAction

class TouchAdaptationStoreTest {
    private val a = KeyGeometry("1-a", "a", KeyAction.Character('a'), 0f, 0f, 40f, 50f)
    private val s = KeyGeometry("1-s", "s", KeyAction.Character('s'), 46f, 0f, 86f, 50f)

    @Test
    fun stableIdUsesCharacterNotRow() {
        assertEquals("c:a", TouchAdaptationStore.stableId(a))
        assertEquals(
            "c:a",
            TouchAdaptationStore.stableId(a.copy(id = "2-a", action = KeyAction.Character('A'))),
        )
        assertEquals("space", TouchAdaptationStore.stableId(
            KeyGeometry("space", " ", KeyAction.Space, 0f, 0f, 100f, 40f),
        ))
        assertNull(TouchAdaptationStore.stableId(
            KeyGeometry("toolbar-ai", "", KeyAction.OpenAi, 0f, 0f, 40f, 40f),
        ))
    }

    @Test
    fun adaptiveCenterPullsTowardHabitualTap() {
        // Gap mid-point leans to "s" geometrically; shift "a" center right so it wins.
        val policy = TouchTargetPolicy(10f)
        assertEquals("1-s", policy.resolve(listOf(a, s), 45f, 25f)?.id)
        policy.setCenterBiasPx(mapOf("1-a" to (14f to 0f))) // a effective center ~34
        assertEquals("1-a", policy.resolve(listOf(a, s), 45f, 25f)?.id)
    }

    @Test
    fun withoutBiasNearestGeometricCenterWins() {
        val policy = TouchTargetPolicy(10f)
        assertEquals("1-s", policy.resolve(listOf(a, s), 45f, 25f)?.id)
    }

    @Test
    fun strengthScalesWithSamples() {
        val weak = TouchAdaptationStore.Bias(0.2f, 0f, 3)
        val full = TouchAdaptationStore.Bias(0.2f, 0f, 20)
        assertTrue(weak.strength < full.strength)
        assertEquals(1f, full.strength, 0.001f)
        assertNotNull(TouchAdaptationStore.stableId(a))
    }
}
