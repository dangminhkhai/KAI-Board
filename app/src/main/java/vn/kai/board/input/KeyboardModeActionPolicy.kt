package vn.kai.board.input

object KeyboardModeActionPolicy {
    fun bottomRightAction(aiMode: Boolean): KeyAction =
        if (aiMode) KeyAction.SendAi else KeyAction.Enter
}
