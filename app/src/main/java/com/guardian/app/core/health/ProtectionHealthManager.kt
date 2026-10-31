package com.guardian.app.core.health

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.os.PowerManager
import android.provider.Settings
import com.guardian.app.core.data.PreferencesManager
import com.guardian.app.service.GuardianAccessibilityService
import com.guardian.app.service.GuardianDeviceAdminReceiver

class ProtectionHealthManager(private val context: Context) {

    private val prefsManager = PreferencesManager(context)

    suspend fun evaluateHealth(): ProtectionHealthReport {
        val dnsHealth = evaluateDnsHealth()
        val accessibilityHealth = evaluateAccessibilityHealth()
        val deviceAdminHealth = evaluateDeviceAdminHealth()
        val batteryHealth = evaluateBatteryHealth()

        val overallLevel = when {
            dnsHealth.state == ComponentState.ACTIVE &&
                    deviceAdminHealth.state == ComponentState.ACTIVE &&
                    accessibilityHealth.state == ComponentState.ACTIVE -> ProtectionLevel.SHIELDED

            dnsHealth.state == ComponentState.ACTIVE ||
                    (dnsHealth.isSystemPrivateDnsConfigured && dnsHealth.isDnsResolutionWorking) -> ProtectionLevel.DEGRADED

            else -> ProtectionLevel.AT_RISK
        }

        return ProtectionHealthReport(
            overallLevel = overallLevel,
            dnsHealth = dnsHealth,
            accessibilityHealth = accessibilityHealth,
            deviceAdminHealth = deviceAdminHealth,
            batteryHealth = batteryHealth
        )
    }

    private suspend fun evaluateDnsHealth(): DnsHealth {
        val contentResolver = context.contentResolver
        val privateDnsMode = Settings.Global.getString(contentResolver, "private_dns_mode")
        val privateDnsSpecifier = Settings.Global.getString(contentResolver, "private_dns_specifier")

        val targetProfile = prefsManager.nextDnsProfileId
        val isConfigured = (privateDnsMode == "hostname") &&
                (!privateDnsSpecifier.isNullOrBlank() && (privateDnsSpecifier.contains("nextdns") || privateDnsSpecifier.contains(targetProfile)))

        val isResolutionWorking = NetworkDnsProbe.testBasicResolution()
        val isFilterVerified = if (isConfigured && isResolutionWorking) {
            NetworkDnsProbe.verifyNextDnsFiltering(targetProfile)
        } else {
            false
        }

        val message = when {
            isConfigured && isResolutionWorking && isFilterVerified ->
                "Private DNS is locked to NextDNS and active filtering is verified."
            isConfigured && isResolutionWorking ->
                "Private DNS hostname is configured, but live NextDNS filtering handshake could not be confirmed."
            isConfigured && !isResolutionWorking ->
                "Private DNS is configured but network resolution is failing (no internet or invalid hostname)."
            privateDnsMode == "opportunistic" ->
                "Private DNS is in Automatic mode. Adult domain filtering is NOT enforced."
            else ->
                "Private DNS is currently disabled. Adult domain filtering is OFF."
        }

        return DnsHealth(
            isSystemPrivateDnsConfigured = isConfigured,
            configuredHostname = privateDnsSpecifier,
            isDnsResolutionWorking = isResolutionWorking,
            isNextDnsFilterVerified = isFilterVerified,
            detailMessage = message
        )
    }

    private fun evaluateAccessibilityHealth(): AccessibilityHealth {
        val isServiceRunning = GuardianAccessibilityService.isRunning
        val hasPermission = isAccessibilityServiceEnabled(context, GuardianAccessibilityService::class.java)

        val message = when {
            isServiceRunning -> "Accessibility watchdog is actively monitoring settings and browser address bars."
            hasPermission -> "Accessibility was granted but the background service was terminated by HyperOS battery management."
            else -> "Accessibility permission is not granted. (Requires 'Allow restricted settings' in HyperOS App Info)."
        }

        return AccessibilityHealth(
            isServiceRunning = isServiceRunning,
            hasPermissionGranted = hasPermission,
            isHyperOsRestricted = !hasPermission,
            detailMessage = message
        )
    }

    private fun evaluateDeviceAdminHealth(): DeviceAdminHealth {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
        val isAdmin = dpm.isAdminActive(adminComponent)

        val message = if (isAdmin) {
            "Device Administrator is active. Prevents casual uninstallation from home screen."
        } else {
            "Device Administrator is inactive. The app can be uninstalled without authorization."
        }

        return DeviceAdminHealth(
            isAdminActive = isAdmin,
            detailMessage = message
        )
    }

    private fun evaluateBatteryHealth(): BatteryHealth {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        val message = if (isIgnoring) {
            "Battery optimization is set to Unrestricted."
        } else {
            "Battery optimization is active. HyperOS may terminate protection services after inactivity."
        }

        return BatteryHealth(
            isIgnoringOptimizations = isIgnoring,
            detailMessage = message
        )
    }

    companion object {
        fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
            val expectedComponentName = ComponentName(context, serviceClass).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServices.split(":").any { it.equals(expectedComponentName, ignoreCase = true) }
        }
    }
}
