package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Test

class ComposingRewritePolicyTest {
    @Test fun shrinkingLatinUsesFinishDeleteSet() {
        assertEquals(
            ComposingRewriteMode.FinishDeleteSet,
            ComposingRewritePolicy.mode(previous = "Safe", next = "Saf", directCommit = false),
        )
    }

    @Test fun expandingMarkedToLatinUsesFinishDeleteSet() {
        assertEquals(
            ComposingRewriteMode.FinishDeleteSet,
            ComposingRewritePolicy.mode(previous = "Sà", next = "Safe", directCommit = false),
        )
    }

    @Test fun firstCharUsesSetOnly() {
        assertEquals(
            ComposingRewriteMode.SetOnly,
            ComposingRewritePolicy.mode(previous = "", next = "S", directCommit = false),
        )
    }

    @Test fun emptyNextClears() {
        assertEquals(
            ComposingRewriteMode.Clear,
            ComposingRewritePolicy.mode(previous = "Saf", next = "", directCommit = false),
        )
    }

    @Test fun directCommitEditorsUseDirectPath() {
        assertEquals(
            ComposingRewriteMode.DirectCommit,
            ComposingRewritePolicy.mode(previous = "Safe", next = "Saf", directCommit = true),
        )
    }

    @Test fun hostileOemForcesDirectEvenWhenNotDirectEditor() {
        assertEquals(
            ComposingRewriteMode.DirectCommit,
            ComposingRewritePolicy.mode(
                previous = "Safe",
                next = "Saf",
                directCommit = false,
                preferDirectCommit = true,
            ),
        )
    }
}
