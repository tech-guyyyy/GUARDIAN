package com.guardian.app.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.app.core.data.PreferencesManager
import com.guardian.app.core.health.ProtectionHealthManager
import com.guardian.app.core.health.ProtectionHealthReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val healthManager = ProtectionHealthManager(application)
    private val prefsManager = PreferencesManager(application)

    private val _healthReport = MutableStateFlow<ProtectionHealthReport?>(null)
    val healthReport: StateFlow<ProtectionHealthReport?> = _healthReport.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val nextDnsProfileId: String
        get() = prefsManager.nextDnsProfileId

    val isPinConfigured: Boolean
        get() = prefsManager.isPinConfigured

    init {
        refreshHealth()
    }

    fun refreshHealth() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val report = healthManager.evaluateHealth()
                _healthReport.value = report
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun updateNextDnsProfile(newProfile: String) {
        prefsManager.nextDnsProfileId = newProfile
        refreshHealth()
    }
}
