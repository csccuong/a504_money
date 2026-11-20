package com.money.tracker.budget.planner.moneymanager.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.money.tracker.budget.planner.moneymanager.notiSpecial.WakeService
import java.lang.ref.WeakReference

object NotificationPermissionHelper {

    private const val CHECK_INTERVAL_MS = 300L
    private const val MAX_CHECK_ATTEMPTS = 200 // ~60s upper bound to avoid infinite polling

    fun isNotificationPermissionGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun startWakeServiceSafely(context: Context) {
        runCatching {
            val serviceIntent = Intent(context.applicationContext, WakeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.applicationContext.startForegroundService(serviceIntent)
            } else {
                context.applicationContext.startService(serviceIntent)
            }
        }
    }

    fun openNotificationSettings(activity: Activity, nextActivityClass: Class<*>) {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            } else {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("app_package", activity.packageName)
                putExtra("app_uid", activity.applicationInfo.uid)
            }
        }

        try {
            activity.startActivity(intent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(fallbackIntent)
        }

        pollNotificationPermission(activity, nextActivityClass)
    }

    private fun pollNotificationPermission(activity: Activity, nextActivityClass: Class<*>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val activityRef = WeakReference(activity)
        val handler = Handler(Looper.getMainLooper())
        var attempts = 0

        val checkTask = object : Runnable {
            override fun run() {
                val host = activityRef.get()
                if (host == null || host.isDestroyed || host.isFinishing || attempts++ >= MAX_CHECK_ATTEMPTS) {
                    return
                }
                if (isNotificationPermissionGranted(host)) {
                    startWakeServiceSafely(host)
                    val backIntent = Intent(host, nextActivityClass).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    host.startActivity(backIntent)
                } else {
                    handler.postDelayed(this, CHECK_INTERVAL_MS)
                }
            }
        }

        handler.postDelayed(checkTask, CHECK_INTERVAL_MS)
    }
}
