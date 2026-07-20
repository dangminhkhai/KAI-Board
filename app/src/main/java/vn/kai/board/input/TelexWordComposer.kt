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
        val restoredLatin = restoreInvalidSyllable && shouldRestoreRaw(raw, transformed)
        val text = if (restoredLatin) raw else transformed
        val lock = when {
            escapedModifier -> text.length
            restoredLatin -> firstTelexTransformLength(raw)
            else -> 0
        }
        return TelexComposeResult(text, lock, raw)
    }

    fun lockAfterBackspace(newLength: Int, literalLockLength: Int): Int =
        literalLockLength.takeIf { it > 0 && newLength >= it } ?: 0

    fun backspace(word: String, rawWord: String, literalLockLength: Int): TelexComposeResult {
        if (word.isEmpty() && rawWord.isEmpty()) return TelexComposeResult("", 0, "")

        // Marked Vietnamese (display ≠ raw), no lock: delete one *visible* char.
        // Mí -> M, Gô -> G, Sà -> S — never strip only the raw tone key.
        if (word.isNotEmpty() && word != rawWord && literalLockLength == 0 && word.any(::isVietnameseMarked)) {
            val visible = word.dropLast(1)
            return TelexComposeResult(visible, 0, visible)
        }

        // Always remove one visible character when the buffer is Latin (including locked
        // shape/tone escapes where raw can be longer than display: aaa→aa, lock=2).
        if (word.isEmpty()) {
            val raw = rawWord.dropLast(1)
            return if (raw.isEmpty()) TelexComposeResult("", 0, "") else compose(raw)
        }

        val newWord = word.dropLast(1)
        var newRaw = rawWord.dropLast(1)
        // Shape escape keeps extra raw keys (aaa displayed as aa). Keep raw in sync
        // with the shorter visible string so the next BS deletes again.
        if (newRaw.length > newWord.length) {
            newRaw = newRaw.take(newWord.length)
        }
        if (newWord.isEmpty()) return TelexComposeResult("", 0, "")

        val lock = lockAfterBackspace(newWord.length, literalLockLength)
        if (lock > 0) {
            return TelexComposeResult(newWord, lock, newRaw.ifEmpty { newWord })
        }

        // Fully Latin remainder: never recompose through Telex (Saf must not become Sà;
        // aa/ee/oo/aw/ow/uw/dd tails must not reshape).
        val raw = newRaw.ifEmpty { newWord }
        val wasLatinBuffer = !newWord.any(::isVietnameseMarked)
        if (wasLatinBuffer) {
            val recomposed = compose(raw)
            if (recomposed.text != raw && recomposed.text != newWord) {
                val relock = firstTelexTransformLength(raw).coerceIn(1, raw.length)
                return TelexComposeResult(newWord, relock, raw)
            }
            // Prefer the visible Latin drop when compose would re-mark.
            if (recomposed.text != newWord && newWord == raw) {
                val relock = firstTelexTransformLength(newWord).coerceIn(1, newWord.length)
                return TelexComposeResult(newWord, relock, newWord)
            }
            if (recomposed.text == newWord || newWord == raw) {
                return if (recomposed.text != newWord) {
                    TelexComposeResult(newWord, firstTelexTransformLength(newWord).coerceIn(1, newWord.length), newWord)
                } else {
                    recomposed.copy(rawText = raw)
                }
            }
            return TelexComposeResult(newWord, 0, raw)
        }
        return compose(raw)
    }

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

    private fun firstTelexTransformLength(raw: String): Int {
        for (index in raw.indices) {
            val prefix = raw.substring(0, index)
            if (TelexEngine.apply(prefix, raw[index]) != null) return index + 1
        }
        return raw.length.coerceAtLeast(1)
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
