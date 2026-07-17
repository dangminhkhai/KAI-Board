package vn.kai.board.telex

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TelexCompatibilityMatrixTest(
    private val keys: String,
    private val expected: String,
) {
    @Test fun convertsLikeDesktopTelex() {
        assertEquals("keys=$keys", expected, typeSequence(keys))
    }

    private fun typeSequence(input: String): String = input.fold("") { word, key ->
        TelexEngine.apply(word, key) ?: "$word$key"
    }

    companion object {
        private val toneKeys = listOf('s', 'f', 'r', 'x', 'j')

        @JvmStatic
        @Parameterized.Parameters(name = "{0} -> {1}")
        fun cases(): Collection<Array<String>> {
            val cases = mutableListOf<Pair<String, String>>()

            val plainFamilies = listOf(
                "a" to "aáàảãạ",
                "e" to "eéèẻẽẹ",
                "i" to "iíìỉĩị",
                "o" to "oóòỏõọ",
                "u" to "uúùủũụ",
                "y" to "yýỳỷỹỵ",
            )
            plainFamilies.forEach { (input, family) ->
                toneKeys.forEachIndexed { index, tone -> cases += "$input$tone" to family[index + 1].toString() }
            }

            val shapedFamilies = listOf(
                "aa" to "âấầẩẫậ",
                "aw" to "ăắằẳẵặ",
                "ee" to "êếềểễệ",
                "oo" to "ôốồổỗộ",
                "ow" to "ơớờởỡợ",
                "uw" to "ưứừửữự",
            )
            shapedFamilies.forEach { (input, family) ->
                cases += input to family[0].toString()
                toneKeys.forEachIndexed { index, tone -> cases += "$input$tone" to family[index + 1].toString() }
            }

            cases += listOf(
                "dd" to "đ",
                "DD" to "Đ",
                "uow" to "ươ",
                "uows" to "ướ",
                "uowf" to "ườ",
                "uowr" to "ưở",
                "uowx" to "ưỡ",
                "uowj" to "ượ",
                "chaof" to "chào",
                "tieengs" to "tiếng",
                "tuaans" to "tuấn",
                "ddaats" to "đất",
                "nguowif" to "người",
                "Vieetj" to "Việt",
                "thuowng" to "thương",
                "truowngf" to "trường",
                "khoong" to "không",
                "ddieenj" to "điện",
                "thoaij" to "thoại",
                "quyeets" to "quyết",
                "toans" to "toán",
                "hoaf" to "hòa",
                "gias" to "giá",
                "quys" to "quý",
                "thuyr" to "thủy",
                "tias" to "tía",
                "toans" to "toán",
                "tuans" to "tuán",
                "tuAAns" to "tuẤn",
                "aas" to "ấ",
                "aass" to "âs",
                "aaf" to "ầ",
                "aaff" to "âf",
                "aws" to "ắ",
                "awss" to "ăs",
                "ees" to "ế",
                "eess" to "ês",
                "oos" to "ố",
                "ooss" to "ôs",
                "ows" to "ớ",
                "owss" to "ơs",
                "uws" to "ứ",
                "uwss" to "ưs",
                "ddd" to "dd",
                "aaa" to "aa",
                "eee" to "ee",
                "ooo" to "oo",
                "aww" to "aw",
                "oww" to "ow",
                "uww" to "uw",
                "uoww" to "uow",
                "AS" to "Á",
                "AAF" to "Ầ",
                "AWS" to "Ắ",
                "EES" to "Ế",
                "OOS" to "Ố",
                "OWS" to "Ớ",
                "UWS" to "Ứ",
                "Vinfast" to "Vinfast",
                "casse" to "case",
                "saffe" to "safe",
                "carre" to "care",
                "maxxed" to "maxed",
                "android" to "android",
                "Android" to "Android",
                "mazda" to "mazda",
                "amazon" to "amazon",
                "zalo" to "zalo",
            )

            return cases.distinct().map { (input, expected) -> arrayOf(input, expected) }
        }
    }
}
