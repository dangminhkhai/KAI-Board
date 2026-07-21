package vn.kai.board.ai

import java.text.Normalizer
import java.util.Locale

data class AiPromptEdit(val prompt: String, val cursor: Int)

/**
 * Fast, offline command completion for the AI prompt editor.
 *
 * Suggestions are always the **next single word** (Gboard-style), never a multi-word
 * blob like "đề ngắn". After the user picks "đề", the next offer can be "ngắn".
 */
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
        val completingPartial = partial.isNotEmpty()
        if (completingPartial) {
            val needle = normalize(partial)
            transitions.forEach { (key, weight) ->
                val candidate = decode(key).second
                // Only complete the current word — never expand into a multi-word phrase.
                if (!isSingleWord(candidate)) return@forEach
                if (normalize(candidate).startsWith(needle) && !candidate.equals(partial, true)) {
                    ranked[candidate] = maxOf(ranked[candidate] ?: Int.MIN_VALUE, 50_000 + weight * 100)
                }
            }
        }
        // Next-word from real context first. Do NOT mix empty-context "command starters"
        // (e.g. Limo) when we already have a word like "Tiêu" — that polluted the bar.
        val maxCtx = minOf(2, context.size)
        if (maxCtx > 0) {
            for (contextSize in maxCtx downTo 1) {
                val suffix = context.takeLast(contextSize)
                candidatesFor(transitions, suffix).forEach { (candidate, weight) ->
                    if (!isSingleWord(candidate)) return@forEach
                    // Skip echoing the last context token itself.
                    if (context.lastOrNull()?.let { it.equals(candidate, ignoreCase = true) } == true) return@forEach
                    val score = contextSize * 100_000 + weight * 1_000
                    ranked[candidate] = maxOf(ranked[candidate] ?: Int.MIN_VALUE, score)
                }
            }
        }
        // Global starters only when the prompt has no context yet (or no contextual hits).
        if (maxCtx == 0 || ranked.isEmpty()) {
            candidatesFor(transitions, emptyList()).forEach { (candidate, weight) ->
                if (!isSingleWord(candidate)) return@forEach
                if (partial.isNotEmpty() && candidate.equals(partial, ignoreCase = true)) return@forEach
                val score = weight * 1_000
                ranked[candidate] = maxOf(ranked[candidate] ?: Int.MIN_VALUE, score)
            }
        }
        return ranked.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.length })
            // Next-word keeps learned casing (đề not Đề). Only partial-completions follow typed case.
            .map { if (completingPartial) matchCase(it.key, partial) else it.key }
            .distinctBy(::normalize)
            .filterNot { partial.isNotEmpty() && it.equals(partial, ignoreCase = true) }
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
                // Next-word only (size 1). Multi-word candidates made "Tiêu" suggest "đề ngắn".
                val candidate = words[index]
                for (contextSize in 0..minOf(2, index)) {
                    val context = words.subList(index - contextSize, index).map(::normalize)
                    val key = encode(context, candidate)
                    result[key] = (result[key] ?: 0) + baseWeight
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

    /** True for one token only — rejects legacy multi-word blobs already on disk. */
    private fun isSingleWord(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isNotEmpty() && ' ' !in trimmed && '\t' !in trimmed
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase(Locale.ROOT)

    private fun matchCase(value: String, reference: String): String = when {
        reference.firstOrNull()?.isUpperCase() == true -> value.replaceFirstChar { it.titlecase(Locale.ROOT) }
        else -> value
    }
}
