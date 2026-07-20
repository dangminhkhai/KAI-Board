package vn.kai.board.input

import vn.kai.board.telex.TelexEngine

data class TelexComposeResult(
    val text: String,
    val literalLockLength: Int,
)

/** Keeps the rest of a word literal after the user explicitly escapes a tone. */
object TelexWordComposer {
    fun removeLast(word: String): String = word.dropLast(1)

    fun append(word: String, key: Char, literalLockLength: Int): TelexComposeResult {
        if (literalLockLength > 0) return TelexComposeResult("$word$key", literalLockLength)
        val escapedModifier = TelexEngine.isRepeatedModifierEscape(word, key)
        val text = TelexEngine.apply(word, key) ?: "$word$key"
        return TelexComposeResult(text, if (escapedModifier) text.length else 0)
    }

    fun lockAfterBackspace(newLength: Int, literalLockLength: Int): Int =
        literalLockLength.takeIf { it > 0 && newLength >= it } ?: 0
}
