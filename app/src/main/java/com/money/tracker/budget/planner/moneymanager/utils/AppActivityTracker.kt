package com.money.tracker.budget.planner.moneymanager.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.money.tracker.budget.planner.moneymanager.ui.common.SystemBarsHelper
import java.lang.ref.WeakReference

object AppActivityTracker : Application.ActivityLifecycleCallbacks {

    private var currentActivityRef: WeakReference<Activity>? = null

    val currentActivity: Activity?
        get() = currentActivityRef?.get()

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        SystemBarsHelper.applyImmersiveSticky(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        SystemBarsHelper.applyImmersiveSticky(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        SystemBarsHelper.applyImmersiveSticky(activity)
    }

    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
