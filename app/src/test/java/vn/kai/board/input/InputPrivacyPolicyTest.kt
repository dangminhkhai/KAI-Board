package vn.kai.board.input

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputPrivacyPolicyTest {
    @Test fun detectsAllTextPasswordVariations() {
        assertTrue(InputPrivacyPolicy.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertTrue(InputPrivacyPolicy.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD))
        assertTrue(InputPrivacyPolicy.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
        assertTrue(InputPrivacyPolicy.isSensitive(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
    }

    @Test fun allowsNormalTextAndNonTextFields() {
        assertFalse(InputPrivacyPolicy.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL))
        assertFalse(InputPrivacyPolicy.isSensitive(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL))
    }

    @Test fun respectsNoPersonalizedLearningFlag() {
        val normal = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        assertTrue(InputPrivacyPolicy.isPrivateSession(normal, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING))
        assertFalse(InputPrivacyPolicy.isPrivateSession(normal, 0))
    }
}
