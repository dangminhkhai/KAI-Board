package vn.kai.board.input

import vn.kai.board.telex.TelexEngine

data class TelexComposeResult(
    val text: String,
    val literalLockLength: Int,
    val rawText: String = text,
)

/** Keeps the rest of a word literal after the user explicitly escapes a tone. */
object TelexWordComposer {
    fun removeLast(word: String): String = word.dropLast(1)

    fun append(word: String, key: Char, literalLockLength: Int): TelexComposeResult {
        return append(word, key, literalLockLength, word, restoreInvalidSyllable = false)
    }

    fun append(word: String, key: Char, literalLockLength: Int, rawWord: String): TelexComposeResult =
        append(word, key, literalLockLength, rawWord, restoreInvalidSyllable = true)

    fun compose(rawWord: String): TelexComposeResult {
        var state = TelexComposeResult("", 0, "")
        rawWord.forEach { key ->
            state = append(state.text, key, state.literalLockLength, state.rawText)
        }
        return state
    }

    private fun append(
        word: String,
        key: Char,
        literalLockLength: Int,
        rawWord: String,
        restoreInvalidSyllable: Boolean,
    ): TelexComposeResult {
        val raw = "$rawWord$key"
        if (literalLockLength > 0) return TelexComposeResult("$word$key", literalLockLength, raw)
        if (restoreInvalidSyllable && word == rawWord && hasLiteralSplitVowels(rawWord)) {
            return TelexComposeResult(raw, 0, raw)
        }
        val escapedModifier = TelexEngine.isRepeatedModifierEscape(word, key)
        val transformed = TelexEngine.apply(word, key) ?: "$word$key"
        val text = if (restoreInvalidSyllable && shouldRestoreRaw(raw, transformed)) raw else transformed
        return TelexComposeResult(text, if (escapedModifier) text.length else 0, raw)
    }

    fun lockAfterBackspace(newLength: Int, literalLockLength: Int): Int =
        literalLockLength.takeIf { it > 0 && newLength >= it } ?: 0

    private fun shouldRestoreRaw(raw: String, rendered: String): Boolean {
        if (raw.equals(rendered, ignoreCase = false) || rendered.none(::isVietnameseMarked)) return false
        val lower = rendered.lowercase()
        val firstVowel = lower.indexOfFirst(::isVowel)
        if (firstVowel < 0) return false

        var nucleusEnd = firstVowel
        while (nucleusEnd + 1 < lower.length && isVowel(lower[nucleusEnd + 1])) nucleusEnd++
        val laterVowel = (nucleusEnd + 1 until lower.length).any { isVowel(lower[it]) }
        if (laterVowel) return true

        val coda = lower.substring(nucleusEnd + 1)
        if (coda !in setOf("", "c", "ch", "m", "n", "ng", "nh", "p", "t")) return true

        val nucleus = lower.substring(firstVowel, nucleusEnd + 1)
        val bases = nucleus.map(::vowelBase).joinToString("")
        if (bases in setOf("ae", "ue") && nucleus.none { it in "ăâêôơư" }) return true
        if (nucleus in setOf("ăe", "âe", "ôe", "ơe", "ưe")) return true

        // Two different tone letters inside a raw word before another vowel
        // are overwhelmingly an English consonant run: cursor, parser, etc.
        val firstRawVowel = raw.indexOfFirst(::isVowel)
        if (firstRawVowel >= 0) {
            var toneLetters = 0
            for (index in firstRawVowel + 1 until raw.length) {
                val char = raw[index].lowercaseChar()
                if (char in "sfrxj") toneLetters++
                if (isVowel(char) && toneLetters >= 2) return true
            }
        }
        return false
    }

    private fun isVietnameseMarked(char: Char): Boolean =
        char.lowercaseChar() == 'đ' || (isVowel(char) && char.lowercaseChar() != vowelBase(char))

    private fun isVowel(char: Char): Boolean = vowelBase(char) in "aeiouy"

    private fun hasLiteralSplitVowels(word: String): Boolean {
        val firstVowel = word.indexOfFirst(::isVowel)
        if (firstVowel < 0) return false
        var sawConsonant = false
        for (index in firstVowel + 1 until word.length) {
            if (isVowel(word[index])) {
                if (sawConsonant) return true
            } else {
                sawConsonant = true
            }
        }
        return false
    }

    private fun vowelBase(char: Char): Char = when (char.lowercaseChar()) {
        in "aáàảãạăắằẳẵặâấầẩẫậ" -> 'a'
        in "eéèẻẽẹêếềểễệ" -> 'e'
        in "iíìỉĩị" -> 'i'
        in "oóòỏõọôốồổỗộơớờởỡợ" -> 'o'
        in "uúùủũụưứừửữự" -> 'u'
        in "yýỳỷỹỵ" -> 'y'
        else -> char.lowercaseChar()
    }
}
