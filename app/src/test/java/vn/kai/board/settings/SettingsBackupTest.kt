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
}
