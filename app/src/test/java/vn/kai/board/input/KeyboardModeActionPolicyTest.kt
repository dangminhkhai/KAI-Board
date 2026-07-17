package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardModeActionPolicyTest {
    @Test fun enterSendsAiWhileAiModeIsOpen() {
        assertEquals(KeyAction.SendAi, KeyboardModeActionPolicy.bottomRightAction(aiMode = true))
    }

    @Test fun enterKeepsNormalBehaviorOutsideAiMode() {
        assertEquals(KeyAction.Enter, KeyboardModeActionPolicy.bottomRightAction(aiMode = false))
    }
}
