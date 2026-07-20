package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Test

class SmartClipboardClassifierTest {
    @Test fun extractsOtpOnlyWithSecurityHint() {
        val result = SmartClipboardClassifier.classify("Mã OTP của bạn là 482913. Không chia sẻ.")
        assertEquals(ClipboardContentKind.OTP, result.kind)
        assertEquals("482913", result.pasteText)
    }

    @Test fun extractsEmailUrlAndVietnamesePhone() {
        assertEquals("kai@example.com", SmartClipboardClassifier.classify("Email: kai@example.com").pasteText)
        assertEquals("https://kai.vn/a", SmartClipboardClassifier.classify("Mở https://kai.vn/a.").pasteText)
        assertEquals("090 123 4567", SmartClipboardClassifier.classify("Gọi 090 123 4567 nhé").pasteText)
    }

    @Test fun ordinaryTextIsPreserved() {
        val text = "Hẹn lúc 123456"
        assertEquals(SmartClipboardContent(text, text, ClipboardContentKind.TEXT), SmartClipboardClassifier.classify(text))
    }
}
