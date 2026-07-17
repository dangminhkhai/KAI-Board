package vn.kai.board.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureApiKeyStore {
    private const val ALIAS = "kai_board_ai_api_key"
    private const val FILE = "ai_secret"
    private const val VALUE = "value"
    private const val IV = "iv"

    fun save(context: Context, apiKey: String) {
        if (apiKey.isBlank()) return clear(context)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(VALUE, Base64.encodeToString(cipher.doFinal(apiKey.trim().toByteArray()), Base64.NO_WRAP))
            .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun read(context: Context): String = runCatching {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val encrypted = Base64.decode(prefs.getString(VALUE, ""), Base64.NO_WRAP)
        val iv = Base64.decode(prefs.getString(IV, ""), Base64.NO_WRAP)
        if (encrypted.isEmpty() || iv.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted))
    }.getOrDefault("")

    fun clear(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }
    }
}
