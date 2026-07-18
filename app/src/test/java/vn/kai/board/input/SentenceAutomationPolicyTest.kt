package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceAutomationPolicyTest {
    @Test fun capitalizesAtStartAndAfterSentencePunctuation() {
        assertTrue(SentenceAutomationPolicy.shouldCapitalize(""))
        assertTrue(SentenceAutomationPolicy.shouldCapitalize("Xin chào. "))
        assertTrue(SentenceAutomationPolicy.shouldCapitalize("Được không?  "))
        assertTrue(SentenceAutomationPolicy.shouldCapitalize("Hay quá!"))
        assertTrue(SentenceAutomationPolicy.shouldCapitalize("Dòng trước\n"))
    }

    @Test fun doesNotCapitalizeInsideSentence() {
        assertFalse(SentenceAutomationPolicy.shouldCapitalize("Xin chào "))
        assertFalse(SentenceAutomationPolicy.shouldCapitalize("phiên bản 1.1"))
    }

    @Test fun periodSpaceIsOptionalAndDuplicateSpaceIsDetected() {
        assertEquals(".", SentenceAutomationPolicy.periodOutput(false))
        assertEquals(". ", SentenceAutomationPolicy.periodOutput(true))
        assertTrue(SentenceAutomationPolicy.alreadyHasAutomaticPeriodSpace("x. "))
        assertFalse(SentenceAutomationPolicy.alreadyHasAutomaticPeriodSpace("x."))
    }
}
