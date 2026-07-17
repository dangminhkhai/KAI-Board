package vn.kai.board.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionDeletionPolicyTest {
    @Test fun deletesWhenSelectionCallbackReportsActiveRange() {
        assertTrue(SelectionDeletionPolicy.shouldDeleteSelection(true, null))
    }

    @Test fun deletesWhenEditorReturnsSelectedText() {
        assertTrue(SelectionDeletionPolicy.shouldDeleteSelection(false, "cụm từ"))
    }

    @Test fun fallsBackToNormalBackspaceWithoutSelection() {
        assertFalse(SelectionDeletionPolicy.shouldDeleteSelection(false, ""))
        assertFalse(SelectionDeletionPolicy.shouldDeleteSelection(false, null))
    }
}
