package com.example.focusguard.domain.usecase

import com.example.focusguard.domain.FocusRepository
import com.example.focusguard.domain.model.AppInfo
import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(private val repository: FocusRepository) {
    suspend operator fun invoke(): List<AppInfo> = repository.getInstalledApps()
}

class CheckAppUsageUseCase @Inject constructor(private val repository: FocusRepository) {
    fun isPackageAllowed(packageName: String): Boolean = repository.isPackageAllowed(packageName)

    fun temporarilyAllowPackage(packageName: String, durationMs: Long) {
        repository.temporarilyAllowPackage(packageName, durationMs)
    }
}
