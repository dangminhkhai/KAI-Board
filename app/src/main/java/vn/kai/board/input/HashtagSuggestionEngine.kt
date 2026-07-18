package vn.kai.board.input

object HashtagSuggestionEngine {
    private val completePattern = Regex("#[\\p{L}\\p{N}_]{2,64}")

    fun currentToken(textBeforeCursor: CharSequence): String = textBeforeCursor
        .takeLastWhile { it == '#' || it == '_' || it.isLetterOrDigit() }
        .toString()
        .takeIf { it.startsWith('#') }
        .orEmpty()

    fun suggest(token: String, learned: List<String>, limit: Int = 3): List<String> {
        val query = token.lowercase()
        if (!query.startsWith('#') || limit <= 0) return emptyList()
        return learned.asSequence()
            .filter { it.lowercase().startsWith(query) }
            .distinctBy { it.lowercase() }
            .take(limit)
            .toList()
    }

    fun isComplete(value: String): Boolean = completePattern.matches(value.trim())
}
