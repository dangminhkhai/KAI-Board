package vn.kai.board.input

object EmailSuggestionEngine {
    private val commonDomains = listOf("gmail.com", "outlook.com", "yahoo.com")

    fun currentToken(textBeforeCursor: CharSequence): String = textBeforeCursor
        .takeLastWhile { !it.isWhitespace() && it !in ",;<>" }
        .toString()

    fun suggest(token: String, savedEmails: List<String>, limit: Int = 3): List<String> {
        val query = token.trim().lowercase()
        if (query.isEmpty() || limit <= 0 || query.length > 96) return emptyList()
        val saved = savedEmails.asSequence()
            .filter { it.lowercase().startsWith(query) }
        val domains = if ('@' in query) {
            val local = query.substringBefore('@')
            val domainPrefix = query.substringAfter('@')
            commonDomains.asSequence()
                .filter { it.startsWith(domainPrefix) }
                .map { "$local@$it" }
        } else emptySequence()
        return (saved + domains).distinct().take(limit).toList()
    }

    fun isCompleteEmail(value: String): Boolean {
        val token = value.trim()
        val at = token.indexOf('@')
        return at > 0 && at == token.lastIndexOf('@') && token.substring(at + 1).contains('.') &&
            token.none(Char::isWhitespace)
    }
}
