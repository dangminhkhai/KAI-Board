package vn.kai.board.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PhrasePackTest {
    @Test
    fun parseTsvBuildsLeftToRightMap() {
        val text = """
            xin	chào
            xin	lỗi
            cảm	ơn
            # comment
            bad-line
            ok	nhé
        """.trimIndent()
        val map = PhrasePack.parseTsv(text)
        assertEquals(listOf("chào", "lỗi"), map["xin"])
        assertEquals(listOf("ơn"), map["cảm"])
        assertEquals(listOf("nhé"), map["ok"])
        assertFalse(map.containsKey("bad-line"))
    }

    @Test
    fun parseTsvDedupesAndLowercasesLeft() {
        val map = PhrasePack.parseTsv("Xin\tchào\nXIN\tchào\nxin\tlỗi\n")
        assertEquals(listOf("chào", "lỗi"), map["xin"])
    }

    @Test
    fun shippedPackOnDiskHasValidShapeAndHash() {
        val roots = listOf(
            java.io.File("phrase-packs/vi_social.tsv"),
            java.io.File("../phrase-packs/vi_social.tsv"),
            java.io.File("../../phrase-packs/vi_social.tsv"),
            java.io.File("app/src/main/assets/phrase_packs/vi_social.tsv"),
            java.io.File("src/main/assets/phrase_packs/vi_social.tsv"),
        )
        val file = roots.firstOrNull { it.isFile }
            ?: return // skip if working directory differs; parse unit tests still cover logic
        val bytes = file.readBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        assertEquals(PhrasePack.EXPECTED_SHA256, digest)
        val map = PhrasePack.parseTsv(bytes.toString(Charsets.UTF_8))
        val count = map.values.sumOf { it.size }
        assertTrue("expected 500–1.2M pairs, was $count", count in 500..1_200_000)
        assertTrue("expected large pack over 200k, was $count", count >= 200_000)
        assertTrue(map["xin"].orEmpty().isNotEmpty())
    }

    @Test
    fun mergeOrderIsPersonalThenPack() {
        val merged = SuggestionPriority.merge(
            emptyList(),
            listOf("personal"),
            listOf("pack"),
            limit = 5,
        )
        assertEquals(listOf("personal", "pack"), merged)
    }
}
