package com.money.tracker.budget.planner.moneymanager.notiSpecial

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.money.tracker.budget.planner.moneymanager.utils.NotificationPermissionHelper

class UserPresentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        val appContext = context.applicationContext
        UnlockNotifier.handleUserPresent(appContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            NotificationPermissionHelper.startWakeServiceSafely(appContext)
        }
    }
}
