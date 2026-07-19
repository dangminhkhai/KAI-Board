package vn.kai.board.input

/** Returns the UTF-16 length of the last user-perceived Unicode character. */
object UnicodeDeletionPolicy {
    fun charactersToDeleteBeforeCursor(text: CharSequence): Int {
        if (text.isEmpty()) return 0
        var start = text.length

        fun previousCodePoint(): Int = Character.codePointBefore(text, start)
        fun consumeCodePoint() { start -= Character.charCount(previousCodePoint()) }
        fun consumeClusterPart() {
            while (start > 0 && isExtender(previousCodePoint())) consumeCodePoint()
            if (start > 0) consumeCodePoint()
        }

        consumeClusterPart()
        if (start > 0 && isRegionalIndicator(Character.codePointAt(text, start)) &&
            isRegionalIndicator(previousCodePoint())) {
            consumeCodePoint()
        }
        while (start > 0 && previousCodePoint() == ZERO_WIDTH_JOINER) {
            consumeCodePoint()
            consumeClusterPart()
        }
        return text.length - start
    }

    fun removeLastCluster(text: String): String =
        text.dropLast(charactersToDeleteBeforeCursor(text))

    private fun isExtender(codePoint: Int): Boolean {
        val type = Character.getType(codePoint)
        return codePoint == VARIATION_SELECTOR_15 || codePoint == VARIATION_SELECTOR_16 ||
            codePoint in EMOJI_MODIFIER_START..EMOJI_MODIFIER_END ||
            codePoint in TAG_START..TAG_END ||
            codePoint == COMBINING_KEYCAP ||
            type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt()
    }

    private fun isRegionalIndicator(codePoint: Int) = codePoint in 0x1F1E6..0x1F1FF

    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val VARIATION_SELECTOR_15 = 0xFE0E
    private const val VARIATION_SELECTOR_16 = 0xFE0F
    private const val COMBINING_KEYCAP = 0x20E3
    private const val EMOJI_MODIFIER_START = 0x1F3FB
    private const val EMOJI_MODIFIER_END = 0x1F3FF
    private const val TAG_START = 0xE0020
    private const val TAG_END = 0xE007F
}
