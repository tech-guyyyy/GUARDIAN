package com.guardian.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.guardian.app.core.health.ProtectionHealthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles device reboot recovery.
 * Verifies if HyperOS preserved the accessibility service and Private DNS configuration.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val healthManager = ProtectionHealthManager(context)
                    // Trigger evaluation to ensure state is ready and logged
                    healthManager.evaluateHealth()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
