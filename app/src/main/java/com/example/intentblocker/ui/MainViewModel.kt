package com.example.intentblocker.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.intentblocker.data.AppPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppInfo(
    val packageName: String,
    val label: String,
    val isBlocked: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appPrefs = AppPrefs(application)
    private val packageManager = application.packageManager

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    
    val uiState: StateFlow<List<AppInfo>> = combine(
        _installedApps,
        appPrefs.blockedPackages
    ) { apps, blocked ->
        apps.map { app ->
            app.copy(isBlocked = blocked.contains(app.packageName))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val frictionSentence = appPrefs.frictionSentence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val allowDuration = appPrefs.allowDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPrefs.DEFAULT_DURATION)

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            }
            val apps = packages
                .mapNotNull { packageInfo ->
                    packageInfo.applicationInfo?.let { appInfo ->
                        // Filter out system apps and our own app
                        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) &&
                            packageInfo.packageName != getApplication<Application>().packageName) {
                            AppInfo(
                                packageName = packageInfo.packageName,
                                label = appInfo.loadLabel(packageManager).toString(),
                                isBlocked = false // Will be updated by combine
                            )
                        } else {
                            null
                        }
                    }
                }
                .sortedBy { it.label }
            
            _installedApps.value = apps
        }
    }

    fun toggleAppBlock(packageName: String, currentBlocked: Boolean) {
        viewModelScope.launch {
            if (currentBlocked) {
                appPrefs.removeBlockedPackage(packageName)
            } else {
                appPrefs.addBlockedPackage(packageName)
            }
        }
    }

    fun updateFrictionSentence(sentence: String) {
        viewModelScope.launch {
            appPrefs.setFrictionSentence(sentence)
        }
    }

    fun updateAllowDuration(duration: Long) {
        viewModelScope.launch {
            appPrefs.setAllowDuration(duration)
        }
    }
}
