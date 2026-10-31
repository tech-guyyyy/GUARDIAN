package com.guardian.app.core.health

enum class ProtectionLevel {
    SHIELDED,
    DEGRADED,
    AT_RISK
}

enum class ComponentState {
    ACTIVE,
    DEGRADED,
    INACTIVE,
    NOT_CONFIGURED
}

data class DnsHealth(
    val isSystemPrivateDnsConfigured: Boolean,
    val configuredHostname: String?,
    val isDnsResolutionWorking: Boolean,
    val isNextDnsFilterVerified: Boolean,
    val detailMessage: String
) {
    val state: ComponentState
        get() = when {
            isSystemPrivateDnsConfigured && isDnsResolutionWorking && isNextDnsFilterVerified -> ComponentState.ACTIVE
            isSystemPrivateDnsConfigured && isDnsResolutionWorking -> ComponentState.DEGRADED
            else -> ComponentState.INACTIVE
        }
}

data class AccessibilityHealth(
    val isServiceRunning: Boolean,
    val hasPermissionGranted: Boolean,
    val isHyperOsRestricted: Boolean,
    val detailMessage: String
) {
    val state: ComponentState
        get() = when {
            isServiceRunning -> ComponentState.ACTIVE
            hasPermissionGranted -> ComponentState.DEGRADED // Granted but killed by OS
            else -> ComponentState.INACTIVE
        }
}

data class DeviceAdminHealth(
    val isAdminActive: Boolean,
    val detailMessage: String
) {
    val state: ComponentState
        get() = if (isAdminActive) ComponentState.ACTIVE else ComponentState.INACTIVE
}

data class BatteryHealth(
    val isIgnoringOptimizations: Boolean,
    val detailMessage: String
) {
    val state: ComponentState
        get() = if (isIgnoringOptimizations) ComponentState.ACTIVE else ComponentState.DEGRADED
}

data class ProtectionHealthReport(
    val overallLevel: ProtectionLevel,
    val dnsHealth: DnsHealth,
    val accessibilityHealth: AccessibilityHealth,
    val deviceAdminHealth: DeviceAdminHealth,
    val batteryHealth: BatteryHealth,
    val timestampMs: Long = System.currentTimeMillis()
)
