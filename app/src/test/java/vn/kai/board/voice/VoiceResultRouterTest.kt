package vn.kai.board.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceResultRouterTest {
    @Test fun routesTranslationTranscript() {
        assertEquals(
            VoiceResult(VoiceTarget.TRANSLATION, "xin chào"),
            VoiceResultRouter.prepare("TRANSLATION", "  xin chào  "),
        )
    }

    @Test fun routesAiCommandWithoutSendingIt() {
        assertEquals(
            VoiceResult(VoiceTarget.AI_COMMAND, "viết lại lịch sự"),
            VoiceResultRouter.prepare("AI_COMMAND", "viết lại lịch sự"),
        )
    }

    @Test fun rejectsBlankTranscriptAndDefaultsUnknownTargetToNormal() {
        assertNull(VoiceResultRouter.prepare("AI_COMMAND", "   "))
        assertEquals(
            VoiceResult(VoiceTarget.NORMAL, "hello"),
            VoiceResultRouter.prepare("UNKNOWN", "hello"),
        )
    }

    @Test fun limitsTranscriptByDestination() {
        assertEquals(500, VoiceResultRouter.prepare("TRANSLATION", "x".repeat(800))?.text?.length)
    }
}
