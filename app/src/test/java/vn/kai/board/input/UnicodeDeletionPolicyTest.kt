package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Test

class UnicodeDeletionPolicyTest {
    @Test fun deletesWholeSurrogatePair() {
        assertEquals("a", UnicodeDeletionPolicy.removeLastCluster("a😀"))
    }

    @Test fun deletesEmojiWithSkinTone() {
        assertEquals("a", UnicodeDeletionPolicy.removeLastCluster("a👍🏽"))
    }

    @Test fun deletesFlagPair() {
        assertEquals("a", UnicodeDeletionPolicy.removeLastCluster("a🇻🇳"))
    }

    @Test fun deletesJoinedFamilyEmoji() {
        assertEquals("a", UnicodeDeletionPolicy.removeLastCluster("a👨‍👩‍👧‍👦"))
    }

    @Test fun deletesKeycapSequence() {
        assertEquals("a", UnicodeDeletionPolicy.removeLastCluster("a1️⃣"))
    }

    @Test fun deletesOneAsciiCharacter() {
        assertEquals("a", UnicodeDeletionPolicy.removeLastCluster("ab"))
    }
}
