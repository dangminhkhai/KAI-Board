package vn.kai.board.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiKeyPoolTest {
    @Test fun parsesLinesAndRemovesDuplicates() {
        assertEquals(listOf("gsk_one", "gsk_two"), ApiKeyPool.parse(" gsk_one\ngsk_two, gsk_one "))
    }

    @Test fun ignoresBlankKeys() {
        assertEquals(emptyList<String>(), ApiKeyPool.parse(" \n, ; "))
    }
}
