package vn.kai.board.input

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelexInputPolicyTest {
    @Test fun enabledForNormalAndMultilineText() {
        assertTrue(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT))
        assertTrue(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE))
    }

    @Test fun enabledForUrlAndWebSearchButDisabledForEmailAndPasswords() {
        assertTrue(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
        assertTrue(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT))
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
    }

    @Test fun disabledForNumericFields() {
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_NUMBER))
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_NULL))
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_PHONE))
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_NUMBER_FLAG_DECIMAL))
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_NUMBER_FLAG_SIGNED))
    }

    @Test fun malformedTextFlagsWithoutTextClassRemainTelexCompatible() {
        val shopeeRestartedSearch = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        assertTrue(TelexInputPolicy.isEnabled(shopeeRestartedSearch))
        assertTrue(TelexInputPolicy.requiresDirectCommit(shopeeRestartedSearch, EditorInfo.IME_ACTION_SEARCH))
    }

    @Test fun noSuggestionsSearchFieldUsesDirectCommitCompatibility() {
        val shopeeSearch = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        assertTrue(TelexInputPolicy.isEnabled(shopeeSearch))
        assertTrue(TelexInputPolicy.requiresDirectCommit(shopeeSearch, EditorInfo.IME_ACTION_NONE))
        assertFalse(TelexInputPolicy.requiresDirectCommit(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_NONE))
    }

    @Test fun searchAndGoActionsUseDirectCommitEvenWithAutoCorrectFlag() {
        val shopeeSearch = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        assertTrue(TelexInputPolicy.requiresDirectCommit(shopeeSearch, EditorInfo.IME_ACTION_SEARCH))
        assertTrue(TelexInputPolicy.requiresDirectCommit(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_GO))
    }
}
