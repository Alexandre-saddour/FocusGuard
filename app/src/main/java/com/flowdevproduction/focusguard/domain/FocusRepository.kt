package com.flowdevproduction.focusguard.domain

import com.flowdevproduction.focusguard.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface FocusRepository {
    val blockedPackages: Flow<Set<String>>
    val frictionSentence: Flow<String>
    val allowDuration: Flow<Long>
    val isServiceEnabled: Flow<Boolean>
    val isAnalyticsEnabled: Flow<Boolean?>

    suspend fun addBlockedPackage(packageName: String)
    suspend fun removeBlockedPackage(packageName: String)
    suspend fun setFrictionSentence(sentence: String)
    suspend fun setAllowDuration(duration: Long)
    suspend fun setServiceEnabled(enabled: Boolean)
    suspend fun setAnalyticsEnabled(enabled: Boolean)

    suspend fun getInstalledApps(): List<AppInfo>

    fun isPackageAllowed(packageName: String): Boolean
    fun temporarilyAllowPackage(packageName: String, durationMs: Long)
}
