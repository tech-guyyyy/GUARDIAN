package com.guardian.app.ui.dashboard

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guardian.app.core.health.ComponentState
import com.guardian.app.core.health.ProtectionLevel
import com.guardian.app.service.GuardianDeviceAdminReceiver
import com.guardian.app.ui.auth.PinAuthDialog
import com.guardian.app.ui.auth.PinSetupDialog
import com.guardian.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToBlocklist: () -> Unit,
    onNavigateToGuide: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val healthReport by viewModel.healthReport.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showPinSetup by remember { mutableStateOf(!viewModel.isPinConfigured) }
    var showAuthDialogForBlocklist by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ShieldBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "GUARDIAN",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshHealth() }, enabled = !isRefreshing) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Health",
                            tint = TextPrimary
                        )
                    }
                    IconButton(onClick = onNavigateToGuide) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "HyperOS Guide",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Overall Shield Banner
                val overall = healthReport?.overallLevel ?: ProtectionLevel.DEGRADED
                OverallStatusCard(overall, isRefreshing)
            }

            item {
                Text(
                    text = "Protection Subsystems",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Layer 1: DNS
            item {
                val dns = healthReport?.dnsHealth
                SubsystemCard(
                    title = "Private DNS (NextDNS DoT)",
                    badge = "PRIMARY FILTER",
                    state = dns?.state ?: ComponentState.NOT_CONFIGURED,
                    description = dns?.detailMessage ?: "Checking DNS status...",
                    actionLabel = "Configure DNS",
                    onAction = {
                        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Layer 2: Device Admin
            item {
                val admin = healthReport?.deviceAdminHealth
                SubsystemCard(
                    title = "Device Administrator",
                    badge = "UNINSTALL FRICTION",
                    state = admin?.state ?: ComponentState.NOT_CONFIGURED,
                    description = admin?.detailMessage ?: "Checking admin status...",
                    actionLabel = if (admin?.isAdminActive == true) "Active" else "Activate Admin",
                    actionEnabled = admin?.isAdminActive != true,
                    onAction = {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(
                                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                ComponentName(context, GuardianDeviceAdminReceiver::class.java)
                            )
                            putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "GUARDIAN requires Device Admin privileges to prevent unauthorized uninstallation."
                            )
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Layer 3: Accessibility
            item {
                val acc = healthReport?.accessibilityHealth
                SubsystemCard(
                    title = "Accessibility Guard",
                    badge = "SUPPLEMENTARY",
                    state = acc?.state ?: ComponentState.NOT_CONFIGURED,
                    description = acc?.detailMessage ?: "Checking accessibility...",
                    actionLabel = "Open Accessibility",
                    onAction = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Layer 4: Battery Optimization
            item {
                val battery = healthReport?.batteryHealth
                SubsystemCard(
                    title = "HyperOS Battery Exemption",
                    badge = "PERSISTENCE",
                    state = battery?.state ?: ComponentState.NOT_CONFIGURED,
                    description = battery?.detailMessage ?: "Checking battery saver...",
                    actionLabel = "Set Unrestricted",
                    onAction = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(fallback)
                        }
                    }
                )
            }

            // Custom Blocklist Navigation Button
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Custom Blocklist",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "PIN authorization required to remove blocked domains.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                if (viewModel.isPinConfigured) {
                                    showAuthDialogForBlocklist = true
                                } else {
                                    showPinSetup = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ShieldBlue)
                        ) {
                            Text("Manage")
                        }
                    }
                }
            }

            item {
                // Honest Security Boundary Statement
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Security Boundary Notice",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Private DNS is the core filtering barrier. Device Admin and Accessibility provide strong friction against impulsive tampering, but cannot completely prevent factory reset or safe-mode uninstallation on unrooted devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showPinSetup) {
        PinSetupDialog(
            onPinConfigured = {
                showPinSetup = false
                viewModel.refreshHealth()
            },
            onDismiss = {
                if (viewModel.isPinConfigured) {
                    showPinSetup = false
                }
            }
        )
    }

    if (showAuthDialogForBlocklist) {
        PinAuthDialog(
            title = "Unlock Blocklist",
            description = "Enter trusted person PIN to view and modify protected blocklists.",
            onAuthorized = {
                showAuthDialogForBlocklist = false
                onNavigateToBlocklist()
            },
            onDismiss = { showAuthDialogForBlocklist = false }
        )
    }
}

@Composable
fun OverallStatusCard(level: ProtectionLevel, isRefreshing: Boolean) {
    val (bgColor, borderColor, icon, title, description) = when (level) {
        ProtectionLevel.SHIELDED -> Quintuple(
            StatusActiveBg.copy(alpha = 0.4f),
            StatusActive,
            Icons.Default.CheckCircle,
            "SYSTEM SHIELDED",
            "Private DNS is active with verified filtering, and tamper watchdogs are operational."
        )
        ProtectionLevel.DEGRADED -> Quintuple(
            StatusDegradedBg.copy(alpha = 0.4f),
            StatusDegraded,
            Icons.Default.Warning,
            "PROTECTION DEGRADED",
            "Core DNS resolution is functioning, but one or more anti-tamper subsystems require attention."
        )
        ProtectionLevel.AT_RISK -> Quintuple(
            StatusInactiveBg.copy(alpha = 0.4f),
            StatusInactive,
            Icons.Default.Error,
            "SYSTEM AT RISK",
            "Private DNS is disconnected or disabled. Adult domain filtering is currently offline."
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun SubsystemCard(
    title: String,
    badge: String,
    state: ComponentState,
    description: String,
    actionLabel: String,
    actionEnabled: Boolean = true,
    onAction: () -> Unit
) {
    val (indicatorColor, stateLabel) = when (state) {
        ComponentState.ACTIVE -> Pair(StatusActive, "ACTIVE")
        ComponentState.DEGRADED -> Pair(StatusDegraded, "DEGRADED")
        ComponentState.INACTIVE -> Pair(StatusInactive, "INACTIVE")
        ComponentState.NOT_CONFIGURED -> Pair(TextSecondary, "PENDING")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(indicatorColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: $stateLabel",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = indicatorColor
                )

                OutlinedButton(
                    onClick = onAction,
                    enabled = actionEnabled,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, ShieldBlue)
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
