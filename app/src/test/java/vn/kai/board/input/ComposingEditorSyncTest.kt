package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposingEditorSyncTest {
    @Test fun deletesExactPreviousSuffix() {
        assertEquals("Safe", ComposingEditorSync.suffixToDelete("hello Safe", "Safe", "Saf"))
    }

    @Test fun repairsDoubledToneLetterSaff() {
        assertEquals("Saff", ComposingEditorSync.suffixToDelete("Saff", "Safe", "Saf"))
        assertEquals("Saff", ComposingEditorSync.suffixToDelete("xxSaff", "Safe", "Saf"))
        assertEquals("Saff", ComposingEditorSync.suffixToDelete("Saff", "Saf", "Saf"))
    }

    @Test fun repairsCafeToCaffCorruption() {
        assertEquals("Caff", ComposingEditorSync.suffixToDelete("Caff", "Cafe", "Caf"))
    }

    @Test fun idempotentWhenNextAlreadyPresent() {
        assertEquals("Saf", ComposingEditorSync.suffixToDelete("Saf", "Safe", "Saf"))
    }

    @Test fun prefixShrinkSafeToSaf() {
        assertTrue(ComposingEditorSync.isPrefixShrink("Safe", "Saf"))
        assertEquals(1, ComposingEditorSync.prefixShrinkDeleteLength("Safe", "Saf"))
        assertTrue(ComposingEditorSync.isPrefixShrink("Cafe", "Caf"))
        assertTrue(ComposingEditorSync.isPrefixShrink("Sà", "S"))
        assertFalse(ComposingEditorSync.isPrefixShrink("Sà", "Safe"))
    }

    @Test fun detectsTrailingTelexToneLetterRisk() {
        assertTrue(ComposingEditorSync.endsWithTelexToneLetter("Saf"))
        assertTrue(ComposingEditorSync.endsWithTelexToneLetter("Mas"))
        assertFalse(ComposingEditorSync.endsWithTelexToneLetter("Safe"))
        assertFalse(ComposingEditorSync.endsWithTelexToneLetter("hello"))
        assertFalse(ComposingEditorSync.endsWithTelexToneLetter("s"))
    }

    @Test fun vivoFamilyPrefersDirectCommit() {
        assertTrue(ComposingEditorSync.manufacturersPreferDirectCommit("vivo", "vivo"))
        assertTrue(ComposingEditorSync.manufacturersPreferDirectCommit("BBK", "iQOO"))
    }
}
