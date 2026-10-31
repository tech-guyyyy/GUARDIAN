package com.guardian.app.core.crypto

/**
 * Manages rate-limiting and progressive exponential lockout for PIN verification attempts.
 *
 * Backoff progression:
 * - 0 to 2 failed attempts: No delay (immediate retry)
 * - 3 to 4 failed attempts: 30 seconds delay
 * - 5 to 6 failed attempts: 5 minutes delay (300 seconds)
 * - 7 to 9 failed attempts: 30 minutes delay (1,800 seconds)
 * - 10+ failed attempts: 24 hours lockout (86,400 seconds)
 */
class LockoutPolicy(
    var failedAttempts: Int = 0,
    var lastFailedTimestampMs: Long = 0L
) {
    data class Status(
        val isLockedOut: Boolean,
        val remainingCooldownSeconds: Long,
        val failedAttemptsCount: Int
    )

    fun getStatus(currentTimeMs: Long = System.currentTimeMillis()): Status {
        if (failedAttempts < 3) {
            return Status(isLockedOut = false, remainingCooldownSeconds = 0, failedAttemptsCount = failedAttempts)
        }

        val requiredCooldownMs = getCooldownDurationMs(failedAttempts)
        val elapsedMs = currentTimeMs - lastFailedTimestampMs

        return if (elapsedMs < requiredCooldownMs) {
            val remainingMs = requiredCooldownMs - elapsedMs
            val remainingSec = (remainingMs + 999) / 1000
            Status(isLockedOut = true, remainingCooldownSeconds = remainingSec, failedAttemptsCount = failedAttempts)
        } else {
            Status(isLockedOut = false, remainingCooldownSeconds = 0, failedAttemptsCount = failedAttempts)
        }
    }

    fun recordFailedAttempt(currentTimeMs: Long = System.currentTimeMillis()): Status {
        failedAttempts++
        lastFailedTimestampMs = currentTimeMs
        return getStatus(currentTimeMs)
    }

    fun recordSuccessfulAuth() {
        failedAttempts = 0
        lastFailedTimestampMs = 0L
    }

    companion object {
        fun getCooldownDurationMs(failures: Int): Long {
            return when {
                failures < 3 -> 0L
                failures < 5 -> 30_000L           // 30 seconds
                failures < 7 -> 300_000L          // 5 minutes
                failures < 10 -> 1_800_000L       // 30 minutes
                else -> 86_400_000L               // 24 hours
            }
        }
    }
}
