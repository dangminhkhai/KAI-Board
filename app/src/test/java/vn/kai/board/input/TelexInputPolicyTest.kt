package vn.kai.board.input

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelexInputPolicyTest {
    @Test fun enabledForNormalAndMultilineText() {
        assertTrue(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT))
        assertTrue(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE))
    }

    @Test fun enabledForUrlButDisabledForEmailWebEditorAndPasswords() {
        assertTrue(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT))
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
    }

    @Test fun disabledForNumericFields() {
        assertFalse(TelexInputPolicy.isEnabled(InputType.TYPE_CLASS_NUMBER))
    }
}
