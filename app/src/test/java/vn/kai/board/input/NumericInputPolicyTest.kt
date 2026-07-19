package vn.kai.board.input

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Test

class NumericInputPolicyTest {
    @Test fun numberAndOtpUseNumericPad() {
        assertEquals(NumericInputMode(enabled = true), NumericInputPolicy.resolve(InputType.TYPE_CLASS_NUMBER))
        assertEquals(
            NumericInputMode(enabled = true),
            NumericInputPolicy.resolve(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD),
        )
    }

    @Test fun decimalSignedAndPhoneFlagsArePreserved() {
        assertEquals(
            NumericInputMode(enabled = true, decimal = true, signed = true),
            NumericInputPolicy.resolve(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED),
        )
        assertEquals(NumericInputMode(enabled = true, phone = true), NumericInputPolicy.resolve(InputType.TYPE_CLASS_PHONE))
    }

    @Test fun textDoesNotUseNumericPad() {
        assertEquals(NumericInputMode(enabled = false), NumericInputPolicy.resolve(InputType.TYPE_CLASS_TEXT))
    }
}
