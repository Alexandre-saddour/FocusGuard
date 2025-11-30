package com.flowdevproduction.focusguard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowdevproduction.focusguard.domain.model.AppInfo
import com.flowdevproduction.focusguard.domain.usecase.GetAllowDurationUseCase
import com.flowdevproduction.focusguard.domain.usecase.GetAnalyticsConsentUseCase
import com.flowdevproduction.focusguard.domain.usecase.GetBlockedAppsUseCase
import com.flowdevproduction.focusguard.domain.usecase.GetFrictionSentenceUseCase
import com.flowdevproduction.focusguard.domain.usecase.GetGlobalServiceStateUseCase
import com.flowdevproduction.focusguard.domain.usecase.GetInstalledAppsUseCase
import com.flowdevproduction.focusguard.domain.usecase.SetAnalyticsConsentUseCase
import com.flowdevproduction.focusguard.domain.usecase.ToggleAppBlockUseCase
import com.flowdevproduction.focusguard.domain.usecase.ToggleGlobalServiceStateUseCase
import com.flowdevproduction.focusguard.domain.usecase.UpdateAllowDurationUseCase
import com.flowdevproduction.focusguard.domain.usecase.UpdateFrictionSentenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
        application: Application,
        getFrictionSentenceUseCase: GetFrictionSentenceUseCase,
        getAllowDurationUseCase: GetAllowDurationUseCase,
        getGlobalServiceStateUseCase: GetGlobalServiceStateUseCase,
        getBlockedAppsUseCase: GetBlockedAppsUseCase,
        getAnalyticsConsentUseCase: GetAnalyticsConsentUseCase,
        private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
        private val toggleAppBlockUseCase: ToggleAppBlockUseCase,
        private val updateFrictionSentenceUseCase: UpdateFrictionSentenceUseCase,
        private val updateAllowDurationUseCase: UpdateAllowDurationUseCase,
        private val toggleGlobalServiceStateUseCase: ToggleGlobalServiceStateUseCase,
        private val setAnalyticsConsentUseCase: SetAnalyticsConsentUseCase
) : AndroidViewModel(application) {

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

    val isAnalyticsEnabled =
            getAnalyticsConsentUseCase()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch { setAnalyticsConsentUseCase(enabled) }
    }
}
