package vn.kai.board.input

enum class SuggestionLanguage { VIETNAMESE, ENGLISH, UNKNOWN }

object SuggestionLanguageDetector {
    private val vietnameseMarks = Regex("[ăâđêôơưáàảãạấầẩẫậắằẳẵặéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ]", RegexOption.IGNORE_CASE)
    private val englishHints = listOf("w", "f", "j", "z", "sh", "ing", "tion", "app", "hel", "you", "thank", "the", "this", "with")

    fun detect(current: String, previous: String? = null): SuggestionLanguage {
        val context = previous.orEmpty() + " " + current
        if (vietnameseMarks.containsMatchIn(context)) return SuggestionLanguage.VIETNAMESE
        val lower = context.lowercase()
        if (englishHints.any(lower::contains)) return SuggestionLanguage.ENGLISH
        return SuggestionLanguage.UNKNOWN
    }
}
