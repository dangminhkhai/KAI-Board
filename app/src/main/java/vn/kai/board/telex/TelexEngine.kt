package vn.kai.board.telex

/**
 * Stateless Telex transformer for the Vietnamese word before the cursor.
 *
 * Tone placement uses **kiểu mới** (modern):
 * - `gi` / `qu` are digraph consonants (not tone carriers) when followed by a vowel → `gias` → `giá`
 * - Marked vowels ăâêôơư take tone first
 * - Open diphthongs oa/oe/uy: tone on first (òa, òe, ùy)
 * - Open ia/ya/ua/ưa: tone on first (ía, úa, ứa)
 * - Closed syllables: tone on last vowel of the nucleus
 */
object TelexEngine {
    private val vietnameseOnsets = setOf(
        "", "b", "c", "ch", "d", "đ", "g", "gh", "gi", "h", "k", "kh", "l",
        "m", "n", "ng", "ngh", "nh", "p", "ph", "q", "qu", "r", "s", "t",
        "th", "tr", "v", "x"
    )
    private val toneKeys = "zsf r xj".filterNot(Char::isWhitespace) // z=none, sắc huyền hỏi ngã nặng
    private val families = mapOf(
        'a' to "aáàảãạ", 'ă' to "ăắằẳẵặ", 'â' to "âấầẩẫậ",
        'e' to "eéèẻẽẹ", 'ê' to "êếềểễệ", 'i' to "iíìỉĩị",
        'o' to "oóòỏõọ", 'ô' to "ôốồổỗộ", 'ơ' to "ơớờởỡợ",
        'u' to "uúùủũụ", 'ư' to "ưứừửữự", 'y' to "yýỳỷỹỵ"
    )
    private val familyByChar = families.flatMap { (shape, chars) -> chars.map { it to shape } }.toMap()
    private val markedShapes = setOf('ă', 'â', 'ê', 'ô', 'ơ', 'ư')

    fun apply(word: String, key: Char): String? {
        if (word.isEmpty()) return null
        val lower = key.lowercaseChar()
        repairAdjacentDTypo(word, lower)?.let { return it }
        restoreRawToneBeforeLiteral(word, lower)?.let { return it }
        restoreRawShapeBeforeLiteralZ(word, lower)?.let { return it }
        if (lower in "sfrxjz") return applyTone(word, lower)
        return applyShape(word, lower)
    }

    /** True when [key] explicitly undoes a tone, shape, horn/breve, or đ. */
    fun isRepeatedModifierEscape(word: String, key: Char): Boolean {
        val lower = key.lowercaseChar()
        val tone = toneKeys.indexOf(lower)
        if (tone > 0 && word.any { toneIndex(it) == tone }) return true
        return when (lower) {
            'a' -> word.any { baseShape(it) == 'â' }
            'e' -> word.any { baseShape(it) == 'ê' }
            'o' -> word.any { baseShape(it) == 'ô' }
            'w' -> word.any { baseShape(it) in setOf('ă', 'ơ', 'ư') }
            'd' -> word.lastOrNull()?.lowercaseChar() == 'đ'
            else -> false
        }
    }

    /**
     * On QWERTY, S is directly beside D, so the intended `dd...` for đ is
     * sometimes entered as `ds...`. Wait for a following vowel before repairing
     * it, keeping a standalone `ds` abbreviation literal.
     * Example: dsangwj -> đa... -> đặng.
     */
    private fun repairAdjacentDTypo(word: String, key: Char): String? {
        if (!isVowel(key) || word.length != 2) return null
        if (!word.equals("ds", ignoreCase = true)) return null
        val outD = if (word.first().isUpperCase()) 'Đ' else 'đ'
        return "$outD$key"
    }

    private fun restoreRawShapeBeforeLiteralZ(word: String, key: Char): String? {
        if (key != 'z' || word.any { toneIndex(it) > 0 }) return null
        for (index in word.indices.reversed()) {
            val shape = baseShape(word[index])
            val (base, rawKey) = when (shape) {
                'â' -> 'a' to 'a'
                'ê' -> 'e' to 'e'
                'ô' -> 'o' to 'o'
                'ă' -> 'a' to 'w'
                'ơ' -> 'o' to 'w'
                'ư' -> 'u' to 'w'
                else -> continue
            }
            val raw = matchCase(base, word[index])
            return "${word.replaceRange(index, index + 1, raw.toString())}$rawKey$key"
        }
        return null
    }

    /**
     * If a tone key was consumed inside a word that later becomes non-Vietnamese, restore it.
     * This lets brand/English text such as `Vinfast` stay literal while preserving `bàn` → `bàng`.
     */
    private fun restoreRawToneBeforeLiteral(word: String, key: Char): String? {
        if (key in "sfrxjz") return null
        val toneAt = word.indexOfFirst { toneIndex(it) > 0 }
        if (toneAt < 0) return null

        val suffix = word.substring(toneAt + 1)
        var sawConsonant = false
        var splitSyllable = false
        suffix.forEach { char ->
            if (isVowel(char)) {
                if (sawConsonant) splitSyllable = true
            } else {
                sawConsonant = true
            }
        }
        val vowelAfterFinalConsonant = isVowel(key) && sawConsonant
        if (!splitSyllable && !vowelAfterFinalConsonant) return null

        val source = word[toneAt]
        val toneKey = toneKeys[toneIndex(source)]
        val plain = families.getValue(baseShape(source))[0]
        val cleared = word.replaceRange(toneAt, toneAt + 1, matchCase(plain, source).toString())
        return "$cleared$toneKey$key"
    }

    private fun applyShape(word: String, key: Char): String? {
        undoRepeatedShape(word, key)?.let { return it }
        // Accept both prefix `dd...` and UniKey-style trailing `d...d`.
        if (key == 'd') {
            val target = when {
                word.last().lowercaseChar() == 'd' -> word.lastIndex
                word.first().lowercaseChar() == 'd' && word.any(::isVowel) -> 0
                else -> return null
            }
            return word.replaceRange(target, target + 1, matchCase('đ', word[target]).toString())
        }
        // Do not consume English modifier letters after an onset which cannot
        // begin a Vietnamese syllable: free, zoom, jazz, smart, Android, ...
        if (!hasVietnameseOnset(word)) return null
        // UniKey-style modifiers may reach the vowel nucleus across final consonants:
        // tieng + e -> tiêng, tuan + a -> tuân.
        if (key in "aeo" && !isVowel(word.last())) {
            val lastVowel = word.indexOfLast(::isVowel)
            val coda = if (lastVowel >= 0) word.substring(lastVowel + 1).lowercase() else ""
            if (coda !in setOf("c", "ch", "m", "n", "ng", "nh", "p", "t")) return null
        }
        if (key == 'w' && word.length >= 2) {
            val lastVowel = word.indexOfLast(::isVowel)
            val start = lastVowel - 1
            if (start >= 0 && word.substring(start, lastVowel + 1).lowercase() == "uo") {
                return word.replaceRange(start, lastVowel + 1, preserveCase("ươ", word.substring(start, lastVowel + 1)))
            }
        }
        val (targets, replacement) = when (key) {
            'a' -> setOf('a') to 'â'
            'e' -> setOf('e') to 'ê'
            'o' -> setOf('o') to 'ô'
            'w' -> setOf('a', 'o', 'u') to null
            else -> return null
        }
        for (i in word.indices.reversed()) {
            val shape = baseShape(word[i])
            if (shape !in targets) continue
            val outShape = replacement ?: mapOf('a' to 'ă', 'o' to 'ơ', 'u' to 'ư').getValue(shape)
            val toneIndex = toneIndex(word[i])
            val out = families.getValue(outShape)[toneIndex]
            return word.replaceRange(i, i + 1, matchCase(out, word[i]).toString())
        }
        return null
    }

    /**
     * Repeating the Telex shape key restores the raw keystrokes:
     * â+a→aa, ô+o→oo, đ+d→dd, ă+w→aw, ươ+w→uow.
     * Existing tone is kept on the unshaped vowel (ấ+a→áa).
     */
    private fun undoRepeatedShape(word: String, key: Char): String? {
        if (key == 'd') {
            if (word.last().lowercaseChar() != 'đ') return null
            val raw = if (word.last().isUpperCase()) "DD" else "dd"
            return word.dropLast(1) + raw
        }

        if (key == 'w') {
            // ươ is produced by one `w`; repeating it restores the original `uow` sequence.
            for (i in word.length - 2 downTo 0) {
                if (baseShape(word[i]) == 'ư' && baseShape(word[i + 1]) == 'ơ') {
                    val rawU = unshape(word[i], 'u')
                    val rawO = unshape(word[i + 1], 'o')
                    val unshaped = word.replaceRange(i, i + 2, "$rawU$rawO")
                    return "${unshaped}w"
                }
            }
        }

        val shapedToBase = when (key) {
            'a' -> mapOf('â' to 'a')
            'e' -> mapOf('ê' to 'e')
            'o' -> mapOf('ô' to 'o')
            'w' -> mapOf('ă' to 'a', 'ơ' to 'o', 'ư' to 'u')
            else -> return null
        }
        for (i in word.indices.reversed()) {
            val base = shapedToBase[baseShape(word[i])] ?: continue
            val rawVowel = unshape(word[i], base)
            val rawKey = if (word[i].isUpperCase()) key.uppercaseChar() else key
            val unshaped = word.replaceRange(i, i + 1, rawVowel.toString())
            // When the original modifier reached back across a final consonant,
            // its repeated escape belongs at the cursor, not beside the vowel:
            // bôt + o -> boto (not boot), dât + a -> data (not daat).
            return "$unshaped$rawKey"
        }
        return null
    }

    private fun unshape(source: Char, base: Char): Char {
        val tone = toneIndex(source)
        val out = families.getValue(base)[tone]
        return matchCase(out, source)
    }

    private fun applyTone(word: String, key: Char): String? {
        // A plain z is a literal English letter. It only acts as the Telex
        // remove-tone command when there is an actual tone to remove.
        if (key == 'z' && word.none { toneIndex(it) > 0 }) return null
        if (!hasVietnameseOnset(word)) return null
        val vowels = vowelIndices(word)
        if (vowels.isEmpty()) return null
        val target = toneTargetIndex(word, vowels) ?: return null
        val shape = baseShape(word[target])
        val tone = toneKeys.indexOf(key)
        if (tone < 0) return null
        if (tone > 0 && toneIndex(word[target]) == tone) {
            val plain = families.getValue(shape)[0]
            val cleared = word.replaceRange(
                target,
                target + 1,
                matchCase(plain, word[target]).toString()
            )
            val rawKey = if (word[target].isUpperCase()) key.uppercaseChar() else key
            return "$cleared$rawKey"
        }
        val out = families.getValue(shape)[tone]
        return word.replaceRange(target, target + 1, matchCase(out, word[target]).toString())
    }

    /** Vowel positions; `gi`/`qu` digraphs do not contribute a tone-bearing vowel when another vowel follows. */
    private fun vowelIndices(word: String): List<Int> {
        val all = word.indices.filter { isVowel(word[it]) }
        if (word.length < 3) return all
        val c0 = word[0].lowercaseChar()
        val c1 = word[1].lowercaseChar()
        val skipIndex1 = when {
            c0 == 'g' && c1 == 'i' && isVowel(word[2]) -> true
            c0 == 'q' && c1 == 'u' && isVowel(word[2]) -> true
            else -> false
        }
        return if (skipIndex1) all.filter { it != 1 } else all
    }

    /**
     * Modern (kiểu mới) main-vowel selection for tone marks.
     */
    private fun toneTargetIndex(word: String, vowels: List<Int>): Int? {
        if (vowels.isEmpty()) return null

        // Prefer last marked nucleus (ă â ê ô ơ ư)
        vowels.lastOrNull { baseShape(word[it]) in markedShapes }?.let { return it }

        if (vowels.size == 1) return vowels[0]

        val endsWithVowel = vowels.last() == word.lastIndex

        // Closed syllable (…vowel + consonant): tone on last vowel
        if (!endsWithVowel) return vowels.last()

        // Open syllable: use last two vowels for diphthong rules
        val iFirst = vowels[vowels.size - 2]
        val iSecond = vowels.last()
        val pair = "${baseShape(word[iFirst])}${baseShape(word[iSecond])}"

        // Kiểu mới: oa, oe, uy → òa, òe, ùy (tone on first)
        if (pair in setOf("oa", "oe", "uy")) return iFirst
        // ia, ya, ua, ưa → ía, ýa, úa, ứa
        if (pair in setOf("ia", "ya", "ua", "ưa")) return iFirst

        // Other open multi-vowel groups (ai, ao, au, eo, …): tone on first of the ending pair
        // e.g. chào (a+o→a), tái (a+i→a)
        return iFirst
    }

    private fun replaceLast(word: String, targets: Set<Char>, replacement: Char): String? {
        val i = word.indexOfLast { it.lowercaseChar() in targets }
        if (i < 0) return null
        return word.replaceRange(i, i + 1, matchCase(replacement, word[i]).toString())
    }

    private fun isVowel(c: Char) = familyByChar.containsKey(c.lowercaseChar())
    private fun hasVietnameseOnset(word: String): Boolean {
        val firstVowel = word.indexOfFirst(::isVowel)
        if (firstVowel < 0) return false
        return word.substring(0, firstVowel).lowercase() in vietnameseOnsets
    }
    private fun baseShape(c: Char) = familyByChar[c.lowercaseChar()] ?: c.lowercaseChar()
    private fun toneIndex(c: Char): Int {
        val shape = baseShape(c)
        return families[shape]?.indexOf(c.lowercaseChar())?.coerceAtLeast(0) ?: 0
    }
    private fun matchCase(out: Char, source: Char) = if (source.isUpperCase()) out.uppercaseChar() else out
    private fun preserveCase(out: String, source: String) =
        out.mapIndexed { i, c -> matchCase(c, source.getOrElse(i) { c }) }.joinToString("")
}
