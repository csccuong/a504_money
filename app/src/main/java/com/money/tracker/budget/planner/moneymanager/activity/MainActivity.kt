package com.money.tracker.budget.planner.moneymanager.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.ActivityMainBinding
import com.money.tracker.budget.planner.moneymanager.fragment.BudgetFragment
import com.money.tracker.budget.planner.moneymanager.fragment.HomeFragment
import com.money.tracker.budget.planner.moneymanager.fragment.SettingsFragment
import com.money.tracker.budget.planner.moneymanager.fragment.StatisticsFragment
import com.money.tracker.budget.planner.moneymanager.notiSpecial.BottomSheetPerNotiFragment
import com.money.tracker.budget.planner.moneymanager.ui.common.SystemBarsHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.utils.NotificationPermissionHelper
import com.money.tracker.budget.planner.moneymanager.utils.TransactionUpdateEvent
import org.greenrobot.eventbus.EventBus

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private val preferences by lazy { AppPreferences.getInstance(this) }
    private val transactionRepository by lazy { TransactionRepository(preferences) }

    private var activeFragment: Fragment? = null
    private var lastNavAdTime: Long = 0L
    private var isNativeCollapClosed: Boolean = false

    private val addTransactionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val transactionJson = result.data?.getStringExtra(AddTransactionActivity.RESULT_TRANSACTION_DATA)
            if (transactionRepository.fromJson(transactionJson) != null) {
                val updatedList = transactionRepository.getTransactions()
                EventBus.getDefault().post(TransactionUpdateEvent(updatedList))
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationPermissionHelper.startWakeServiceSafely(this)
        }
    }

    override fun setupView(savedInstanceState: Bundle?) {
        if (preferences.getSelectedCurrencyRaw().isEmpty()) {
            startActivity(Intent(this, CurrencyUnitActivity::class.java))
            finish()
            return
        }

        preferences.incrementCounter()
        setupClickListeners()
        setupBackPressHandler()

        val openAddBudgetSheet = intent.getBooleanExtra(EXTRA_OPEN_ADD_BUDGET_SHEET, false)
        if (savedInstanceState == null) {
            applyShortcutDestination(intent)
        }

        handleNotificationPermissionFlow(skipPromptSheet = openAddBudgetSheet)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (preferences.getSelectedCurrencyRaw().isNotEmpty()) {
            applyShortcutDestination(intent)
        }
    }

    private fun applyShortcutDestination(targetIntent: Intent) {
        val tabIndex = targetIntent.getIntExtra(EXTRA_OPEN_TAB, TAB_HOME)
        val openAddBudgetSheet = targetIntent.getBooleanExtra(EXTRA_OPEN_ADD_BUDGET_SHEET, false)
        targetIntent.removeExtra(EXTRA_OPEN_ADD_BUDGET_SHEET)

        when (tabIndex) {
            TAB_BUDGET -> {
                val activeBudget = activeFragment as? BudgetFragment
                if (activeBudget != null) {
                    updateBottomNavSelection(TAB_BUDGET)
                    if (openAddBudgetSheet) {
                        activeBudget.openCreateBudgetSheetIfReady()
                    }
                } else {
                    openBudgetTab(openAddBudgetSheet)
                }
            }
            TAB_STATISTIC -> {
                loadFragment(StatisticsFragment())
                updateBottomNavSelection(TAB_STATISTIC)
            }
            TAB_SETTINGS -> {
                loadFragment(SettingsFragment())
                updateBottomNavSelection(TAB_SETTINGS)
            }
            else -> {
                loadFragment(HomeFragment())
                updateBottomNavSelection(TAB_HOME)
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                CustomBottomSheetDialogExitFragment.newInstance()
                    .show(supportFragmentManager, "ExitDialog")
            }
        })
    }

    private fun setupClickListeners() {
        binding.itemNoti.setOnClickListener {
            NotificationPermissionHelper.openNotificationSettings(this, MainActivity::class.java)
        }

        binding.navHome.setOnClickListener { view ->
            onTabClicked(view, TAB_HOME) { HomeFragment() }
        }
        binding.navStatistic.setOnClickListener { view ->
            onTabClicked(view, TAB_STATISTIC) { StatisticsFragment() }
        }
        binding.navBudget.setOnClickListener { view ->
            onTabClicked(view, TAB_BUDGET) { BudgetFragment() }
        }
        binding.navSettings.setOnClickListener { view ->
            onTabClicked(view, TAB_SETTINGS) { SettingsFragment() }
        }

        binding.navAdd.setOnClickListener { view ->
            view.isEnabled = false
            AdsManager.loadAndShowInter(
                activity = this,
                adUnitIdRes = R.string.inter_all,
                isEnabled = RemoteConfigs.inter_all,
                requireFullAds = true
            ) {
                startAddTransactionActivity()
                view.isEnabled = true
            }
        }
    }

    private fun onTabClicked(navButton: View, tabIndex: Int, fragmentFactory: () -> Fragment) {
        navButton.isEnabled = false
        handleNavWithCooldown(navButton) {
            loadFragment(fragmentFactory())
            updateBottomNavSelection(tabIndex)
        }
    }

    private fun handleNavWithCooldown(navButton: View, action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (AdsManager.isFullAdsEnabled() && now - lastNavAdTime > AD_COOLDOWN_MS) {
            AdsManager.loadAndShowInter(
                activity = this,
                adUnitIdRes = R.string.inter_all,
                isEnabled = RemoteConfigs.inter_all,
                requireFullAds = true
            ) {
                lastNavAdTime = System.currentTimeMillis()
                action()
                navButton.isEnabled = true
            }
        } else {
            action()
            navButton.isEnabled = true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        if (activeFragment?.javaClass == fragment.javaClass) return

        if (fragment is HomeFragment || fragment is StatisticsFragment || fragment is BudgetFragment) {
            val transactions = transactionRepository.getTransactions()
            val args = fragment.arguments ?: Bundle()
            args.putString(HomeFragment.ARG_TRANSACTION_LIST, transactionRepository.toJson(transactions))
            fragment.arguments = args
        }

        activeFragment = fragment
        runCatching {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss()
        }.onFailure {
            Log.e(TAG, "Error loading fragment", it)
        }
    }

    private fun updateBottomNavSelection(selectedIndex: Int) {
        val inactiveColor = compatColor(R.color.figma_nav_inactive)
        val activeColor = compatColor(R.color.figma_indigo)

        val tabs: List<Pair<ImageView, TextView>> = listOf(
            binding.ivHome to binding.tvHome,
            binding.ivStatistic to binding.tvStatistic,
            binding.ivBudget to binding.tvBudget,
            binding.ivSettings to binding.tvSettings
        )

        tabs.forEachIndexed { index, (icon, label) ->
            val color = if (index == selectedIndex) activeColor else inactiveColor
            icon.setColorFilter(color)
            label.setTextColor(color)
        }

        SystemBarsHelper.applySystemBars(
            this,
            R.color.figma_home_status_bar,
            SystemBarsHelper.StatusBarIconStyle.LIGHT
        )
    }

    fun openAddTransaction(
        preselectCategory: String? = null,
        preselectType: com.money.tracker.budget.planner.moneymanager.model.TransactionType? = null
    ) {
        val intent = Intent(this, AddTransactionActivity::class.java).apply {
            if (!preselectCategory.isNullOrBlank()) {
                putExtra(AddTransactionActivity.EXTRA_PRESELECT_CATEGORY, preselectCategory)
            }
            if (preselectType != null) {
                putExtra(AddTransactionActivity.EXTRA_PRESELECT_TYPE, preselectType.value)
            }
        }
        addTransactionLauncher.launch(intent)
    }

    fun openBudgetTab(openAddBudgetSheet: Boolean = false) {
        val fragment = BudgetFragment().apply {
            if (openAddBudgetSheet) {
                arguments = Bundle().apply {
                    putBoolean(BudgetFragment.ARG_OPEN_ADD_BUDGET_SHEET, true)
                }
            }
        }
        loadFragment(fragment)
        updateBottomNavSelection(TAB_BUDGET)
    }

    private fun startAddTransactionActivity() {
        openAddTransaction()
    }

    private fun handleNotificationPermissionFlow(skipPromptSheet: Boolean = false) {
        val launchCount = preferences.getAppLaunchCount()
        if (launchCount == 0) {
            requestNotificationPermission()
        } else if (!skipPromptSheet) {
            showNotificationSheetIfNeeded()
        }
        preferences.setAppLaunchCount(launchCount + 1)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showNotificationSheetIfNeeded() {
        if (NotificationPermissionHelper.isNotificationPermissionGranted(this)) return

        preferences.setShownNotificationSheet(true)
        BottomSheetPerNotiFragment.newInstance().apply {
            onSetClick = {
                NotificationPermissionHelper.openNotificationSettings(this@MainActivity, MainActivity::class.java)
            }
            show(supportFragmentManager, "BottomSheetDialogAlwayFragment")
        }
    }

    private fun loadHomeBottomAds() {
        if (isFinishing || isDestroyed) return
        if (!AdsManager.isFullAdsEnabled()) {
            AdsManager.stopCollapsibleNative(this)
            binding.frAdsCollap.gone()
            binding.frAdsBanner.gone()
            return
        }

        if (isNativeCollapClosed) {
            AdsManager.stopCollapsibleNative(this)
            binding.frAdsCollap.gone()
            AdsManager.showNativeBannerHome(
                activity = this,
                container = binding.frAdsBanner,
                isEnabled = RemoteConfigs.native_banner,
                requireFullAds = true,
                onLoaded = {
                    if (!isFinishing && !isDestroyed) {
                        binding.frAdsBanner.show()
                    }
                },
                onFailed = {
                    if (!isFinishing && !isDestroyed) {
                        binding.frAdsBanner.gone()
                    }
                }
            )
            return
        }

        AdsManager.showCollapsibleNativeHome(
            activity = this,
            collapContainer = binding.frAdsCollap,
            bannerFallbackContainer = binding.frAdsBanner,
            isCollapEnabled = RemoteConfigs.native_collap,
            isBannerFallbackEnabled = RemoteConfigs.native_banner,
            requireFullAds = true,
            onClosedToBanner = {
                isNativeCollapClosed = true
            },
            onShown = {
                if (!isFinishing && !isDestroyed) {
                    binding.frAdsCollap.show()
                }
            },
            onFailed = {
                if (!isFinishing && !isDestroyed) {
                    binding.frAdsCollap.gone()
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (NotificationPermissionHelper.isNotificationPermissionGranted(this)) {
            binding.itemNoti.gone()
        } else {
            binding.itemNoti.show()
        }
        listOf(
            binding.navAdd,
            binding.navHome,
            binding.navBudget,
            binding.navStatistic,
            binding.navSettings
        ).forEach { it.isEnabled = true }

        loadHomeBottomAds()
    }

    override fun onPause() {
        super.onPause()
        AdsManager.stopCollapsibleNative(this)
    }

    override fun onDestroy() {
        AdsManager.stopCollapsibleNative(this)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val AD_COOLDOWN_MS = 45_000L
        const val TAB_HOME = 0
        const val TAB_STATISTIC = 1
        const val TAB_BUDGET = 2
        const val TAB_SETTINGS = 3
        const val EXTRA_OPEN_TAB = "extra_open_tab"
        const val EXTRA_OPEN_ADD_BUDGET_SHEET = "extra_open_add_budget_sheet"
    }
}
