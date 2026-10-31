package com.guardian.app.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Non-destructive Device Administrator Receiver.
 * Strictly avoids declaring wipe-data or password-reset capabilities.
 * Serves only to provide uninstallation friction.
 */
class GuardianDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "GUARDIAN Device Administrator activated.", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Deactivating GUARDIAN Device Administrator requires the trusted person's authorization PIN. Removing this permission removes uninstall protection."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "GUARDIAN Device Administrator deactivated.", Toast.LENGTH_LONG).show()
    }
}
