package com.guardian.app.ui.blocklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.app.GuardianApplication
import com.guardian.app.core.model.BlockedDomain
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlocklistViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = GuardianApplication.instance.database.blockedDomainDao()

    val domains: StateFlow<List<BlockedDomain>> = dao.getAllDomainsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addDomain(rawInput: String, onComplete: (Boolean) -> Unit) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            onComplete(false)
            return
        }

        val normalized = BlockedDomain.normalize(trimmed)
        viewModelScope.launch {
            val exists = dao.isDomainBlocked(normalized)
            if (!exists) {
                dao.insertDomain(
                    BlockedDomain(
                        rawInput = trimmed,
                        normalizedDomain = normalized
                    )
                )
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun removeDomain(domain: BlockedDomain) {
        viewModelScope.launch {
            dao.deleteDomain(domain)
        }
    }
}
