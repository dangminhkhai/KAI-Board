package vn.kai.board.ai

import java.text.Normalizer
import java.util.Locale

data class AiPromptEdit(val prompt: String, val cursor: Int)

/** Fast, offline command completion for the AI prompt editor. */
object AiCommandSuggestionEngine {
    private const val FIELD = "\u001E"
    private val tokenRegex = Regex("[\\p{L}\\p{N}._+-]+")

    fun suggest(
        prompt: String,
        cursor: Int = prompt.length,
        learned: Map<String, Int> = emptyMap(),
        limit: Int = 3,
    ): List<String> {
        if (limit <= 0) return emptyList()
        val before = prompt.substring(0, cursor.coerceIn(0, prompt.length))
        val words = tokens(before)
        val trailingSpace = before.lastOrNull()?.isWhitespace() == true
        var partial = if (trailingSpace) "" else words.lastOrNull().orEmpty()
        var context = if (partial.isEmpty()) words else words.dropLast(1)
        val transitions = learned

        // An exact command word such as "Viết" is more useful as context than
        // as a completed partial, so immediately offer what normally follows it.
        if (partial.isNotEmpty() && candidatesFor(transitions, context + partial).isNotEmpty()) {
            context = context + partial
            partial = ""
        }

        val ranked = LinkedHashMap<String, Int>()
        if (partial.isNotEmpty()) {
            val needle = normalize(partial)
            transitions.forEach { (key, weight) ->
                val candidate = decode(key).second
                if (normalize(candidate).startsWith(needle) && !candidate.equals(partial, true)) {
                    ranked[candidate] = maxOf(ranked[candidate] ?: Int.MIN_VALUE, 50_000 + weight * 100)
                }
            }
        }
        for (contextSize in minOf(2, context.size) downTo 0) {
            val suffix = context.takeLast(contextSize)
            candidatesFor(transitions, suffix).forEach { (candidate, weight) ->
                // A longer matching context must always beat a globally frequent
                // command starter. Frequency only ranks candidates at the same depth.
                val score = contextSize * 100_000 + weight * 1_000 + if (' ' in candidate) 10 else 0
                ranked[candidate] = maxOf(ranked[candidate] ?: Int.MIN_VALUE, score)
            }
        }
        return ranked.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.length })
            .map { matchCase(it.key, partial.ifEmpty { context.lastOrNull().orEmpty() }) }
            .distinctBy(::normalize)
            .take(limit)
    }

    fun applySuggestion(prompt: String, cursor: Int, suggestion: String): AiPromptEdit {
        val safeCursor = cursor.coerceIn(0, prompt.length)
        val before = prompt.substring(0, safeCursor)
        val after = prompt.substring(safeCursor)
        val match = tokenRegex.findAll(before).lastOrNull()
        val partial = match?.takeIf { it.range.last == before.lastIndex && before.lastOrNull()?.isWhitespace() != true }
        val completesPartial = partial != null && normalize(suggestion).startsWith(normalize(partial.value))
        val prefix = if (completesPartial) before.substring(0, partial!!.range.first) else before
        val separator = if (prefix.isEmpty() || prefix.last().isWhitespace()) "" else " "
        val suffix = after.trimStart()
        val inserted = prefix + separator + suggestion.trim() + if (suffix.isEmpty()) " " else ""
        val result = inserted + suffix
        return AiPromptEdit(result.take(2_000), inserted.length.coerceAtMost(2_000))
    }

    fun learnedTransitions(prompt: String): Map<String, Int> = buildTransitions(listOf(prompt), baseWeight = 1)

    internal fun encode(context: List<String>, candidate: String): String =
        context.joinToString(" ") + FIELD + candidate

    private fun candidatesFor(transitions: Map<String, Int>, context: List<String>): List<Pair<String, Int>> {
        val normalizedContext = context.joinToString(" ") { normalize(it) }
        return transitions.mapNotNull { (key, weight) ->
            val (storedContext, candidate) = decode(key)
            if (storedContext == normalizedContext) candidate to weight else null
        }
    }

    private fun buildTransitions(commands: List<String>, baseWeight: Int): Map<String, Int> {
        val result = LinkedHashMap<String, Int>()
        commands.forEach { command ->
            val words = tokens(command)
            words.indices.forEach { index ->
                for (contextSize in 0..minOf(2, index)) {
                    val context = words.subList(index - contextSize, index).map(::normalize)
                    for (candidateSize in 1..minOf(2, words.size - index)) {
                        val candidate = words.subList(index, index + candidateSize).joinToString(" ")
                        val key = encode(context, candidate)
                        result[key] = (result[key] ?: 0) + baseWeight
                    }
                }
            }
        }
        return result
    }

    private fun decode(key: String): Pair<String, String> {
        val split = key.lastIndexOf(FIELD)
        return if (split < 0) "" to key else key.substring(0, split) to key.substring(split + 1)
    }

    private fun tokens(value: String): List<String> = tokenRegex.findAll(value).map { it.value }.toList()

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase(Locale.ROOT)

    private fun matchCase(value: String, reference: String): String = when {
        reference.firstOrNull()?.isUpperCase() == true -> value.replaceFirstChar { it.titlecase(Locale.ROOT) }
        else -> value
    }
}
