package vn.kai.board.input

/**
 * Decides how much text before the cursor belongs to the current Telex word so the IME can
 * replace it safely.
 *
 * Observed Vivo OriginOS failure mode:
 * rewriting `Safe` → `Saf` via delete-all + [android.view.inputmethod.InputConnection.commitText]
 * ends up as `Saff` (final tone letter doubled). Backspace must therefore prefer **delete-only**
 * when [next] is a proper prefix of [previous], never re-commit the shorter Latin form.
 */
object ComposingEditorSync {
    /**
     * Suffix of [beforeCursor] that should be deleted before writing [next].
     * Empty string means delete nothing.
     */
    fun suffixToDelete(beforeCursor: String, previous: String, next: String): String {
        if (previous.isNotEmpty() && beforeCursor.endsWith(previous)) return previous

        // Already showing the desired next word (idempotent rewrite / repair mid-flight).
        if (next.isNotEmpty() && beforeCursor.endsWith(next)) return next

        // Observed Vivo corruption: shrinking `Safe`→`Saf` leaves a doubled final tone letter `Saff`.
        if (next.isNotEmpty() && beforeCursor.endsWith(next + next.last())) {
            return next + next.last()
        }

        // Same corruption while we still believe previous is the longer Latin form (`Safe` vs `Saff`).
        if (previous.length >= 2) {
            val doubledLast = previous.dropLast(1) + previous.last() + previous.last()
            if (beforeCursor.endsWith(doubledLast)) return doubledLast
        }

        // If the editor only has a proper prefix of previous (partial delete already happened).
        if (previous.isNotEmpty() && next.isNotEmpty() && previous.startsWith(next) && beforeCursor.endsWith(next)) {
            return next
        }

        return if (previous.isNotEmpty()) previous else ""
    }

    /**
     * True when [next] is [previous] with only a trailing slice removed — editor should only delete,
     * never commit the shorter string (OEM may double a trailing Telex modifier).
     *
     * Covers every Telex modifier class on Backspace:
     * tones `s f r x j`, shapes `aa ee oo aw ow uw`, and `dd`.
     */
    fun isPrefixShrink(previous: String, next: String): Boolean =
        next.length < previous.length && previous.startsWith(next)

    /** UTF-16 units to delete for a pure prefix shrink. */
    fun prefixShrinkDeleteLength(previous: String, next: String): Int =
        if (isPrefixShrink(previous, next)) previous.length - next.length else 0

    fun manufacturersPreferDirectCommit(manufacturer: String, brand: String): Boolean {
        val m = manufacturer.lowercase()
        val b = brand.lowercase()
        return m.contains("vivo") || b.contains("vivo") ||
            m.contains("bbk") || b.contains("bbk") ||
            m.contains("iqoo") || b.contains("iqoo")
    }

    /** All Latin keys Telex consumes as tone or shape modifiers. */
    const val TELEX_MODIFIER_LETTERS = "sfrxjaeowd"

    /**
     * Trailing Latin Telex tone/shape letter that OEMs sometimes re-parse on commitText.
     * Includes:
     * - tones: s f r x j (Safe→Saf, case→cas, …)
     * - shapes: a e o w d when they close a restored pair (aa, ee, oo, aw, ow, uw, dd)
     */
    fun endsWithTelexToneLetter(text: String): Boolean = endsWithTelexModifierLetter(text)

    fun endsWithTelexModifierLetter(text: String): Boolean {
        if (text.isEmpty()) return false
        val last = text.last().lowercaseChar()
        if (last !in TELEX_MODIFIER_LETTERS) return false
        val body = text.dropLast(1)
        if (body.isEmpty()) return false
        // Tone after any vowel / marked vowel (Sa+f).
        if (last in "sfrxj" && body.any(::isTelexVowelLike)) return true
        // Shape digraph restore: …a+a, …e+e, …o+o, …a+w, …o+w, …u+w, …d+d
        val prev = body.last().lowercaseChar()
        return when (last) {
            'a' -> prev == 'a' || isTelexVowelLike(prev)
            'e' -> prev == 'e' || isTelexVowelLike(prev)
            'o' -> prev == 'o' || isTelexVowelLike(prev)
            'w' -> prev in "aouăâơư" || isTelexVowelLike(prev)
            'd' -> prev == 'd' || prev == 'đ'
            else -> false
        }
    }

    private fun isTelexVowelLike(ch: Char): Boolean {
        val c = ch.lowercaseChar()
        return c in "aeiouyáàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵ"
    }
}
