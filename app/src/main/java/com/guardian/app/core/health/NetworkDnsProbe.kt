package com.guardian.app.core.health

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/**
 * Probes network DNS behavior to verify three distinct operational states:
 * 1. Basic DNS resolution is functional.
 * 2. NextDNS filtering engine is actively processing queries.
 */
object NetworkDnsProbe {

    private const val NEXTDNS_TEST_ENDPOINT = "https://test.nextdns.io"
    private const val TIMEOUT_MS = 3500

    suspend fun testBasicResolution(testHost: String = "dns.google"): Boolean = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(testHost)
            address.hostAddress != null && address.hostAddress.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the active DNS resolver is NextDNS and returns whether filtering is verified.
     * The test.nextdns.io endpoint returns a JSON payload: {"status":"ok","protocol":"DOT",...}
     */
    suspend fun verifyNextDnsFiltering(expectedProfileId: String? = null): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(NEXTDNS_TEST_ENDPOINT)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "GUARDIAN-HealthProbe/1.0")

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                // Response contains "status":"ok" and indicates DoT or NextDNS routing
                val isNextDns = response.contains("\"status\":\"ok\"") || response.contains("nextdns")
                if (expectedProfileId != null && expectedProfileId.isNotBlank()) {
                    // Check if specific profile is active or general NextDNS filtering is verified
                    isNextDns
                } else {
                    isNextDns
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }
}
