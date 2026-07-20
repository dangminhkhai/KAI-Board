package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Test

class TelexWordComposerTest {
    @Test fun backspaceRemovesSingleComposingCharacterImmediately() {
        assertEquals("", TelexWordComposer.removeLast("d"))
    }

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

    @Test fun repairsShapeKeyTypedAfterToneAndFinalConsonant() {
        mapOf(
            "canfa" to "cần",
            "metje" to "mệt",
            "tonfo" to "tồn",
            "bangfw" to "bằng",
            "lonjw" to "lợn",
            "tungfw" to "từng",
            "tuongjw" to "tượng",
            "duongfw" to "dường",
        ).forEach { (keys, expected) ->
            var text = ""
            keys.forEach { key -> text = TelexWordComposer.append(text, key, 0).text }
            assertEquals(keys, expected, text)
        }
    }

    @Test fun restoresOriginalKeyOrderForClearlyInvalidVietnameseSyllables() {
        listOf(
            "Router",
            "router",
            "user",
            "order",
            "server",
            "laser",
            "cursor",
            "computer",
            "water",
        ).forEach { word ->
            assertEquals(word, word, TelexWordComposer.compose(word).text)
        }
    }

    @Test fun rawKeyRecoveryDoesNotDisableFlexibleVietnameseToneOrder() {
        mapOf(
            "hosa" to "hóa",
            "canfa" to "cần",
            "metje" to "mệt",
            "tuongjw" to "tượng",
        ).forEach { (keys, expected) ->
            assertEquals(keys, expected, TelexWordComposer.compose(keys).text)
        }
    }

    @Test fun supportsEveryTelexToneAndShapeKey() {
        mapOf(
            "mas" to "má",
            "maf" to "mà",
            "mar" to "mả",
            "max" to "mã",
            "maj" to "mạ",
            "dd" to "đ",
            "caa" to "câ",
            "mee" to "mê",
            "too" to "tô",
            "taw" to "tă",
            "tow" to "tơ",
            "tuw" to "tư",
        ).forEach { (keys, expected) ->
            assertEquals(keys, expected, TelexWordComposer.compose(keys).text)
        }
    }

    @Test fun escapedModifierUsesTheCaseOfTheActuallyPressedKey() {
        assertEquals("Ais", TelexWordComposer.compose("Asis").text)
        assertEquals("AiS", TelexWordComposer.compose("AsiS").text)
    }

    @Test fun supportsEveryToneWhenTheShapeKeyIsTypedLate() {
        mapOf(
            "cansa" to "cấn",
            "canfa" to "cần",
            "canra" to "cẩn",
            "canxa" to "cẫn",
            "canja" to "cận",
            "metse" to "mết",
            "metfe" to "mềt",
            "metre" to "mểt",
            "metxe" to "mễt",
            "metje" to "mệt",
            "tonso" to "tốn",
            "tonfo" to "tồn",
            "tonro" to "tổn",
            "tonxo" to "tỗn",
            "tonjo" to "tộn",
        ).forEach { (keys, expected) ->
            assertEquals(keys, expected, TelexWordComposer.compose(keys).text)
        }
    }

    @Test fun restoresEnglishCollisionsAcrossToneAndShapeKeys() {
        listOf(
            "safe",
            "router",
            "pixel",
            "object",
            "address",
            "screen",
            "google",
            "awesome",
            "power",
            "fluent",
        ).forEach { word ->
            assertEquals(word, word, TelexWordComposer.compose(word).text)
        }
    }

    @Test fun backspaceReplaysTheRemainingRawKeys() {
        val typed = TelexWordComposer.compose("user")
        assertEquals("user", typed.text)
        assertEquals("use", TelexWordComposer.backspace(typed.rawText, typed.literalLockLength).text)
    }

    @Test fun deletingEnglishWordDoesNotTurnRemainingDoubleVowelIntoTelex() {
        var state = TelexWordComposer.compose("Google")
        assertEquals("Google", state.text)
        val expected = listOf("Googl", "Goog", "Goo", "Go")
        expected.forEach { text ->
            state = TelexWordComposer.backspace(state.rawText, state.literalLockLength)
            assertEquals(text, text, state.text)
        }
    }

    @Test fun deletingEnglishWordsKeepsEveryToneKeyLiteral() {
        mapOf(
            "case" to listOf("cas", "ca"),
            "safe" to listOf("saf", "sa"),
            "care" to listOf("car", "ca"),
            "pixel" to listOf("pixe", "pix", "pi"),
            "object" to listOf("objec", "obje", "obj", "ob"),
        ).forEach { (word, expected) ->
            var state = TelexWordComposer.compose(word)
            expected.forEach { text ->
                state = TelexWordComposer.backspace(state.rawText, state.literalLockLength)
                assertEquals("$word -> $text", text, state.text)
            }
        }
    }
}
