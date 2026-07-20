package vn.kai.board.input

import android.text.InputType

data class NumericInputMode(
    val enabled: Boolean,
    val decimal: Boolean = false,
    val signed: Boolean = false,
    val phone: Boolean = false,
)

object NumericInputPolicy {
    fun resolve(inputType: Int): NumericInputMode {
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val number = inputClass == InputType.TYPE_CLASS_NUMBER
        val phone = inputClass == InputType.TYPE_CLASS_PHONE
        return NumericInputMode(
            enabled = number || phone,
            decimal = number && inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0,
            signed = number && inputType and InputType.TYPE_NUMBER_FLAG_SIGNED != 0,
            phone = phone,
        )
    }
}
