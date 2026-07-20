package vn.kai.board.input

enum class ClipboardContentKind(val label: String) { OTP("OTP"), EMAIL("EMAIL"), URL("URL"), PHONE("SĐT"), TEXT("") }

data class SmartClipboardContent(val sourceText: String, val pasteText: String, val kind: ClipboardContentKind)

object SmartClipboardClassifier {
    private val otpHint = Regex("(?i)\\b(otp|mã|code|verification|xác thực|xac thuc)\\b")
    private val otp = Regex("(?<!\\d)\\d{4,8}(?!\\d)")
    private val email = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val url = Regex("(?i)\\b(?:https?://|www\\.)[^\\s]+")
    private val phone = Regex("(?<!\\d)(?:\\+?84|0)(?:[ .-]?\\d){8,10}(?!\\d)")

    fun classify(raw: String): SmartClipboardContent {
        val text = raw.trim()
        if (otpHint.containsMatchIn(text)) otp.find(text)?.value?.let { return SmartClipboardContent(text, it, ClipboardContentKind.OTP) }
        email.find(text)?.value?.let { return SmartClipboardContent(text, it, ClipboardContentKind.EMAIL) }
        url.find(text)?.value?.trimEnd('.', ',', ';', ')')?.let { return SmartClipboardContent(text, it, ClipboardContentKind.URL) }
        phone.find(text)?.value?.let { return SmartClipboardContent(text, it, ClipboardContentKind.PHONE) }
        return SmartClipboardContent(text, text, ClipboardContentKind.TEXT)
    }
}
