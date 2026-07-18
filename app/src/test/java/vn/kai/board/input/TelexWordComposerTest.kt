package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Test

class TelexWordComposerTest {
    @Test fun repeatedToneEscapeKeepsRestOfCurrentWordLiteral() {
        var text = "Vin"
        var lock = 0

        TelexWordComposer.append(text, 'f', lock).also { text = it.text; lock = it.literalLockLength }
        assertEquals("Vìn", text)
        assertEquals(0, lock)

        TelexWordComposer.append(text, 'f', lock).also { text = it.text; lock = it.literalLockLength }
        assertEquals("Vinf", text)

        TelexWordComposer.append(text, 'a', lock).also { text = it.text; lock = it.literalLockLength }
        TelexWordComposer.append(text, 's', lock).also { text = it.text; lock = it.literalLockLength }
        assertEquals("Vinfas", text)
    }

    @Test fun deletingPastEscapeUnlocksTelexAgain() {
        assertEquals(4, TelexWordComposer.lockAfterBackspace(4, 4))
        assertEquals(0, TelexWordComposer.lockAfterBackspace(3, 4))
    }

    @Test fun everyRepeatedToneKeyLocksTheRemainingWordAsLiteral() {
        listOf(
            Triple("Vín", 's', "Vins"),
            Triple("Vìn", 'f', "Vinf"),
            Triple("Vỉn", 'r', "Vinr"),
            Triple("Vĩn", 'x', "Vinx"),
            Triple("Vịn", 'j', "Vinj"),
        ).forEach { (word, key, expected) ->
            val escaped = TelexWordComposer.append(word, key, 0)
            assertEquals(expected, escaped.text)
            assertEquals(expected.length, escaped.literalLockLength)
            assertEquals(expected + "as", TelexWordComposer.append(expected + "a", 's', escaped.literalLockLength).text)
        }
    }

    @Test fun repeatedShapeAndStrokeKeysAlsoLockTheRemainingWord() {
        listOf(
            Triple("dât", 'a', "data"),
            Triple("bôt", 'o', "boto"),
            Triple("têt", 'e', "tete"),
            Triple("tăn", 'w', "tanw"),
            Triple("đ", 'd', "dd"),
        ).forEach { (word, key, expected) ->
            val escaped = TelexWordComposer.append(word, key, 0)
            assertEquals(expected, escaped.text)
            assertEquals(expected.length, escaped.literalLockLength)
        }
    }
}
