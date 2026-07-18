package vn.kai.board.input

sealed interface KeyAction {
    data class Character(val value: Char) : KeyAction
    data class CommitText(val value: String) : KeyAction
    data class SelectSuggestion(val value: String) : KeyAction
    data class SelectAiSuggestion(val value: String) : KeyAction
    data class ForgetSuggestion(val value: String) : KeyAction
    data object Backspace : KeyAction
    data object Shift : KeyAction
    data object CapsLock : KeyAction
    data object Space : KeyAction
    data class MoveCursor(val characters: Int) : KeyAction
    data object Enter : KeyAction
    data object OpenSettings : KeyAction
    data object VoiceInput : KeyAction
    data object VoiceTranslation : KeyAction
    data object VoiceAi : KeyAction
    data object CancelVoice : KeyAction
    data object ToggleVoicePause : KeyAction
    data object OpenTranslator : KeyAction
    data object CloseTranslator : KeyAction
    data object CycleTranslationSource : KeyAction
    data object CycleTranslationTarget : KeyAction
    data object SwapTranslationLanguages : KeyAction
    data object OpenAi : KeyAction
    data object CloseAi : KeyAction
    data object SendAi : KeyAction
    data class SetAiCursor(val index: Int) : KeyAction
    data object ToggleEmoji : KeyAction
    data object ToggleClipboard : KeyAction
    data class SelectEmojiGroup(val index: Int) : KeyAction
    data object ToggleEmojiSearch : KeyAction
    data class EmojiSearchCharacter(val value: Char) : KeyAction
    data object EmojiSearchBackspace : KeyAction
    data class SelectClipboardTab(val index: Int) : KeyAction
    data object OpenClipboardManager : KeyAction
    data object HideKeyboard : KeyAction
    data object ToggleSymbols : KeyAction
    data object ToggleSymbolPage : KeyAction
    data object MoveKeyboardUp : KeyAction
    data object MoveKeyboardDown : KeyAction
    data object DecreaseKeyboardHeight : KeyAction
    data object IncreaseKeyboardHeight : KeyAction
    data object FinishKeyboardAdjustment : KeyAction
}
