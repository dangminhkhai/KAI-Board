package vn.kai.board.voice

object VoiceLanguageResolver {
    private val speechLocales = mapOf(
        "vi" to "vi-VN",
        "en" to "en-US",
        "zh" to "zh-CN",
        "ja" to "ja-JP",
        "ko" to "ko-KR",
        "fr" to "fr-FR",
        "de" to "de-DE",
        "es" to "es-ES",
        "it" to "it-IT",
        "pt" to "pt-PT",
        "ru" to "ru-RU",
        "uk" to "uk-UA",
        "th" to "th-TH",
        "id" to "id-ID",
        "ms" to "ms-MY",
        "tl" to "fil-PH",
        "ar" to "ar-SA",
        "hi" to "hi-IN",
    )

    fun resolve(languageTag: String): String {
        val normalized = languageTag.trim()
        if ('-' in normalized) return normalized
        return speechLocales[normalized.lowercase()] ?: normalized
    }
}
