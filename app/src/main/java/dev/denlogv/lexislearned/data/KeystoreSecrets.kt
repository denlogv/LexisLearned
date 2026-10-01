package dev.denlogv.lexislearned.data

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores secrets in SharedPreferences, encrypted with an AES-GCM key that never leaves the Android Keystore.
 *
 * @param sp where the encrypted values are kept.
 */
class KeystoreSecrets(private val sp: SharedPreferences) : Secrets {
    /**
     * Encrypts and saves a secret.
     *
     * @param name what the secret is for.
     * @param value the secret; an empty value deletes it.
     */
    override fun put(name: String, value: String) {
        if (value.isEmpty()) {
            sp.edit().remove(prefKey(name)).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val blob = cipher.iv + cipher.doFinal(value.toByteArray())
        sp.edit().putString(prefKey(name), Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
    }

    /**
     * Reads and decrypts a secret.
     *
     * @param name what the secret is for.
     * @return the secret, or null if none is saved or it cannot be decrypted (for example after the key was lost).
     */
    @Suppress("SwallowedException") // A secret that cannot be decrypted is treated as missing, so the user can enter it again.
    override fun get(name: String): String? {
        val blob = sp.getString(prefKey(name), null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, blob.copyOfRange(0, IV_BYTES)))
            String(cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES))
        } catch (e: GeneralSecurityException) {
            null
        }
    }

    /**
     * The preference key a secret is stored under.
     *
     * @param name what the secret is for.
     * @return the key.
     */
    private fun prefKey(name: String) = "secret_$name"

    /**
     * The encryption key, created on first use.
     *
     * @return the Keystore key.
     */
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(spec) }.generateKey()
    }

    private companion object {
        const val ALIAS = "lexislearned_secrets"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
