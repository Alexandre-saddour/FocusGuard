package com.example.intentblocker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.intentblocker.data.AppPrefs
import com.example.intentblocker.ui.FrictionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UsageMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var appPrefs: AppPrefs
    private var blockedPackages: Set<String> = emptySet()
    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false

    private val monitoringRunnable =
            object : Runnable {
                override fun run() {
                    checkForegroundApp()
                    if (isMonitoring) {
                        handler.postDelayed(this, POLLING_INTERVAL_MS)
                    }
                }
            }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "intent_blocker_service"
        private const val POLLING_INTERVAL_MS = 1000L // 1 second
    }

    override fun onCreate() {
        super.onCreate()
        appPrefs = AppPrefs(applicationContext)

        serviceScope.launch { appPrefs.blockedPackages.collectLatest { blockedPackages = it } }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isMonitoring) {
            isMonitoring = true
            handler.post(monitoringRunnable)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        handler.removeCallbacks(monitoringRunnable)
    }

    private fun checkForegroundApp() {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()

        // Query events from the last 2 seconds
        val events = usageStatsManager.queryEvents(currentTime - 2000, currentTime)
        var foregroundPackage: String? = null

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundPackage = event.packageName
            }
        }

        if (foregroundPackage != null &&
                        foregroundPackage != packageName &&
                        blockedPackages.contains(foregroundPackage) &&
                        !BlockManager.isPackageAllowed(foregroundPackage)
        ) {

            launchFrictionActivity(foregroundPackage)
        }
    }

    private fun launchFrictionActivity(packageName: String) {
        val intent =
                Intent(this, FrictionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    putExtra("target_package", packageName)
                }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                                    CHANNEL_ID,
                                    "Intent Blocker Service",
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply { description = "Monitors app usage to block distracting apps" }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Intent Blocker")
                .setContentText("Monitoring app usage")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
    }
}
