package com.guardian.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.guardian.app.ui.blocklist.BlocklistScreen
import com.guardian.app.ui.dashboard.DashboardScreen
import com.guardian.app.ui.setup.SetupGuideScreen
import com.guardian.app.ui.theme.GuardianTheme

enum class Screen {
    DASHBOARD,
    BLOCKLIST,
    SETUP_GUIDE
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GuardianTheme {
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                when (currentScreen) {
                    Screen.DASHBOARD -> {
                        DashboardScreen(
                            onNavigateToBlocklist = { currentScreen = Screen.BLOCKLIST },
                            onNavigateToGuide = { currentScreen = Screen.SETUP_GUIDE }
                        )
                    }
                    Screen.BLOCKLIST -> {
                        BlocklistScreen(
                            onNavigateBack = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                    Screen.SETUP_GUIDE -> {
                        SetupGuideScreen(
                            onNavigateBack = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                }
            }
        }
    }
}
