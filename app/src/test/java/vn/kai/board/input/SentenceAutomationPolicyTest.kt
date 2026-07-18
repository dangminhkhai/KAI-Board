package vn.kai.board.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceAutomationPolicyTest {
    @Test fun capitalizesOnlyAtStartAndAfterNewLine() {
        assertTrue(SentenceAutomationPolicy.shouldCapitalize(""))
        assertTrue(SentenceAutomationPolicy.shouldCapitalize("Dòng trước\n"))
    }

    @Test fun doesNotCapitalizeInsideSentenceOrAfterPunctuation() {
        assertFalse(SentenceAutomationPolicy.shouldCapitalize("Xin chào "))
        assertFalse(SentenceAutomationPolicy.shouldCapitalize("phiên bản 1.1"))
        assertFalse(SentenceAutomationPolicy.shouldCapitalize("Xin chào. "))
        assertFalse(SentenceAutomationPolicy.shouldCapitalize("Được không?  "))
        assertFalse(SentenceAutomationPolicy.shouldCapitalize("Hay quá!"))
    }
}
