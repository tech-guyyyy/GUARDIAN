package com.guardian.app.core.crypto

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Enterprise-grade PIN hashing using PBKDF2WithHmacSHA256.
 *
 * Security parameters:
 * - 210,000 iterations (aligns with modern OWASP password storage recommendations)
 * - 16-byte (128-bit) cryptographically secure salt generated via SecureRandom
 * - 256-bit derived key length
 * - Constant-time equality comparison via MessageDigest.isEqual
 */
object PinHasher {

    const val ITERATIONS = 210_000
    const val KEY_LENGTH_BITS = 256
    const val SALT_LENGTH_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    data class HashResult(
        val hashBase64: String,
        val saltBase64: String,
        val iterations: Int
    )

    /**
     * Generates a new cryptographically random salt.
     */
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        return salt
    }

    /**
     * Hashes the plaintext PIN using PBKDF2WithHmacSHA256 and a freshly generated salt.
     */
    fun hashPin(pin: CharArray, salt: ByteArray = generateSalt(), iterations: Int = ITERATIONS): HashResult {
        require(pin.isNotEmpty()) { "PIN cannot be empty" }
        require(salt.size >= SALT_LENGTH_BYTES) { "Salt must be at least $SALT_LENGTH_BYTES bytes" }

        val spec = PBEKeySpec(pin, salt, iterations, KEY_LENGTH_BITS)
        val skf = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = skf.generateSecret(spec).encoded
        spec.clearPassword()

        return HashResult(
            hashBase64 = Base64.getEncoder().encodeToString(hashBytes),
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            iterations = iterations
        )
    }

    /**
     * Verifies an input PIN against a previously stored hash and salt.
     * Uses MessageDigest.isEqual to ensure constant-time comparison and prevent timing side channels.
     */
    fun verifyPin(
        candidatePin: CharArray,
        storedHashBase64: String,
        storedSaltBase64: String,
        iterations: Int = ITERATIONS
    ): Boolean {
        return try {
            val salt = Base64.getDecoder().decode(storedSaltBase64)
            val expectedHash = Base64.getDecoder().decode(storedHashBase64)

            val spec = PBEKeySpec(candidatePin, salt, iterations, KEY_LENGTH_BITS)
            val skf = SecretKeyFactory.getInstance(ALGORITHM)
            val candidateHash = skf.generateSecret(spec).encoded
            spec.clearPassword()

            MessageDigest.isEqual(expectedHash, candidateHash)
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException,
                is NoSuchAlgorithmException,
                is InvalidKeySpecException -> false
                else -> throw e
            }
        }
    }
}
