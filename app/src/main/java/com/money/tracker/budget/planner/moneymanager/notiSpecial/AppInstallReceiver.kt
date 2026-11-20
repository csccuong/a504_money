package com.money.tracker.budget.planner.moneymanager.notiSpecial

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

class AppInstallReceiver : BroadcastReceiver() {

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val appIcon: Drawable?
    )

    override fun onReceive(context: Context, intent: Intent) {
        val analytics = FirebaseAnalytics.getInstance(context)
        when (val action = intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                analytics.logEvent("event_show_noti_install_app_new", null)
                handleAppInstalled(context, intent)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                analytics.logEvent("event_show_noti_replace_app_new", null)
                handleAppReplaced(context, intent)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                analytics.logEvent("event_show_noti_remove_app_new", null)
                handleAppRemoved(context, intent)
            }
            else -> Log.d(TAG, "Ignored action: $action")
        }
    }

    private fun handleAppInstalled(context: Context, intent: Intent) {
        runCatching {
            val packageName = intent.data?.schemeSpecificPart ?: return
            val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            if (isReplacing) return

            val appInfo = resolveAppInfo(context, packageName) ?: return
            NotificationUtil.showAppInstallNotification(context, appInfo)
        }.onFailure {
            Log.e(TAG, "Error handling app install", it)
        }
    }

    private fun handleAppReplaced(context: Context, intent: Intent) {
        runCatching {
            val packageName = intent.data?.schemeSpecificPart ?: return
            val appInfo = resolveAppInfo(context, packageName) ?: return
            NotificationUtil.showAppUpdatedNotification(context, appInfo)
        }.onFailure {
            Log.e(TAG, "Error handling app update", it)
        }
    }

    private fun handleAppRemoved(context: Context, intent: Intent) {
        runCatching {
            val packageName = intent.data?.schemeSpecificPart ?: return
            val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            if (isReplacing) return

            val appInfo = resolveAppInfo(context, packageName)
                ?: AppInfo(packageName = packageName, appName = packageName, appIcon = null)
            NotificationUtil.showAppRemovedNotification(context, appInfo)
        }.onFailure {
            Log.e(TAG, "Error handling app removal", it)
        }
    }

    private fun resolveAppInfo(context: Context, packageName: String): AppInfo? = runCatching {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        AppInfo(
            packageName = packageName,
            appName = pm.getApplicationLabel(info).toString(),
            appIcon = pm.getApplicationIcon(info)
        )
    }.getOrNull()

    private companion object {
        const val TAG = "AppInstallReceiver"
    }
}
