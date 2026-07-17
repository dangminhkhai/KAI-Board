package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailSuggestionEngineTest {
    @Test fun completesCommonDomains() {
        assertEquals(listOf("kai@gmail.com"), EmailSuggestionEngine.suggest("kai@gm", emptyList()))
    }

    @Test fun prioritizesPreviouslyUsedAddresses() {
        val saved = listOf("kai@example.com", "kai@gmail.com")
        assertEquals("kai@example.com", EmailSuggestionEngine.suggest("kai@", saved).first())
    }

    @Test fun readsOnlyTokenAtCursor() {
        assertEquals("kai@gm", EmailSuggestionEngine.currentToken("send to kai@gm"))
    }

    @Test fun validatesCompletedAddressBeforeLearning() {
        assertTrue(EmailSuggestionEngine.isCompleteEmail("kai@example.com"))
        assertFalse(EmailSuggestionEngine.isCompleteEmail("kai@example"))
        assertFalse(EmailSuggestionEngine.isCompleteEmail("kai @example.com"))
    }
}
