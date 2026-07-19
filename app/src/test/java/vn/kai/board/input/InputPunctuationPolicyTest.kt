package vn.kai.board.input

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Test

class InputPunctuationPolicyTest {
    @Test fun emailUsesAtSign() {
        assertEquals('@', InputPunctuationPolicy.leadingKey(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        assertEquals('@', InputPunctuationPolicy.leadingKey(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS))
    }

    @Test fun urlUsesSlash() {
        assertEquals('/', InputPunctuationPolicy.leadingKey(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
    }

    @Test fun regularTextKeepsComma() {
        assertEquals(',', InputPunctuationPolicy.leadingKey(InputType.TYPE_CLASS_TEXT))
    }
}
