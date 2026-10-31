package com.guardian.app.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardian.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(
    onNavigateBack: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val adbCommand = "adb shell pm grant com.guardian.app android.permission.WRITE_SECURE_SETTINGS"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HyperOS Setup Guide", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Xiaomi Redmi 13 (HyperOS) Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ShieldBlue
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Xiaomi's HyperOS enforces aggressive background task termination and blocks accessibility for sideloaded APKs by default. Follow these steps to ensure continuous protection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Step 1
            item {
                GuideStepCard(
                    stepNumber = "1",
                    title = "Bypass HyperOS 'Restricted Setting'",
                    description = "HyperOS flags sideloaded APKs when requesting Accessibility:\n" +
                            "• Go to phone Settings → Apps → Manage apps → GUARDIAN.\n" +
                            "• Tap the three vertical dots (⋮) in the top-right corner.\n" +
                            "• Tap 'Allow restricted settings'.\n" +
                            "• Authenticate with your phone fingerprint/pattern if prompted."
                )
            }

            // Step 2
            item {
                GuideStepCard(
                    stepNumber = "2",
                    title = "Enable Accessibility Watchdog",
                    description = "• Go to Settings → Additional settings → Accessibility.\n" +
                            "• Tap 'Downloaded apps' and select GUARDIAN.\n" +
                            "• Toggle 'Use GUARDIAN' to ON and accept the system notice."
                )
            }

            // Step 3
            item {
                GuideStepCard(
                    stepNumber = "3",
                    title = "Prevent HyperOS Process Killing",
                    description = "• Go to Settings → Apps → Manage apps → GUARDIAN.\n" +
                            "• Enable 'Autostart'.\n" +
                            "• Scroll down to 'Battery saver' and select 'No restrictions'.\n" +
                            "• Open your Recent Apps tray, press and hold GUARDIAN, then tap the Lock icon (padlock)."
                )
            }

            // Step 4
            item {
                GuideStepCard(
                    stepNumber = "4",
                    title = "Set System-Wide Private DNS (Primary Shield)",
                    description = "• Go to Settings → Connection & sharing → Private DNS.\n" +
                            "• Choose 'Private DNS provider hostname'.\n" +
                            "• Enter your NextDNS profile address (e.g. xxxxxx.dns.nextdns.io).\n" +
                            "• Tap Save. All network traffic across all browsers is now filtered by NextDNS."
                )
            }

            // Step 5
            item {
                GuideStepCard(
                    stepNumber = "5",
                    title = "Prevent Browser DNS Bypasses",
                    description = "Some browsers have their own internal DNS-over-HTTPS (DoH) which can bypass system DNS:\n" +
                            "• In Chrome: Settings → Privacy and security → Use secure DNS → set to 'Use current provider' or OFF.\n" +
                            "• Repeat for Brave or Firefox if applicable."
                )
            }

            // Step 6 (ADB)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusDegraded)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Optional: Lock Private DNS via ADB",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "To allow GUARDIAN to monitor and lock Private DNS programmatically, run this command via USB debugging from a PC:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = adbCommand,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(adbCommand))
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ShieldBlue)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun GuideStepCard(
    stepNumber: String,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ShieldBlue,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stepNumber,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
            }
        }
    }
}
