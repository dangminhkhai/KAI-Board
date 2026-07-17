package vn.kai.board.translation

data class TranslationLanguage(val tag: String, val name: String)

object TranslationLanguages {
    val all = listOf(
        "vi" to "Tiếng Việt", "en" to "Tiếng Anh", "zh" to "Tiếng Trung", "ja" to "Tiếng Nhật",
        "ko" to "Tiếng Hàn", "fr" to "Tiếng Pháp", "de" to "Tiếng Đức", "es" to "Tiếng Tây Ban Nha",
        "it" to "Tiếng Ý", "pt" to "Tiếng Bồ Đào Nha", "ru" to "Tiếng Nga", "uk" to "Tiếng Ukraina",
        "th" to "Tiếng Thái", "id" to "Tiếng Indonesia", "ms" to "Tiếng Mã Lai", "tl" to "Tiếng Filipino",
        "ar" to "Tiếng Ả Rập", "hi" to "Tiếng Hindi", "bn" to "Tiếng Bengal", "ur" to "Tiếng Urdu",
        "nl" to "Tiếng Hà Lan", "pl" to "Tiếng Ba Lan", "tr" to "Tiếng Thổ Nhĩ Kỳ", "sv" to "Tiếng Thụy Điển",
        "no" to "Tiếng Na Uy", "da" to "Tiếng Đan Mạch", "fi" to "Tiếng Phần Lan", "cs" to "Tiếng Séc",
        "ro" to "Tiếng Romania", "hu" to "Tiếng Hungary", "el" to "Tiếng Hy Lạp", "he" to "Tiếng Do Thái",
        "af" to "Tiếng Afrikaans", "be" to "Tiếng Belarus", "bg" to "Tiếng Bulgaria", "ca" to "Tiếng Catalan",
        "cy" to "Tiếng Wales", "eo" to "Tiếng Esperanto", "et" to "Tiếng Estonia", "fa" to "Tiếng Ba Tư",
        "ga" to "Tiếng Ireland", "gl" to "Tiếng Galicia", "gu" to "Tiếng Gujarat", "hr" to "Tiếng Croatia",
        "ht" to "Tiếng Haiti", "is" to "Tiếng Iceland", "ka" to "Tiếng Georgia", "kn" to "Tiếng Kannada",
        "lt" to "Tiếng Litva", "lv" to "Tiếng Latvia", "mk" to "Tiếng Macedonia", "mr" to "Tiếng Marathi",
        "mt" to "Tiếng Malta", "sk" to "Tiếng Slovakia", "sl" to "Tiếng Slovenia", "sq" to "Tiếng Albania",
        "sw" to "Tiếng Swahili", "ta" to "Tiếng Tamil", "te" to "Tiếng Telugu",
    ).map { TranslationLanguage(it.first, it.second) }
}
