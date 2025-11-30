package com.flowdevproduction.focusguard.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import com.flowdevproduction.focusguard.domain.FocusRepository
import com.flowdevproduction.focusguard.domain.model.AppInfo
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusRepositoryImpl
@Inject
constructor(private val context: Context, private val appPrefs: AppPrefs) : FocusRepository {

    private val packageManager = context.packageManager
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val blockedPackages: Flow<Set<String>> = appPrefs.blockedPackages
    override val frictionSentence: Flow<String> = appPrefs.frictionSentence
    override val allowDuration: Flow<Long> = appPrefs.allowDuration
    override val isServiceEnabled: Flow<Boolean> = appPrefs.isServiceEnabled
    override val isAnalyticsEnabled: Flow<Boolean?> = appPrefs.isAnalyticsEnabled

    override suspend fun addBlockedPackage(packageName: String) =
            appPrefs.addBlockedPackage(packageName)

    override suspend fun removeBlockedPackage(packageName: String) =
            appPrefs.removeBlockedPackage(packageName)

    override suspend fun setFrictionSentence(sentence: String) =
            appPrefs.setFrictionSentence(sentence)

    override suspend fun setAllowDuration(duration: Long) = appPrefs.setAllowDuration(duration)
    override suspend fun setServiceEnabled(enabled: Boolean) = appPrefs.setServiceEnabled(enabled)
    override suspend fun setAnalyticsEnabled(enabled: Boolean) =
            appPrefs.setAnalyticsEnabled(enabled)

    override suspend fun getInstalledApps(): List<AppInfo> {
        val packages =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getInstalledPackages(
                            PackageManager.PackageInfoFlags.of(
                                    PackageManager.GET_META_DATA.toLong()
                            )
                    )
                } else {
                    packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
                }

        return packages
                .mapNotNull { packageInfo ->
                    packageInfo.applicationInfo?.let { appInfo ->
                        // Filter out system apps and our own app
                        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) &&
                                        packageInfo.packageName != context.packageName
                        ) {
                            AppInfo(
                                    packageName = packageInfo.packageName,
                                    label = appInfo.loadLabel(packageManager).toString(),
                                    isBlocked = false
                            )
                        } else {
                            null
                        }
                    }
                }
                .sortedBy { it.label }
    }

    override fun isPackageAllowed(packageName: String): Boolean {
        // Use runBlocking for synchronous access - this is called from service
        val allowed =
                kotlinx.coroutines.runBlocking {
                    val allowedPackages = appPrefs.allowedPackages.first()
                    val startTime = allowedPackages[packageName] ?: return@runBlocking false
                    val currentAllowDuration = appPrefs.allowDuration.first()
                    val elapsed = SystemClock.elapsedRealtime() - startTime

                    if (elapsed >= currentAllowDuration) {
                        // Remove expired entry
                        val updated = allowedPackages.toMutableMap().apply { remove(packageName) }
                        appPrefs.setAllowedPackages(updated)
                        false
                    } else {
                        true
                    }
                }
        return allowed
    }

    override fun temporarilyAllowPackage(packageName: String, durationMs: Long) {
        repositoryScope.launch {
            val currentAllowed = appPrefs.allowedPackages.first().toMutableMap()
            currentAllowed[packageName] = SystemClock.elapsedRealtime()
            appPrefs.setAllowedPackages(currentAllowed)
        }
    }
}
