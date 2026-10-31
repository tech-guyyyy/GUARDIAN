package com.guardian.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.guardian.app.core.crypto.LockoutPolicy
import com.guardian.app.core.data.PreferencesManager
import com.guardian.app.ui.theme.*

@Composable
fun PinAuthDialog(
    title: String = "Authorization Required",
    description: String = "Enter the trusted person's master PIN to proceed.",
    onAuthorized: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutStatus by remember { mutableStateOf(prefsManager.loadLockoutPolicy().getStatus()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = ShieldBlue,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (lockoutStatus.isLockedOut) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusInactiveBg, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusInactive)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Too many failed attempts. Locked for ${lockoutStatus.remainingCooldownSeconds}s",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 12) pinInput = it },
                        label = { Text("Trusted Person PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ShieldBlue,
                            unfocusedBorderColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = StatusInactive,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        enabled = !lockoutStatus.isLockedOut && pinInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ShieldBlue),
                        onClick = {
                            val policy = prefsManager.loadLockoutPolicy()
                            val status = policy.getStatus()
                            if (status.isLockedOut) {
                                lockoutStatus = status
                                return@Button
                            }

                            val isValid = prefsManager.verifyPin(pinInput.toCharArray())
                            if (isValid) {
                                policy.recordSuccessfulAuth()
                                prefsManager.saveLockoutPolicy(policy)
                                onAuthorized()
                            } else {
                                val updatedStatus = policy.recordFailedAttempt()
                                prefsManager.saveLockoutPolicy(policy)
                                lockoutStatus = updatedStatus
                                errorMessage = if (updatedStatus.isLockedOut) {
                                    "Locked out for ${updatedStatus.remainingCooldownSeconds} seconds."
                                } else {
                                    "Incorrect PIN. (${updatedStatus.failedAttemptsCount} failed attempts)"
                                }
                                pinInput = ""
                            }
                        }
                    ) {
                        Text("Authorize", color = TextPrimary)
                    }
                }
            }
        }
    }
}
