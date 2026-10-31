package com.guardian.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.guardian.app.GuardianApplication
import com.guardian.app.core.model.BlockedDomain
import com.guardian.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Best-effort accessibility watchdog.
 *
 * Scope:
 * 1. Settings Anti-Tamper: Detects navigation into app management or device admin settings for GUARDIAN.
 * 2. Supplementary Browser URL Bar Inspection: Inspects address bar nodes in supported browsers.
 *
 * Note: DNS filtering via NextDNS is the primary shield. This accessibility layer is supplementary.
 */
class GuardianAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val browserPackages = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.duckduckgo.mobile.android"
    )

    private val settingsPackage = "com.android.settings"

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100L
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName == settingsPackage) {
            handleSettingsEvent(event)
            return
        }

        if (browserPackages.contains(packageName)) {
            handleBrowserEvent(event)
        }
    }

    private fun handleSettingsEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        try {
            // Check if user is navigating to GUARDIAN app details or uninstall screen in settings
            val guardianNodes = rootNode.findAccessibilityNodeInfosByText("GUARDIAN")
            val uninstallNodes = rootNode.findAccessibilityNodeInfosByText("Uninstall")
            val deactivateNodes = rootNode.findAccessibilityNodeInfosByText("Deactivate")

            if (guardianNodes.isNotEmpty() && (uninstallNodes.isNotEmpty() || deactivateNodes.isNotEmpty())) {
                // Intercept navigation by triggering Home/Back and showing authorization requirement
                performGlobalAction(GLOBAL_ACTION_BACK)
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("ACTION_AUTH_REQUIRED", true)
                }
                startActivity(intent)
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun handleBrowserEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        try {
            val urlNode = findUrlBarNode(rootNode)
            val rawText = urlNode?.text?.toString() ?: return

            if (rawText.isBlank()) return
            val normalizedUrl = BlockedDomain.normalize(rawText)

            serviceScope.launch {
                val db = GuardianApplication.instance.database
                val isBlocked = db.blockedDomainDao().isDomainBlocked(normalizedUrl)

                if (isBlocked) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun findUrlBarNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val viewId = current.viewIdResourceName?.lowercase() ?: ""

            if (viewId.contains("url_bar") || viewId.contains("location_bar") || viewId.contains("search_box") || viewId.contains("toolbar")) {
                if (!current.text.isNullOrBlank()) {
                    return current
                }
            }

            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
