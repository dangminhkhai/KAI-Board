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

    /**
     * Mid-word blend: context phrase hits first (e.g. after "xin", typing "c" → "chào"),
     * then dictionary / lexicon completions.
     */
    fun mergePhraseAndCompletions(
        phrasePrefixHits: List<String>,
        wordCompletions: List<String>,
        limit: Int = 3,
    ): List<String> = (phrasePrefixHits + wordCompletions)
        .distinctBy { it.lowercase() }
        .take(limit.coerceAtLeast(0))
}
