package com.money.tracker.budget.planner.moneymanager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.cscmobi.libraryads.CSCApplication
import com.cscmobi.libraryads.CSCFOConfigs
import com.cscmobi.libraryads.ads.native_ads.CSCNativeManager
import com.cscmobi.libraryads.ads.native_ads.model.AdLoadState
import com.cscmobi.libraryads.ads.native_ads.model.LoadStrategy
import com.cscmobi.libraryads.ads.native_ads.repository.NativeAdRepository
import com.cscmobi.libraryads.ads.open_ads.CSCOpenAds
import com.cscmobi.libraryads.ads.utils.EnumAdsNamePosition
import com.cscmobi.libraryads.commons.sharepreference.CSCSPFHolder
import com.cscmobi.libraryads.data.AdsLanguageConfig
import com.cscmobi.libraryads.data.AdsOBConfig
import com.cscmobi.libraryads.data.AdsSplashConfig
import com.cscmobi.libraryads.data.LanguageConfig
import com.cscmobi.libraryads.data.LanguageSetting
import com.cscmobi.libraryads.data.OBConfig
import com.cscmobi.libraryads.data.OnActivityCallBack
import com.cscmobi.libraryads.data.SplashConfig
import com.cscmobi.libraryads.data.UiLanguageConfig
import com.cscmobi.libraryads.data.UiOBConfig
import com.cscmobi.libraryads.data.UiSplashConfig
import com.cscmobi.libraryads.views.fragments.FragmentOnboarding2
import com.cscmobi.libraryads.views.fragments.FragmentOnboarding3
import com.cscmobi.libraryads.views.language.CSCLanguageActivity
import com.cscmobi.libraryads.views.onboarding.CSCOnboardingActivity
import com.cscmobi.libraryads.views.splash.CSCSplashActivity
import com.facebook.FacebookSdk
import com.google.firebase.FirebaseApp
import com.money.tracker.budget.planner.moneymanager.activity.CurrencyUnitActivity
import com.money.tracker.budget.planner.moneymanager.activity.MainActivity
import com.money.tracker.budget.planner.moneymanager.activity.SplashActivity
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.notiSpecial.AppInstallReceiver
import com.money.tracker.budget.planner.moneymanager.notiSpecial.UserPresentReceiver
import com.money.tracker.budget.planner.moneymanager.utils.AppActivityTracker
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import com.money.tracker.budget.planner.moneymanager.utils.language.EnumSelectLanguage

class Application : android.app.Application() {

    private var nonOrganicOb2Id: String = ""
    private var nonOrganicOb3Id: String = ""

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.applySavedLocale(this)
    }

    override fun onCreate() {
        super.onCreate()
        LocaleHelper.applySavedLocale(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (packageName != processName) {
                runCatching { WebView.setDataDirectorySuffix(processName) }
            }
        }

        runCatching { FirebaseApp.initializeApp(this) }
        FacebookSdk.setClientToken(getString(R.string.facebook_client_token))

        nonOrganicOb2Id = getString(R.string.native_onboarding_2)
        nonOrganicOb3Id = getString(R.string.native_onboarding_3)
        initFO()
        registerOnboardingOrganicCallbacks()
        registerSystemReceivers()
        setupDynamicShortcuts()
        AppActivityTracker.register(this)
    }

    private fun initFO() {
        val cscLibrary = CSCApplication(
            this,
            RemoteConfigs,
            getString(R.string.app_id),
            BuildConfig.DEBUG
        )

        cscLibrary.initSdk(
            adjustAppToken = "juvdp2tcfu2o",
            gsmAppId = "6afed437fe2551ee9ed5c511",
            splashConfig = SplashConfig(
                uiSplashConfig = UiSplashConfig(
                    resLayout = R.layout.activity_splash,
                    showFOForever = false,
                    homeActivity = MainActivity::class.java,
                    timeout = 45_000
                ),
                adsSplashConfig = AdsSplashConfig(
                    bannerId = getString(R.string.banner_splash),
                    interHighId = getString(R.string.inter_splash_high),
                    interAllId = getString(R.string.inter_splash),
                    nativeFullHighId = getString(R.string.native_full_high_inter_splash),
                    nativeFullId = getString(R.string.native_full_inter_splash),
                    nativeFullLayout = R.layout.layout_native_full,
                    loadInterStrategy = LoadStrategy.SEQUENTIAL,
                    loadNativeFullStrategy = LoadStrategy.SEQUENTIAL,
                    admobAOAId = getString(R.string.resume_open_app),
                    isCheckOrganicUser = true
                )
            ),
            languageConfig = LanguageConfig(
                uiLanguageConfig = UiLanguageConfig(
                    resLayout = R.layout.activity_language_app,
                    itemLangDefault = R.layout.item_select_language_default,
                    itemLangSelected = R.layout.item_select_language_selected,
                    listLanguage = EnumSelectLanguage.toLanguageModelList(),
                    languageSetting = object : LanguageSetting {
                        override fun onDone(activity: Activity) {
                            val code = CSCSPFHolder.get().language_code_selected
                            val previousCode = LocaleHelper.getSavedLanguage(activity)
                            val finalCode = if (!code.isNullOrBlank()) code else previousCode
                            LocaleHelper.changeLanguage(activity, finalCode)
                            AppPreferences.getInstance(activity).setLanguageSelectedFlag(true)
                            refreshDynamicShortcuts(activity)
                            if (!previousCode.equals(finalCode, ignoreCase = true)) {
                                val mainIntent = Intent(activity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                activity.startActivity(mainIntent)
                            }
                            activity.finish()
                        }
                    }
                ),
                adsLanguageConfig = AdsLanguageConfig(
                    nativeLangHighId = getString(R.string.native_language_high),
                    nativeLangId = getString(R.string.native_language),
                    nativeLangClickHighId = getString(R.string.native_language_high_click),
                    nativeLangClickId = getString(R.string.native_language_click),
                    layoutNative = R.layout.layout_native_media,
                    layoutNativeClick = R.layout.layout_native_media_click
                )
            ),
            obConfig = OBConfig(
                uiOBConfig = UiOBConfig(
                    resFragmentOB1 = R.layout.fragment_intro1,
                    resFragmentOB2 = R.layout.fragment_intro2,
                    resFragmentOB3 = R.layout.fragment_intro3,
                    resFragmentOB4 = R.layout.fragment_intro4,
                    activityCallback = object : OnActivityCallBack {
                        override fun onNextActivity(activity: Activity, inSession2: Boolean) {
                            val savedLang = CSCSPFHolder.get().language_code_selected
                            if (!savedLang.isNullOrBlank()) {
                                LocaleHelper.changeLanguage(activity, savedLang)
                            }
                            if (inSession2) {
                                val selectedCurrency = AppPreferences.getInstance(activity).getSelectedCurrencyRaw()
                                val target = if (selectedCurrency.isEmpty()) {
                                    CurrencyUnitActivity::class.java
                                } else {
                                    MainActivity::class.java
                                }
                                val intent = Intent(activity, target).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                                activity.startActivity(intent)
                            }
                        }
                    },
                    nextOBActivity = CurrencyUnitActivity::class.java
                ),
                adsOBConfig = AdsOBConfig(
                    nativeOB1Id = getString(R.string.native_onboarding_1),
                    nativeOB2Id = if (AdsManager.isFullAdsEnabled()) nonOrganicOb2Id else "",
                    nativeOB3Id = if (AdsManager.isFullAdsEnabled()) nonOrganicOb3Id else "",
                    nativeOB4Id = getString(R.string.native_onboarding_4),
                    nativeOBFull12Id = getString(R.string.native_onboarding_full_1_2),
                    nativeOBFull34Id = getString(R.string.native_onboarding_full_3_4),
                    layoutNativeOB1 = R.layout.layout_native_media_fo,
                    layoutNativeOB2 = R.layout.layout_native_media_fo,
                    layoutNativeOB3 = R.layout.layout_native_media_fo,
                    layoutNativeOB4 = R.layout.layout_native_media_fo,
                    layoutNativeFullOB = R.layout.admob_layout_native_full,
                    isLoadNativeOBInLanguage = true
                )
            )
        )

        runCatching {
            val field = CSCApplication::class.java.getDeclaredField("cscOpenAds").apply {
                isAccessible = true
            }
            AdsManager.registerOpenAds(field.get(cscLibrary) as? CSCOpenAds)
        }
    }

    private fun registerSystemReceivers() {
        ContextCompat.registerReceiver(
            this,
            UserPresentReceiver(),
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_EXPORTED
        )

        val appInstallFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            this,
            AppInstallReceiver(),
            appInstallFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun setupDynamicShortcuts() {
        refreshDynamicShortcuts(this)
    }

    /**
     * library-gma:0.0.9 gates nativeOBFull* on !isOrganicUser, but preloads and shows
     * nativeOB2Id / nativeOB3Id unconditionally. Keep OB2/OB3 enabled only for non-organic users.
     */
    private fun registerOnboardingOrganicCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is CSCSplashActivity ||
                    activity is CSCLanguageActivity ||
                    activity is CSCOnboardingActivity
                ) {
                    syncOnboardingAdsForOrganicState()
                }
                if (activity is CSCOnboardingActivity) {
                    registerOnboardingFragmentGuard(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun registerOnboardingFragmentGuard(activity: FragmentActivity) {
        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    fragment: Fragment,
                    view: View,
                    savedInstanceState: Bundle?
                ) {
                    guardOnboardingFragmentAds(fragment, view)
                }

                override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
                    fragment.view?.let { guardOnboardingFragmentAds(fragment, it) }
                }
            },
            false
        )
    }

    private fun guardOnboardingFragmentAds(fragment: Fragment, rootView: View) {
        if (fragment !is FragmentOnboarding2 && fragment !is FragmentOnboarding3) return
        syncOnboardingAdsForOrganicState()
        if (AdsManager.isFullAdsEnabled()) return
        hideOnboardingAdContainer(rootView)
    }

    private fun syncOnboardingAdsForOrganicState() {
        val showOb23 = AdsManager.isFullAdsEnabled()
        val targetOb2Id = if (showOb23) nonOrganicOb2Id else ""
        val targetOb3Id = if (showOb23) nonOrganicOb3Id else ""

        runCatching {
            val obConfig = CSCFOConfigs.obConfig
            val adsConfig = obConfig.adsOBConfig
            if (adsConfig.nativeOB2Id != targetOb2Id ||
                adsConfig.nativeOB3Id != targetOb3Id ||
                adsConfig.nativeOB2IdS2 != targetOb2Id ||
                adsConfig.nativeOB3IdS2 != targetOb3Id
            ) {
                val updatedAds = adsConfig.copy(
                    nativeOB2Id = targetOb2Id,
                    nativeOB3Id = targetOb3Id,
                    nativeOB2IdS2 = targetOb2Id,
                    nativeOB3IdS2 = targetOb3Id
                )
                CSCFOConfigs.init(
                    CSCFOConfigs.splashConfig,
                    CSCFOConfigs.languageConfig,
                    obConfig.copy(adsOBConfig = updatedAds)
                )
            }
        }

        syncOnboardingRepositoryEntries(
            showOb23 = showOb23,
            entries = listOf(
                EnumAdsNamePosition.NATIVE_ONBOARDING_2.position to targetOb2Id,
                EnumAdsNamePosition.NATIVE_ONBOARDING_3.position to targetOb3Id
            )
        )
    }

    private fun syncOnboardingRepositoryEntries(
        showOb23: Boolean,
        entries: List<Pair<String, String>>
    ) {
        val repository = runCatching {
            val field = CSCNativeManager::class.java.getDeclaredField("repository").apply {
                isAccessible = true
            }
            field.get(CSCNativeManager) as? NativeAdRepository
        }.getOrNull()

        entries.forEach { (adName, targetId) ->
            if (!showOb23) {
                CSCNativeManager.consumeAdWithoutActive(adName)?.destroy()
                repository?.let { repo ->
                    repo.consumePendingShow(adName)
                    repo.get(adName)?.let { entry ->
                        entry.id = ""
                        entry.idHigh = ""
                        entry.activeAd?.destroy()
                        entry.activeAd = null
                        entry.nativeAd?.destroy()
                        entry.nativeAd = null
                    }
                    repo.updateState(adName, AdLoadState.ERROR)
                }
            } else {
                repository?.get(adName)?.let { entry ->
                    if (entry.id.isBlank() && entry.state == AdLoadState.ERROR && targetId.isNotBlank()) {
                        entry.id = targetId
                        repository.updateState(adName, AdLoadState.NOT_LOADED)
                    }
                }
            }
        }
    }

    private fun hideOnboardingAdContainer(rootView: View) {
        val adFrame = rootView.findViewById<FrameLayout>(R.id.layoutAds) ?: return
        if (adFrame.childCount == 0 && adFrame.tag == TAG_ORGANIC_AD_DISABLED) {
            adFrame.visibility = View.GONE
            return
        }
        adFrame.removeAllViews()
        adFrame.visibility = View.GONE
        val parent = adFrame.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(adFrame)
        val layoutParams = adFrame.layoutParams
        parent.removeViewAt(index)
        val placeholder = FrameLayout(rootView.context).apply {
            id = R.id.layoutAds
            visibility = View.GONE
            tag = TAG_ORGANIC_AD_DISABLED
        }
        parent.addView(placeholder, index, layoutParams)
    }

    companion object {
        private const val TAG_ORGANIC_AD_DISABLED = "organic_ob_ad_disabled"
        private const val SHORTCUT_ID_ADD_BUDGET = "id_add_budget"
        private const val SHORTCUT_ID_UNINSTALL = "id_uninstall_fake"

        fun refreshDynamicShortcuts(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
            val localized = LocaleHelper.localizedContext(context)

            val addBudgetShortcut = buildSplashShortcut(
                context = context,
                shortcutId = SHORTCUT_ID_ADD_BUDGET,
                shortcutExtra = SplashActivity.SHORTCUT_ADD_BUDGET,
                label = localized.getString(R.string.add_budget),
                iconRes = R.drawable.ic_shortcut_add_budget
            )
            val uninstallShortcut = buildSplashShortcut(
                context = context,
                shortcutId = SHORTCUT_ID_UNINSTALL,
                shortcutExtra = SplashActivity.SHORTCUT_UNINSTALL_FAKE,
                label = localized.getString(R.string.uninstall),
                iconRes = R.drawable.ic_uninstall
            )

            shortcutManager.dynamicShortcuts = listOf(addBudgetShortcut, uninstallShortcut)
        }

        private fun buildSplashShortcut(
            context: Context,
            shortcutId: String,
            shortcutExtra: String,
            label: String,
            iconRes: Int
        ): ShortcutInfo {
            val intent = Intent(context, SplashActivity::class.java).apply {
                action = "${context.packageName}.shortcut.$shortcutExtra"
                putExtra(SplashActivity.EXTRA_SHORTCUT, shortcutExtra)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            return ShortcutInfo.Builder(context, shortcutId)
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon(Icon.createWithResource(context, iconRes))
                .setIntent(intent)
                .build()
        }
    }
}
