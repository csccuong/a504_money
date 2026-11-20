package com.money.tracker.budget.planner.moneymanager.ui.common

import android.app.Activity
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.money.tracker.budget.planner.moneymanager.R

object SystemBarsHelper {

    enum class StatusBarIconStyle {
        LIGHT, // White icons (for dark/navy headers)
        DARK   // Dark icons (for light/white backgrounds)
    }

    @Suppress("DEPRECATION")
    fun applySystemBars(
        activity: Activity,
        @ColorRes statusBarColorRes: Int,
        iconStyle: StatusBarIconStyle,
        @ColorRes navBarColorRes: Int = R.color.surface_card
    ) {
        val window = activity.window ?: return
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(activity, statusBarColorRes)
        window.navigationBarColor = ContextCompat.getColor(activity, navBarColorRes)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = (iconStyle == StatusBarIconStyle.DARK)
        controller.isAppearanceLightNavigationBars = true
        applyImmersiveSticky(activity)
    }

    fun setStatusBarColor(
        activity: Activity,
        @ColorRes colorResId: Int,
        iconStyle: StatusBarIconStyle
    ) {
        applySystemBars(activity, colorResId, iconStyle)
    }

    fun applyFullScreenFlag(activity: Activity) {
        applyImmersiveSticky(activity)
    }

    fun hideNavigationBar(activity: Activity) {
        val window = activity.window ?: return
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }

    fun applyImmersiveSticky(activity: Activity) {
        val window = activity.window ?: return
        applyImmersiveSticky(window)
    }

    @Suppress("DEPRECATION")
    fun applyImmersiveSticky(window: Window) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
