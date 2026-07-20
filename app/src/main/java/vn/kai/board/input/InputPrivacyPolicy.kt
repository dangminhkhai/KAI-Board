package vn.kai.board.input

import android.text.InputType
import android.view.inputmethod.EditorInfo

object InputPrivacyPolicy {
    fun isPrivateSession(info: EditorInfo?): Boolean = info != null && isPrivateSession(info.inputType, info.imeOptions)

    fun isPrivateSession(inputType: Int, imeOptions: Int): Boolean =
        isSensitive(inputType) || imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0

    fun isSensitive(inputType: Int): Boolean {
        return when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> when (inputType and InputType.TYPE_MASK_VARIATION) {
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> true
                else -> false
            }
            InputType.TYPE_CLASS_NUMBER ->
                inputType and InputType.TYPE_MASK_VARIATION == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }
}
