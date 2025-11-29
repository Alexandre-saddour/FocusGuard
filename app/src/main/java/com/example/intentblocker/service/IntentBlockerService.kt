package com.example.intentblocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.intentblocker.data.AppPrefs
import com.example.intentblocker.ui.FrictionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IntentBlockerService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var appPrefs: AppPrefs
    private var blockedPackages: Set<String> = emptySet()
    
    // We need to track the last blocked package to avoid loops or repeated launches
    // for the same event sequence if the user hasn't passed friction yet.
    // However, for a simple MVP, we'll rely on the FrictionActivity being on top.

    override fun onServiceConnected() {
        super.onServiceConnected()
        appPrefs = AppPrefs(applicationContext)
        
        serviceScope.launch {
            appPrefs.blockedPackages.collectLatest {
                blockedPackages = it
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Don't block ourselves
            if (packageName == applicationContext.packageName) return

            if (blockedPackages.contains(packageName) && !BlockManager.isPackageAllowed(packageName)) {
                // Launch Friction Activity
                val intent = Intent(this, FrictionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    putExtra(FrictionActivity.EXTRA_TARGET_PACKAGE, packageName)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {
        // Handle interruption if needed
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel scope if needed, though service destruction usually kills the process eventually
    }
}
