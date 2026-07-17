package vn.kai.board.input

object LongPressSymbolMap {
    private val symbols = mapOf(
        'q' to '`', 'w' to '~', 'e' to '[', 'r' to ']', 't' to '{',
        'y' to '}', 'u' to '<', 'i' to '>', 'o' to '^', 'p' to '|',
        'a' to '@', 's' to '#', 'd' to '$', 'f' to '%', 'g' to '&',
        'h' to '-', 'j' to '+', 'k' to '(', 'l' to ')',
        'z' to '*', 'x' to '"', 'c' to '\'', 'v' to ':', 'b' to ';',
        'n' to '!', 'm' to '?',
    )

    fun forKey(value: Char): Char? = symbols[value.lowercaseChar()]
}
