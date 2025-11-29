package com.example.intentblocker.service

import android.os.SystemClock

object BlockManager {
    private val allowedPackages = mutableMapOf<String, Long>()
    private const val DEFAULT_ALLOW_DURATION_MS = 60000L // 1 minute allow window

    fun isPackageAllowed(packageName: String): Boolean {
        val expiry = allowedPackages[packageName] ?: return false
        if (SystemClock.elapsedRealtime() > expiry) {
            allowedPackages.remove(packageName)
            return false
        }
        return true
    }

    fun temporarilyAllowPackage(packageName: String, durationMs: Long = DEFAULT_ALLOW_DURATION_MS) {
        allowedPackages[packageName] = SystemClock.elapsedRealtime() + durationMs
    }
}
