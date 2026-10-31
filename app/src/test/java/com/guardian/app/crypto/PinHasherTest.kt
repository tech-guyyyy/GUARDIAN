package com.guardian.app.crypto

import com.google.common.truth.Truth.assertThat
import com.guardian.app.core.crypto.PinHasher
import org.junit.Test

class PinHasherTest {

    @Test
    fun `hashPin generates unique salt and valid base64 hash`() {
        val pin = "849201".toCharArray()
        val result1 = PinHasher.hashPin(pin)
        val result2 = PinHasher.hashPin(pin)

        assertThat(result1.hashBase64).isNotEmpty()
        assertThat(result1.saltBase64).isNotEmpty()
        assertThat(result1.iterations).isEqualTo(PinHasher.ITERATIONS)

        // Different salts should produce completely different hashes
        assertThat(result1.saltBase64).isNotEqualTo(result2.saltBase64)
        assertThat(result1.hashBase64).isNotEqualTo(result2.hashBase64)
    }

    @Test
    fun `verifyPin returns true for correct pin and false for incorrect pin`() {
        val correctPin = "937402".toCharArray()
        val wrongPin = "937403".toCharArray()

        val result = PinHasher.hashPin(correctPin)

        val isValid = PinHasher.verifyPin(correctPin, result.hashBase64, result.saltBase64)
        val isInvalid = PinHasher.verifyPin(wrongPin, result.hashBase64, result.saltBase64)

        assertThat(isValid).isTrue()
        assertThat(isInvalid).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty pin throws IllegalArgumentException`() {
        PinHasher.hashPin(charArrayOf())
    }
}
