package vn.kai.board.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceLanguageResolverTest {
    @Test fun expandsVietnameseForGoogleSpeech() {
        assertEquals("vi-VN", VoiceLanguageResolver.resolve("vi"))
    }

    @Test fun expandsCommonTranslationLanguages() {
        assertEquals("en-US", VoiceLanguageResolver.resolve("en"))
        assertEquals("zh-CN", VoiceLanguageResolver.resolve("zh"))
    }

    @Test fun preservesExplicitRegionalLocale() {
        assertEquals("en-GB", VoiceLanguageResolver.resolve("en-GB"))
    }
}
