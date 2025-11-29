package com.example.focusguard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusguard.FocusGuardApp
import com.example.focusguard.domain.model.AppInfo
import com.example.focusguard.domain.usecase.GetAllowDurationUseCase
import com.example.focusguard.domain.usecase.GetBlockedAppsUseCase
import com.example.focusguard.domain.usecase.GetFrictionSentenceUseCase
import com.example.focusguard.domain.usecase.GetGlobalServiceStateUseCase
import com.example.focusguard.domain.usecase.GetInstalledAppsUseCase
import com.example.focusguard.domain.usecase.ToggleAppBlockUseCase
import com.example.focusguard.domain.usecase.ToggleGlobalServiceStateUseCase
import com.example.focusguard.domain.usecase.UpdateAllowDurationUseCase
import com.example.focusguard.domain.usecase.UpdateFrictionSentenceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<FocusGuardApp>().repository

    // Use Cases
    private val getInstalledAppsUseCase = GetInstalledAppsUseCase(repository)
    private val getBlockedAppsUseCase = GetBlockedAppsUseCase(repository)
    private val toggleAppBlockUseCase = ToggleAppBlockUseCase(repository)
    private val getFrictionSentenceUseCase = GetFrictionSentenceUseCase(repository)
    private val updateFrictionSentenceUseCase = UpdateFrictionSentenceUseCase(repository)
    private val getAllowDurationUseCase = GetAllowDurationUseCase(repository)
    private val updateAllowDurationUseCase = UpdateAllowDurationUseCase(repository)
    private val getGlobalServiceStateUseCase = GetGlobalServiceStateUseCase(repository)
    private val toggleGlobalServiceStateUseCase = ToggleGlobalServiceStateUseCase(repository)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

    val uiState: StateFlow<List<AppInfo>> =
            combine(_installedApps, getBlockedAppsUseCase()) { apps, blocked ->
                        apps.map { app -> app.copy(isBlocked = blocked.contains(app.packageName)) }
                    }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val frictionSentence =
            getFrictionSentenceUseCase()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val allowDuration =
            getAllowDurationUseCase()
                    .stateIn(
                            viewModelScope,
                            SharingStarted.WhileSubscribed(5000),
                            60000L // Default fallback, though repository handles defaults
                    )

    val isServiceEnabled =
            getGlobalServiceStateUseCase()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch { _installedApps.value = getInstalledAppsUseCase() }
    }

    fun toggleAppBlock(packageName: String, currentBlocked: Boolean) {
        viewModelScope.launch { toggleAppBlockUseCase(packageName, currentBlocked) }
    }

    fun updateFrictionSentence(sentence: String) {
        viewModelScope.launch { updateFrictionSentenceUseCase(sentence) }
    }

    fun updateAllowDuration(duration: Long) {
        viewModelScope.launch { updateAllowDurationUseCase(duration) }
    }

    fun toggleServiceEnabled(enabled: Boolean) {
        viewModelScope.launch { toggleGlobalServiceStateUseCase(enabled) }
    }
}
