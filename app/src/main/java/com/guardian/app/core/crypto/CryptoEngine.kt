package com.guardian.app.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AES-256-GCM encryption engine leveraging the AndroidKeyStore provider.
 * Avoids deprecated androidx.security:security-crypto alpha APIs.
 */
object CryptoEngine {

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "GUARDIAN_MASTER_KEY"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    @Synchronized
    fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE_PROVIDER
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Returns a formatted Base64 string: <Base64(IV)>:<Base64(CiphertextWithGcmTag)>
     */
    fun encrypt(plainText: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherTextWithTag = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        val ivBase64 = Base64.getEncoder().encodeToString(iv)
        val cipherBase64 = Base64.getEncoder().encodeToString(cipherTextWithTag)
        return "$ivBase64:$cipherBase64"
    }

    /**
     * Decrypts an encrypted payload formatted as <Base64(IV)>:<Base64(CiphertextWithGcmTag)>.
     */
    fun decrypt(encryptedPayload: String): String {
        val parts = encryptedPayload.split(":")
        require(parts.size == 2) { "Invalid encrypted payload format" }

        val iv = Base64.getDecoder().decode(parts[0])
        val cipherTextWithTag = Base64.getDecoder().decode(parts[1])

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(cipherTextWithTag)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }
}
