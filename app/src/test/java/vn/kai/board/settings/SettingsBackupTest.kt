package vn.kai.board.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupTest {
    @Test fun rejectsSecretFields() {
        assertTrue(SettingsBackup.containsSecrets("{\"ai_secret\":\"x\"}"))
        assertTrue(SettingsBackup.containsSecrets("{\"api_key\":\"x\"}"))
        assertFalse(SettingsBackup.containsSecrets("{\"theme\":\"dark\"}"))
    }

    @Test fun includesPersonalDictionaryButNotSecretsOrClipboard() {
        val files = SettingsBackup.includedDataFiles()
        assertTrue("user_lexicon" in files)
        assertTrue("phrase_learning" in files)
        assertFalse("ai_secret" in files)
        assertFalse("clipboard_history" in files)
        assertTrue(SettingsBackup.includesThemeExtensions())
    }
}
