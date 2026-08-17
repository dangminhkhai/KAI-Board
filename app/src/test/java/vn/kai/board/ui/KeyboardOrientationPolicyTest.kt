package vn.kai.board.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardOrientationPolicyTest {
    @Test fun landscapeTemporarilyUsesDefaultGeometry() {
        assertEquals(220, KeyboardOrientationPolicy.heightDp(280, landscape = true))
        assertEquals(0, KeyboardOrientationPolicy.bottomOffsetDp(80, landscape = true))
        assertEquals(100, KeyboardOrientationPolicy.widthPercent(75, landscape = true))
        assertEquals(0, KeyboardOrientationPolicy.leftOffsetDp(42, landscape = true))
        assertFalse(KeyboardOrientationPolicy.adjustmentEnabled(saved = true, landscape = true))
    }

    @Test fun portraitKeepsSavedGeometry() {
        assertEquals(280, KeyboardOrientationPolicy.heightDp(280, landscape = false))
        assertEquals(80, KeyboardOrientationPolicy.bottomOffsetDp(80, landscape = false))
        assertEquals(75, KeyboardOrientationPolicy.widthPercent(75, landscape = false))
        assertEquals(42, KeyboardOrientationPolicy.leftOffsetDp(42, landscape = false))
        assertTrue(KeyboardOrientationPolicy.adjustmentEnabled(saved = true, landscape = false))
    }
}
