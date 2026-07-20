package vn.kai.board.input

object SuggestionPriority {
    fun merge(
        ai: List<String>,
        personal: List<String>,
        offline: List<String>,
        limit: Int = 3,
    ): List<String> = (ai + personal + offline)
        .distinctBy { it.lowercase() }
        .take(limit.coerceAtLeast(0))
}
