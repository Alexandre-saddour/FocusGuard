package com.example.focusguard.domain

import com.example.focusguard.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface FocusRepository {
    val blockedPackages: Flow<Set<String>>
    val frictionSentence: Flow<String>
    val allowDuration: Flow<Long>
    val isServiceEnabled: Flow<Boolean>

    suspend fun addBlockedPackage(packageName: String)
    suspend fun removeBlockedPackage(packageName: String)
    suspend fun setFrictionSentence(sentence: String)
    suspend fun setAllowDuration(duration: Long)
    suspend fun setServiceEnabled(enabled: Boolean)

    suspend fun getInstalledApps(): List<AppInfo>

    fun isPackageAllowed(packageName: String): Boolean
    fun temporarilyAllowPackage(packageName: String, durationMs: Long)
}
