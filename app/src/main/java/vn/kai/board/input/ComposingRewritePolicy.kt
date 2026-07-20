package vn.kai.board.input

/**
 * How the IME should push a new composing string into [android.view.inputmethod.InputConnection].
 *
 * Vivo OriginOS and similar OEMs have been observed corrupting in-place composing rewrites
 * (e.g. `Safe` → `Saff`). On those devices always use verified direct commit (delete suffix +
 * [android.view.inputmethod.InputConnection.commitText]) instead of [setComposingText].
 */
enum class ComposingRewriteMode {
    /** First character via setComposingText. */
    SetOnly,

    /** Clear composition entirely. */
    Clear,

    /**
     * finishComposingText → delete matching suffix before cursor → commitText(next).
     * Used for direct-commit editors and hostile OEM composing implementations.
     */
    DirectCommit,

    /** finishComposingText → deleteSurroundingText(previous) → setComposingText(next). */
    FinishDeleteSet,
}

object ComposingRewritePolicy {
    fun mode(
        previous: String,
        next: String,
        directCommit: Boolean,
        preferDirectCommit: Boolean = false,
    ): ComposingRewriteMode = when {
        directCommit || preferDirectCommit -> ComposingRewriteMode.DirectCommit
        next.isEmpty() -> ComposingRewriteMode.Clear
        previous.isEmpty() -> ComposingRewriteMode.SetOnly
        previous == next -> ComposingRewriteMode.SetOnly
        else -> ComposingRewriteMode.FinishDeleteSet
    }
}
