package vn.kai.board.input

import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiCatalogTest {
    @Test fun searchesVietnameseWithoutRequiringDiacritics() {
        assertTrue("🐶" in EmojiCatalog.search("cho"))
        assertTrue("❤️" in EmojiCatalog.search("yeu"))
    }

    @Test fun searchesEnglishAliases() {
        assertTrue("📱" in EmojiCatalog.search("phone"))
    }
}
