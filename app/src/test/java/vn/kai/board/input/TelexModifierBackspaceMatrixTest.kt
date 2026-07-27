package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Full Telex modifier matrix for Backspace / OEM shrink:
 * tones s f r x j and shapes aa ee oo aw ow uw dd.
 */
class TelexModifierBackspaceMatrixTest {

    @Test fun toneKeysComposeAndFirstBackspaceStayLatin() {
        mapOf(
            "case" to "cas",   // s
            "safe" to "saf",   // f
            "care" to "car",   // r
            "pixel" to "pixe", // x (inside word)
            "object" to "objec",
        ).forEach { (word, afterOne) ->
            val state = TelexWordComposer.compose(word)
            assertEquals("compose $word", word, state.text)
            val bs = TelexWordComposer.backspace(state.text, state.rawText, state.literalLockLength)
            assertEquals("backspace $word", afterOne, bs.text)
            assertTrue(
                "prefix-shrink ${state.text}→${bs.text}",
                ComposingEditorSync.isPrefixShrink(state.text, bs.text),
            )
        }
    }

    @Test fun everyToneLetterSfrxjEscapeAndBackspace() {
        listOf('s', 'f', 'r', 'x', 'j').forEach { tone ->
            val base = "Vi"
            val toned = TelexWordComposer.append(base, 'n', 0).let {
                TelexWordComposer.append(it.text, tone, it.literalLockLength, it.rawText)
            }
            // Vin+tone → Vín/Vìn/…
            val escaped = TelexWordComposer.append(toned.text, tone, toned.literalLockLength, toned.rawText)
            assertTrue("escape $tone: ${escaped.text}", escaped.text.endsWith(tone) || escaped.text.endsWith(tone.uppercaseChar()))
            assertTrue(escaped.literalLockLength > 0)
            val extended = TelexWordComposer.append(escaped.text, 'a', escaped.literalLockLength, escaped.rawText)
            val bs = TelexWordComposer.backspace(extended.text, extended.rawText, extended.literalLockLength)
            assertEquals(escaped.text, bs.text)
            assertTrue(ComposingEditorSync.isPrefixShrink(extended.text, bs.text))
        }
    }

    @Test fun shapeKeysVietnameseApply() {
        mapOf(
            "aa" to "â",
            "ee" to "ê",
            "oo" to "ô",
            "aw" to "ă",
            "ow" to "ơ",
            "uw" to "ư",
            "dd" to "đ",
            "uow" to "ươ",
        ).forEach { (keys, expected) ->
            assertEquals(keys, expected, TelexWordComposer.compose(keys).text)
        }
    }

    @Test fun typoAfterCompletedTelexWordNeverExplodesToRawKeys() {
        mapOf(
            "bans" to ("bán" to 'e'), // s
            "banf" to ("bàn" to 'e'), // f
            "banr" to ("bản" to 'e'), // r
            "banx" to ("bãn" to 'e'), // x
            "banj" to ("bạn" to 'e'), // j
            "caan" to ("cân" to 'i'), // aa
            "teen" to ("tên" to 'i'), // ee
            "toon" to ("tôn" to 'i'), // oo
            "cawn" to ("căn" to 'i'), // aw
            "town" to ("tơn" to 'i'), // ow
            "tuwng" to ("tưng" to 'i'), // uw
            "ddang" to ("đang" to 'e'), // dd — reported regression
        ).forEach { (keys, expected) ->
            val (rendered, typoKey) = expected
            val completed = TelexWordComposer.compose(keys)
            assertEquals("precondition $keys", rendered, completed.text)
            val typo = TelexWordComposer.append(
                completed.text,
                typoKey,
                completed.literalLockLength,
                completed.rawText,
            )
            assertEquals("$keys + typo", "$rendered$typoKey", typo.text)
            assertEquals("$keys raw history", "$keys$typoKey", typo.rawText)
        }
    }

    @Test fun shapeKeysRepeatedEscapeThenBackspaceDeletesVisible() {
        // aaa→aa, eee→ee, … first BS must shorten display (not no-op while raw shrinks).
        mapOf(
            "aaa" to ("aa" to "a"),
            "eee" to ("ee" to "e"),
            "ooo" to ("oo" to "o"),
            "aww" to ("aw" to "a"),
            "oww" to ("ow" to "o"),
            "uww" to ("uw" to "u"),
            "ddd" to ("dd" to "d"),
            "uoww" to ("uow" to "uo"),
        ).forEach { (keys, pair) ->
            val (escaped, afterBs) = pair
            val state = TelexWordComposer.compose(keys)
            assertEquals(keys, escaped, state.text)
            val bs = TelexWordComposer.backspace(state.text, state.rawText, state.literalLockLength)
            assertEquals("BS after shape escape $keys", afterBs, bs.text)
            assertTrue(
                "shrink $escaped→$afterBs",
                ComposingEditorSync.isPrefixShrink(escaped, afterBs),
            )
        }
    }

    @Test fun shapeEnglishWordsBackspaceStayLatin() {
        mapOf(
            "google" to listOf("googl", "goog", "goo", "go"),
            "Google" to listOf("Googl", "Goog", "Goo", "Go"),
            "power" to listOf("powe", "pow", "po"),
            "awesome" to listOf("awesom", "aweso", "awes", "awe", "aw"),
            "address" to listOf("addres", "addre", "addr", "add", "ad"),
            "free" to listOf("fre", "fr"),
            "zoom" to listOf("zoo", "zo"),
        ).forEach { (word, steps) ->
            var state = TelexWordComposer.compose(word)
            assertEquals(word, state.text)
            steps.forEach { expected ->
                val prev = state.text
                state = TelexWordComposer.backspace(state.text, state.rawText, state.literalLockLength)
                assertEquals("$word → $expected", expected, state.text)
                assertTrue(ComposingEditorSync.isPrefixShrink(prev, state.text))
            }
        }
    }

    @Test fun vietnameseMarkedBackspaceDeletesVisibleNotRawToneKey() {
        mapOf(
            "mas" to ("má" to "m"),
            "maf" to ("mà" to "m"),
            "mar" to ("mả" to "m"),
            "max" to ("mã" to "m"),
            "maj" to ("mạ" to "m"),
            "caa" to ("câ" to "c"),
            "mee" to ("mê" to "m"),
            "too" to ("tô" to "t"),
            "taw" to ("tă" to "t"),
            "tow" to ("tơ" to "t"),
            "tuw" to ("tư" to "t"),
            "dd" to ("đ" to ""),
            "tieengs" to ("tiếng" to "tiến"),
        ).forEach { (keys, pair) ->
            val (rendered, afterBs) = pair
            val state = TelexWordComposer.compose(keys)
            assertEquals(keys, rendered, state.text)
            val bs = TelexWordComposer.backspace(state.text, state.rawText, state.literalLockLength)
            assertEquals("VN BS $keys ($rendered)", afterBs, bs.text)
        }
    }

    @Test fun modifierRiskDetectionCoversToneAndShape() {
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("Saf"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("cas"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("car"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("max"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("maj"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("aa"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("ee"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("oo"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("aw"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("ow"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("uw"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("dd"))
        assertTrue(ComposingEditorSync.endsWithTelexModifierLetter("uow"))
        assertFalse(ComposingEditorSync.endsWithTelexModifierLetter("Safe"))
        assertFalse(ComposingEditorSync.endsWithTelexModifierLetter("hello"))
        assertFalse(ComposingEditorSync.endsWithTelexModifierLetter("cat"))
    }

    @Test fun uppercaseToneCollisionsBackspace() {
        mapOf(
            "Safe" to "Saf",
            "Cafe" to "Caf",
            "Case" to "Cas",
            "Care" to "Car",
        ).forEach { (word, expected) ->
            val state = TelexWordComposer.compose(word)
            assertEquals(word, state.text)
            val bs = TelexWordComposer.backspace(state.text, state.rawText, state.literalLockLength)
            assertEquals(expected, bs.text)
            assertTrue(ComposingEditorSync.isPrefixShrink(word, expected))
        }
    }
}
