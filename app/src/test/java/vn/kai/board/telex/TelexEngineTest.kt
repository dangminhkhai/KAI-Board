package vn.kai.board.telex

import org.junit.Assert.assertEquals
import org.junit.Test

class TelexEngineTest {
    @Test
    fun shapes() {
        assertEquals("â", TelexEngine.apply("a", 'a'))
        assertEquals("đ", TelexEngine.apply("d", 'd'))
        assertEquals("ươ", TelexEngine.apply("uo", 'w'))
    }

    @Test
    fun repeatedShapeKeyRestoresRawTelex() {
        assertEquals("aa", TelexEngine.apply("â", 'a'))
        assertEquals("ee", TelexEngine.apply("ê", 'e'))
        assertEquals("oo", TelexEngine.apply("ô", 'o'))
        assertEquals("dd", TelexEngine.apply("đ", 'd'))
        assertEquals("aw", TelexEngine.apply("ă", 'w'))
        assertEquals("ow", TelexEngine.apply("ơ", 'w'))
        assertEquals("uw", TelexEngine.apply("ư", 'w'))
        assertEquals("uow", TelexEngine.apply("ươ", 'w'))
    }

    @Test
    fun repeatedShapePreservesCaseAndTone() {
        assertEquals("AA", TelexEngine.apply("Â", 'a'))
        assertEquals("áa", TelexEngine.apply("ấ", 'a'))
        assertEquals("uów", TelexEngine.apply("ướ", 'w'))
    }

    @Test
    fun repeatedShapeEscapeStaysAtCursorAfterFinalConsonant() {
        assertEquals("data", TelexEngine.apply("dât", 'a'))
        assertEquals("boto", TelexEngine.apply("bôt", 'o'))
        assertEquals("tete", TelexEngine.apply("têt", 'e'))
        assertEquals("tanw", TelexEngine.apply("tăn", 'w'))
        assertEquals("uonw", TelexEngine.apply("ươn", 'w'))
        assertEquals("đatd", typeSequence("ddatd"))
    }

    @Test
    fun repeatedToneKeyRestoresLiteralKey() {
        assertEquals("as", TelexEngine.apply("á", 's'))
        assertEquals("af", TelexEngine.apply("à", 'f'))
        assertEquals("ar", TelexEngine.apply("ả", 'r'))
        assertEquals("ax", TelexEngine.apply("ã", 'x'))
        assertEquals("aj", TelexEngine.apply("ạ", 'j'))
        assertEquals("Vinf", TelexEngine.apply("Vìn", 'f'))
    }

    @Test
    fun repeatedToneUndoCoversEveryVowelFamilyAndCase() {
        val toneKeys = "sfrxj"
        val families = listOf(
            "aáàảãạ", "ăắằẳẵặ", "âấầẩẫậ", "eéèẻẽẹ", "êếềểễệ",
            "iíìỉĩị", "oóòỏõọ", "ôốồổỗộ", "ơớờởỡợ",
            "uúùủũụ", "ưứừửữự", "yýỳỷỹỵ"
        )
        families.forEach { family ->
            toneKeys.forEachIndexed { toneIndex, key ->
                val toned = family[toneIndex + 1]
                assertEquals("${family[0]}$key", TelexEngine.apply(toned.toString(), key))
            }
        }
        assertEquals("AS", TelexEngine.apply("Á", 's'))
        assertEquals("hoas", TelexEngine.apply("hóa", 's'))
        assertEquals("thuyr", TelexEngine.apply("thủy", 'r'))
    }

    @Test
    fun nonVietnameseWordRestoresConsumedToneKeys() {
        assertEquals("Vinfast", typeSequence("Vinfast"))
        assertEquals("Vinfast", typeSequence("Vinffast"))
        assertEquals("Sere", typeSequence("Serre"))
        assertEquals("sere", typeSequence("serre"))
        assertEquals("bàng", typeSequence("bangf"))
    }

    @Test
    fun shapeKeysReachBackAcrossFinalConsonants() {
        assertEquals("tiếng", typeSequence("tieengs"))
        assertEquals("tuấn", typeSequence("tuaans"))
        assertEquals("thê", typeSequence("thee"))
        assertEquals("cô", typeSequence("coo"))
    }

    @Test
    fun trailingShapeKeysReachTheirEarlierTargets() {
        assertEquals("đang", typeSequence("dangd"))
        assertEquals("Đang", typeSequence("Dangd"))
        assertEquals("tiêng", typeSequence("tienge"))
        assertEquals("tuân", typeSequence("tuana"))
        assertEquals("dương", typeSequence("duongw"))
        assertEquals("tương", typeSequence("tuongw"))
    }

    @Test
    fun englishToneEscapesStayLiteralForEveryToneKey() {
        // Pressing a tone key twice escapes it as a literal English letter.
        assertEquals("case", typeSequence("casse"))   // s
        assertEquals("safe", typeSequence("saffe"))   // f
        assertEquals("care", typeSequence("carre"))   // r
        assertEquals("maxed", typeSequence("maxxed")) // x
        assertEquals("major", typeSequence("majjorr")) // j, then escaped final r
    }

    @Test
    fun englishWordsKeepNonLocalDAndLiteralZ() {
        assertEquals("android", typeSequence("android"))
        assertEquals("Android", typeSequence("Android"))
        assertEquals("mazda", typeSequence("mazda"))
        assertEquals("amazon", typeSequence("amazon"))
        assertEquals("zalo", typeSequence("zalo"))
        assertEquals("đ", typeSequence("dd"))
        assertEquals("a", typeSequence("asz"))
    }

    @Test
    fun englishWordsKeepAllTelexModifierLettersAfterForeignOnsets() {
        assertEquals("fast", typeSequence("fast"))
        assertEquals("free", typeSequence("free"))
        assertEquals("smart", typeSequence("smart"))
        assertEquals("javascript", typeSequence("javascript"))
        assertEquals("zoom", typeSequence("zoom"))
        assertEquals("jazz", typeSequence("jazz"))
        assertEquals("frozen", typeSequence("frozen"))
    }

    @Test
    fun zIsLiteralUnlessThereIsARealToneToRemove() {
        assertEquals("z", typeSequence("z"))
        assertEquals("zero", typeSequence("zero"))
        assertEquals("zoo", typeSequence("zoo"))
        assertEquals("buzz", typeSequence("buzz"))
        assertEquals("pizza", typeSequence("pizza"))
        assertEquals("a", typeSequence("asz"))
        assertEquals("e", typeSequence("efz"))
        assertEquals("o", typeSequence("orz"))
        assertEquals("u", typeSequence("uxz"))
        assertEquals("y", typeSequence("yjz"))
    }

    @Test
    fun tonesBasic() {
        assertEquals("chào", TelexEngine.apply("chao", 'f'))
        assertEquals("tiếng", TelexEngine.apply("tiêng", 's'))
    }

    @Test
    fun adjacentSTypoAfterDRecoversVietnameseDStroke() {
        assertEquals("ds", typeSequence("ds"))
        assertEquals("đa", typeSequence("dsa"))
        assertEquals("đang", typeSequence("dsang"))
        assertEquals("đặng", typeSequence("dsangwj"))
        assertEquals("Đặng", typeSequence("Dsangwj"))
        assertEquals(typeSequence("ddawngj"), typeSequence("dsangwj"))
        assertEquals("đi", typeSequence("dsi"))
        assertEquals("đúng", typeSequence("dsungs"))
        assertEquals("đấy", typeSequence("dsaays"))
        assertEquals("được", typeSequence("dsuowcj"))
        assertEquals("điện", typeSequence("dsieenj"))
    }

    /** Kiểu mới: gi/qu là phụ âm kép — dấu vào nguyên âm sau. */
    @Test
    fun modernToneOnGiaQua() {
        assertEquals("giá", TelexEngine.apply("gia", 's'))
        assertEquals("già", TelexEngine.apply("gia", 'f'))
        assertEquals("gió", TelexEngine.apply("gio", 's'))
        assertEquals("quá", TelexEngine.apply("qua", 's'))
        assertEquals("quý", TelexEngine.apply("quy", 's'))
    }

    @Test
    fun modernOpenDiphthongs() {
        // oa/oe/uy kiểu mới: dấu trên o/u
        assertEquals("hòa", TelexEngine.apply("hoa", 'f'))
        assertEquals("hóa", TelexEngine.apply("hoa", 's'))
        // ia: dấu trên i (không phải gi)
        assertEquals("tía", TelexEngine.apply("tia", 's'))
    }

    @Test
    fun closedSyllableToneOnLastVowel() {
        assertEquals("toán", TelexEngine.apply("toan", 's'))
        assertEquals("tuán", TelexEngine.apply("tuan", 's')) // â + sắc → gõ "tuan" + a + s = "tuấn"
        assertEquals("tuấn", TelexEngine.apply("tuân", 's'))
    }

    @Test
    fun lateShapeCorrectionWinsBeforeEnglishFallback() {
        assertEquals("cần", TelexEngine.apply("càn", 'a'))
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
            assertEquals(keys, expected, typeSequence(keys))
        }
    }

    private fun typeSequence(keys: String): String = keys.fold("") { word, key ->
        TelexEngine.apply(word, key) ?: "$word$key"
    }
}
