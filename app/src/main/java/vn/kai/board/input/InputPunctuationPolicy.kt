package vn.kai.board.input

import android.text.InputType

object InputPunctuationPolicy {
    fun leadingKey(inputType: Int): Char {
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return ','
        return when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> '@'
            InputType.TYPE_TEXT_VARIATION_URI -> '/'
            else -> ','
        }
    }
}
