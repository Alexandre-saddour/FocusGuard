package com.example.focusguard.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import com.example.focusguard.domain.FocusRepository
import com.example.focusguard.domain.model.AppInfo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class FocusRepositoryImpl
@Inject
constructor(private val context: Context, private val appPrefs: AppPrefs) : FocusRepository {

    private val packageManager = context.packageManager

    private val allowedPackages = mutableMapOf<String, Long>()

    override val blockedPackages: Flow<Set<String>> = appPrefs.blockedPackages
    override val frictionSentence: Flow<String> = appPrefs.frictionSentence
    override val allowDuration: Flow<Long> = appPrefs.allowDuration
    override val isServiceEnabled: Flow<Boolean> = appPrefs.isServiceEnabled

    override suspend fun addBlockedPackage(packageName: String) =
            appPrefs.addBlockedPackage(packageName)

    override suspend fun removeBlockedPackage(packageName: String) =
            appPrefs.removeBlockedPackage(packageName)

    override suspend fun setFrictionSentence(sentence: String) =
            appPrefs.setFrictionSentence(sentence)

    override suspend fun setAllowDuration(duration: Long) = appPrefs.setAllowDuration(duration)
    override suspend fun setServiceEnabled(enabled: Boolean) = appPrefs.setServiceEnabled(enabled)

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
        val expiry = allowedPackages[packageName] ?: return false
        if (SystemClock.elapsedRealtime() > expiry) {
            allowedPackages.remove(packageName)
            return false
        }
        return true
    }

    override fun temporarilyAllowPackage(packageName: String, durationMs: Long) {
        allowedPackages[packageName] = SystemClock.elapsedRealtime() + durationMs
    }
}
