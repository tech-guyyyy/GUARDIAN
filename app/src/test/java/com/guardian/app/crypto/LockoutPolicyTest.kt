package com.guardian.app.crypto

import com.google.common.truth.Truth.assertThat
import com.guardian.app.core.crypto.LockoutPolicy
import org.junit.Test

class LockoutPolicyTest {

    @Test
    fun `less than 3 failures does not trigger lockout`() {
        val policy = LockoutPolicy()
        val now = 1_000_000L

        policy.recordFailedAttempt(now)
        val status1 = policy.getStatus(now)
        assertThat(status1.isLockedOut).isFalse()

        policy.recordFailedAttempt(now + 1000)
        val status2 = policy.getStatus(now + 1000)
        assertThat(status2.isLockedOut).isFalse()
        assertThat(status2.failedAttemptsCount).isEqualTo(2)
    }

    @Test
    fun `3 failures triggers 30 second cooldown`() {
        val policy = LockoutPolicy()
        val now = 1_000_000L

        policy.recordFailedAttempt(now)
        policy.recordFailedAttempt(now + 1000)
        val status = policy.recordFailedAttempt(now + 2000)

        assertThat(status.isLockedOut).isTrue()
        assertThat(status.remainingCooldownSeconds).isEqualTo(30L)

        // After 31 seconds, cooldown should expire
        val afterCooldown = policy.getStatus(now + 2000 + 31_000)
        assertThat(afterCooldown.isLockedOut).isFalse()
    }

    @Test
    fun `5 failures triggers 5 minute cooldown`() {
        val policy = LockoutPolicy(failedAttempts = 4, lastFailedTimestampMs = 1_000_000L)
        val now = 1_001_000L

        val status = policy.recordFailedAttempt(now)
        assertThat(status.isLockedOut).isTrue()
        assertThat(status.remainingCooldownSeconds).isEqualTo(300L) // 5 minutes = 300s
    }

    @Test
    fun `recordSuccessfulAuth resets failure count and lockout`() {
        val policy = LockoutPolicy(failedAttempts = 5, lastFailedTimestampMs = 1_000_000L)
        policy.recordSuccessfulAuth()

        val status = policy.getStatus(1_000_100L)
        assertThat(status.isLockedOut).isFalse()
        assertThat(status.failedAttemptsCount).isEqualTo(0)
    }
}
