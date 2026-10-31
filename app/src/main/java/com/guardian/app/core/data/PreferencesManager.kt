package com.guardian.app.core.data

import android.content.Context
import android.content.SharedPreferences
import com.guardian.app.core.crypto.CryptoEngine
import com.guardian.app.core.crypto.LockoutPolicy
import com.guardian.app.core.crypto.PinHasher

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    var pinSalt: String?
        get() = prefs.getString(KEY_PIN_SALT, null)
        set(value) = prefs.edit().putString(KEY_PIN_SALT, value).apply()

    var nextDnsProfileId: String
        get() = prefs.getString(KEY_NEXTDNS_PROFILE, DEFAULT_NEXTDNS_PROFILE) ?: DEFAULT_NEXTDNS_PROFILE
        set(value) = prefs.edit().putString(KEY_NEXTDNS_PROFILE, value.trim()).apply()

    val isPinConfigured: Boolean
        get() = !pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty()

    fun savePin(pin: CharArray) {
        val result = PinHasher.hashPin(pin)
        prefs.edit()
            .putString(KEY_PIN_HASH, result.hashBase64)
            .putString(KEY_PIN_SALT, result.saltBase64)
            .apply()
    }

    fun verifyPin(candidatePin: CharArray): Boolean {
        val hash = pinHash ?: return false
        val salt = pinSalt ?: return false
        return PinHasher.verifyPin(candidatePin, hash, salt)
    }

    fun loadLockoutPolicy(): LockoutPolicy {
        val failures = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        val timestamp = prefs.getLong(KEY_LAST_FAILED_TIMESTAMP, 0L)
        return LockoutPolicy(failures, timestamp)
    }

    fun saveLockoutPolicy(policy: LockoutPolicy) {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, policy.failedAttempts)
            .putLong(KEY_LAST_FAILED_TIMESTAMP, policy.lastFailedTimestampMs)
            .apply()
    }

    /**
     * Stores sensitive operational configuration using Keystore AES-GCM.
     */
    fun saveEncryptedSetting(key: String, plainValue: String) {
        val cipherPayload = CryptoEngine.encrypt(plainValue)
        prefs.edit().putString(KEY_ENCRYPTED_PREFIX + key, cipherPayload).apply()
    }

    fun getEncryptedSetting(key: String): String? {
        val cipherPayload = prefs.getString(KEY_ENCRYPTED_PREFIX + key, null) ?: return null
        return try {
            CryptoEngine.decrypt(cipherPayload)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "guardian_secure_preferences"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_NEXTDNS_PROFILE = "nextdns_profile"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LAST_FAILED_TIMESTAMP = "last_failed_timestamp"
        private const val KEY_ENCRYPTED_PREFIX = "enc_"

        const val DEFAULT_NEXTDNS_PROFILE = "guardian-shield.dns.nextdns.io"
    }
}
