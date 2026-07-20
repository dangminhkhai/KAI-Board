package vn.kai.board.input

import android.text.InputType
import android.view.inputmethod.EditorInfo

object TelexInputPolicy {
    fun isEnabled(inputType: Int): Boolean {
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val hasTextOnlyFlags = inputType and UNAMBIGUOUS_TEXT_FLAGS != 0
        // Some custom editors (Shopee search is one example) restart the IME with only a
        // TYPE_TEXT_FLAG_* value and accidentally omit TYPE_CLASS_TEXT. Treat that malformed
        // value as text, while keeping null, numeric, phone and datetime editors excluded.
        if (inputClass != InputType.TYPE_CLASS_TEXT && !(inputClass == InputType.TYPE_NULL && hasTextOnlyFlags)) {
            return false
        }
        return when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> false
            else -> true
        }
    }

    /** Some custom search fields discard composing spans but still accept direct text replacement. */
    fun requiresDirectCommit(inputType: Int, imeOptions: Int): Boolean {
        if (!isEnabled(inputType)) return false
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        return inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0 ||
            action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_GO
    }

    // CAP_CHARACTERS/CAP_WORDS are excluded because their bits overlap the numeric
    // SIGNED/DECIMAL flags when a broken editor omits its class bits.
    private const val UNAMBIGUOUS_TEXT_FLAGS =
        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
            InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or
            InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
}
