package com.money.tracker.budget.planner.moneymanager.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.ui.common.SystemBarsHelper
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper

abstract class BaseActivity<VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater) -> VB
) : AppCompatActivity() {

    protected lateinit var binding: VB
        private set

    protected open val applySavedLocale: Boolean = true
    protected open val useFullScreenFlag: Boolean = true
    protected open val hideNavigationBarOnResume: Boolean = true

    @ColorRes
    protected open val statusBarColorRes: Int? = R.color.figma_home_status_bar
    protected open val statusBarIconStyle: SystemBarsHelper.StatusBarIconStyle =
        SystemBarsHelper.StatusBarIconStyle.LIGHT

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (applySavedLocale) {
            LocaleHelper.applySavedLocale(this)
        }
        statusBarColorRes?.let { colorRes ->
            SystemBarsHelper.applySystemBars(this, colorRes, statusBarIconStyle)
        }
        if (useFullScreenFlag) {
            SystemBarsHelper.applyFullScreenFlag(this)
        }
        binding = bindingInflater(layoutInflater)
        setContentView(binding.root)
        setupView(savedInstanceState)
        if (hideNavigationBarOnResume) {
            SystemBarsHelper.applyImmersiveSticky(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hideNavigationBarOnResume) {
            SystemBarsHelper.applyImmersiveSticky(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && hideNavigationBarOnResume) {
            SystemBarsHelper.applyImmersiveSticky(this)
        }
    }

    protected abstract fun setupView(savedInstanceState: Bundle?)
}
