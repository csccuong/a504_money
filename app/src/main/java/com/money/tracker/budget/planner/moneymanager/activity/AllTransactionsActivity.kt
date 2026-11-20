package com.money.tracker.budget.planner.moneymanager.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.adapter.FlatTransactionAdapter
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.ActivityAllTransactionsBinding
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionType
import com.money.tracker.budget.planner.moneymanager.ui.common.MonthPickerDialog
import com.money.tracker.budget.planner.moneymanager.ui.common.PeriodStepperHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.TransactionUpdateEvent
import com.money.tracker.budget.planner.moneymanager.utils.YearMonthKey
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.abs

class AllTransactionsActivity :
    BaseActivity<ActivityAllTransactionsBinding>(ActivityAllTransactionsBinding::inflate) {

    private val preferences by lazy { AppPreferences.getInstance(this) }
    private val transactionRepository by lazy { TransactionRepository(preferences) }

    private val flatAdapter by lazy {
        FlatTransactionAdapter(
            displayStyle = FlatTransactionAdapter.DisplayStyle.GROUPED_CARDS,
            collapsible = true,
            onTransactionClick = ::openTransactionDetail,
            onToggleDay = ::toggleDayCollapsed
        )
    }

    private var allTransactions: List<TransactionModel> = emptyList()
    private var currentMonth: YearMonthKey = YearMonthKey.current()
    private var isAllTimeMode: Boolean = false
    private var selectedTypeFilter: TransactionType? = null // null == All
    private var selectedCategory: String? = null
    private var searchQuery: String = ""
    private var currentCurrency: String = "$"

    private val collapsedDateKeys = mutableSetOf<String>()
    private var lastDisplayedGroupKeys: List<String> = emptyList()
    private var lastRenderedCategories: List<String> = emptyList()
    private var lastRenderedSelectedCategory: String? = null

    private val transactionChangeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshFromStorage()
        }
    }

    override fun setupView(savedInstanceState: Bundle?) {
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        readInitialIntentExtras()
        setupRecyclerView()
        setupListeners()
        updatePeriodControls()
        updateTypeTabsUI()
        refreshFromStorage()
        loadNativeAd()
    }

    private fun readInitialIntentExtras() {
        val now = YearMonthKey.current()
        val year = intent.getIntExtra(EXTRA_YEAR, now.year)
        val monthIdx = intent.getIntExtra(EXTRA_MONTH_INDEX, now.monthIndex).coerceIn(0, 11)
        currentMonth = YearMonthKey(year, monthIdx)
        isAllTimeMode = intent.getBooleanExtra(EXTRA_ALL_TIME, false)

        val typeValue = intent.getStringExtra(EXTRA_TYPE_FILTER)
        selectedTypeFilter = if (typeValue.isNullOrBlank()) {
            null
        } else {
            TransactionType.fromValue(typeValue)
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        binding.rvAllTransactions.layoutManager = layoutManager
        binding.rvAllTransactions.adapter = flatAdapter
        binding.rvAllTransactions.itemAnimator?.changeDuration = 160L

        binding.rvAllTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val shouldShow = firstVisible >= SCROLL_TOP_THRESHOLD
                if (shouldShow && binding.fabScrollTop.visibility != View.VISIBLE) {
                    binding.fabScrollTop.alpha = 0f
                    binding.fabScrollTop.show()
                    binding.fabScrollTop.animate().alpha(1f).setDuration(160L).start()
                } else if (!shouldShow && binding.fabScrollTop.visibility == View.VISIBLE) {
                    binding.fabScrollTop.animate()
                        .alpha(0f)
                        .setDuration(140L)
                        .withEndAction { binding.fabScrollTop.gone() }
                        .start()
                }
            }
        })
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }

        binding.btnPeriodMode.setOnClickListener {
            isAllTimeMode = !isAllTimeMode
            collapsedDateKeys.clear()
            updatePeriodControls()
            renderAll()
        }

        PeriodStepperHelper.bind(
            stepperBinding = binding.periodStepper,
            onPrevious = {
                currentMonth = currentMonth.previous()
                collapsedDateKeys.clear()
                updatePeriodControls()
                renderAll()
            },
            onNext = {
                currentMonth = currentMonth.next()
                collapsedDateKeys.clear()
                updatePeriodControls()
                renderAll()
            },
            onLabelClick = {
                MonthPickerDialog.show(this, currentMonth) { selected ->
                    currentMonth = selected
                    collapsedDateKeys.clear()
                    updatePeriodControls()
                    renderAll()
                }
            }
        )

        binding.tabAll.setOnClickListener { selectTypeFilter(null) }
        binding.tabExpense.setOnClickListener { selectTypeFilter(TransactionType.EXPENSE) }
        binding.tabIncome.setOnClickListener { selectTypeFilter(TransactionType.INCOME) }
        binding.tabLoan.setOnClickListener { selectTypeFilter(TransactionType.LOAN) }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                searchQuery = query
                if (query.isEmpty()) {
                    binding.ivClearSearch.gone()
                } else {
                    binding.ivClearSearch.show()
                }
                renderAll()
            }
        })

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.setText("")
        }

        binding.btnToggleCollapse.setOnClickListener {
            if (lastDisplayedGroupKeys.isEmpty()) return@setOnClickListener
            val allCollapsed = lastDisplayedGroupKeys.all { collapsedDateKeys.contains(it) }
            if (allCollapsed) {
                collapsedDateKeys.clear()
            } else {
                collapsedDateKeys.addAll(lastDisplayedGroupKeys)
            }
            renderAll()
        }

        binding.fabScrollTop.setOnClickListener {
            binding.rvAllTransactions.smoothScrollToPosition(0)
        }

        binding.btnEmptyAction.setOnClickListener {
            if (hasActiveSearchOrCategoryFilter()) {
                resetFilters()
            } else {
                val intent = Intent(this, AddTransactionActivity::class.java)
                transactionChangeLauncher.launch(intent)
            }
        }
    }

    private fun selectTypeFilter(type: TransactionType?) {
        if (selectedTypeFilter == type) return
        selectedTypeFilter = type
        selectedCategory = null
        collapsedDateKeys.clear()
        updateTypeTabsUI()
        renderAll()
    }

    private fun toggleDayCollapsed(dateKey: String) {
        if (!collapsedDateKeys.add(dateKey)) {
            collapsedDateKeys.remove(dateKey)
        }
        renderAll()
    }

    private fun resetFilters() {
        searchQuery = ""
        selectedCategory = null
        selectedTypeFilter = null
        binding.etSearch.setText("")
        updateTypeTabsUI()
        renderAll()
    }

    private fun hasActiveSearchOrCategoryFilter(): Boolean =
        searchQuery.isNotBlank() || !selectedCategory.isNullOrBlank() || selectedTypeFilter != null

    private fun updatePeriodControls() {
        PeriodStepperHelper.setLabel(binding.periodStepper, currentMonth.shortDisplayLabel(this))
        if (isAllTimeMode) {
            binding.periodStepper.root.gone()
            binding.tvAllTimeIndicator.show()
            binding.btnPeriodMode.text = getString(R.string.month)
        } else {
            binding.periodStepper.root.show()
            binding.tvAllTimeIndicator.gone()
            binding.btnPeriodMode.text = getString(R.string.all_time)
        }
    }

    private fun updateTypeTabsUI() {
        val tabs = listOf(
            Triple(binding.tabAll, null, R.color.brand_primary),
            Triple(binding.tabExpense, TransactionType.EXPENSE, R.color.semantic_expense),
            Triple(binding.tabIncome, TransactionType.INCOME, R.color.semantic_income),
            Triple(binding.tabLoan, TransactionType.LOAN, R.color.brand_primary)
        )
        val mediumTypeface = ResourcesCompat.getFont(this, R.font.roboto_medium)
        val boldTypeface = ResourcesCompat.getFont(this, R.font.roboto_bold)
        val inactiveColor = compatColor(R.color.text_secondary)

        tabs.forEach { (view, type, activeColorRes) ->
            val isSelected = selectedTypeFilter == type
            view.setBackgroundResource(
                if (isSelected) R.drawable.bg_segmented_selected else android.R.color.transparent
            )
            view.setTextColor(if (isSelected) compatColor(activeColorRes) else inactiveColor)
            view.typeface = if (isSelected) boldTypeface else mediumTypeface
        }
    }

    private fun renderAll() {
        val periodScope = if (isAllTimeMode) null else currentMonth

        // Step 1: Transactions scoped by Period + Type (used to populate available Category chips)
        val baseTransactions = transactionRepository.filterTransactions(
            transactions = allTransactions,
            yearMonth = periodScope,
            typeFilter = selectedTypeFilter
        )
        val availableCategories = transactionRepository.extractAvailableCategories(baseTransactions)
        if (selectedCategory != null && availableCategories.none { it.equals(selectedCategory, ignoreCase = true) }) {
            selectedCategory = null
        }
        renderCategoryChips(availableCategories)

        // Step 2: Apply Category + Search filters
        val filtered = transactionRepository.filterTransactions(
            transactions = baseTransactions,
            categoryFilter = selectedCategory,
            searchQuery = searchQuery,
            context = this
        )

        // Step 3: Header count & dynamic summary strip
        binding.tvResultCount.text = getString(R.string.transactions_count, filtered.size)
        renderSummaryStrip(filtered)

        // Step 4: Group by day & flatten for 60fps ListAdapter
        val isLoanMode = selectedTypeFilter == TransactionType.LOAN
        val groups = transactionRepository.groupByDisplayDayUnified(filtered, loanOnlyMode = isLoanMode)
        lastDisplayedGroupKeys = groups.map { it.dateKey }
        collapsedDateKeys.retainAll(lastDisplayedGroupKeys.toSet())

        // Show Collapse/Expand toggle when >= 2 days exist
        if (groups.size >= 2) {
            binding.btnToggleCollapse.show()
            val allCollapsed = lastDisplayedGroupKeys.all { collapsedDateKeys.contains(it) }
            binding.btnToggleCollapse.text = getString(
                if (allCollapsed) R.string.expand_all else R.string.collapse_all
            )
        } else {
            binding.btnToggleCollapse.gone()
        }

        if (availableCategories.isEmpty() && groups.size < 2) {
            binding.rowSecondaryControls.gone()
        } else {
            binding.rowSecondaryControls.show()
        }

        if (filtered.isEmpty()) {
            flatAdapter.submitList(emptyList())
            binding.rvAllTransactions.gone()
            binding.fabScrollTop.gone()
            binding.layoutEmpty.show()
            if (hasActiveSearchOrCategoryFilter()) {
                binding.tvEmptyTitle.text = getString(R.string.no_matching_transactions)
                binding.btnEmptyAction.text = getString(R.string.reset)
            } else {
                binding.tvEmptyTitle.text = getString(R.string.add_your_first_transaction)
                binding.btnEmptyAction.text = getString(R.string.add_transaction)
            }
        } else {
            binding.layoutEmpty.gone()
            binding.rvAllTransactions.show()
            val rows = FlatTransactionAdapter.flattenGroups(
                groups = groups,
                currency = currentCurrency,
                collapsedDateKeys = collapsedDateKeys
            )
            flatAdapter.submitList(rows)
        }
    }

    private fun renderSummaryStrip(filtered: List<TransactionModel>) {
        val cashflow = transactionRepository.calculateCashflow(filtered)
        if (selectedTypeFilter == TransactionType.LOAN) {
            binding.tvSummaryLabel1.text = getString(R.string.lent)
            binding.tvSummaryLabel2.text = getString(R.string.borrowed)
            binding.tvSummaryLabel3.text = getString(R.string.net_loan)

            binding.tvSummaryValue1.text = "+$currentCurrency${FormatUtils.formatAmount(cashflow.loan)}"
            binding.tvSummaryValue1.setTextColor(compatColor(R.color.semantic_income_on_dark))

            binding.tvSummaryValue2.text = "-$currentCurrency${FormatUtils.formatAmount(cashflow.borrow)}"
            binding.tvSummaryValue2.setTextColor(compatColor(R.color.semantic_expense_on_dark))

            val netLoan = cashflow.netLoan
            val netPrefix = when {
                netLoan > 0.0 -> "+"
                netLoan < 0.0 -> "-"
                else -> ""
            }
            binding.tvSummaryValue3.text = "$netPrefix$currentCurrency${FormatUtils.formatAmount(abs(netLoan))}"
            binding.tvSummaryValue3.setTextColor(
                compatColor(
                    when {
                        netLoan > 0.0 -> R.color.semantic_income_on_dark
                        netLoan < 0.0 -> R.color.semantic_expense_on_dark
                        else -> R.color.white
                    }
                )
            )
        } else {
            binding.tvSummaryLabel1.text = getString(R.string.income)
            binding.tvSummaryLabel2.text = getString(R.string.expense)
            binding.tvSummaryLabel3.text = getString(R.string.net_savings)

            binding.tvSummaryValue1.text = "+$currentCurrency${FormatUtils.formatAmount(cashflow.income)}"
            binding.tvSummaryValue1.setTextColor(compatColor(R.color.semantic_income_on_dark))

            binding.tvSummaryValue2.text = "-$currentCurrency${FormatUtils.formatAmount(cashflow.expense)}"
            binding.tvSummaryValue2.setTextColor(compatColor(R.color.semantic_expense_on_dark))

            val net = cashflow.netSavings
            val netPrefix = when {
                net > 0.0 -> "+"
                net < 0.0 -> "-"
                else -> ""
            }
            binding.tvSummaryValue3.text = "$netPrefix$currentCurrency${FormatUtils.formatAmount(abs(net))}"
            binding.tvSummaryValue3.setTextColor(
                compatColor(
                    when {
                        net > 0.0 -> R.color.semantic_income_on_dark
                        net < 0.0 -> R.color.semantic_expense_on_dark
                        else -> R.color.white
                    }
                )
            )
        }
    }

    private fun renderCategoryChips(categories: List<String>) {
        if (categories.isEmpty()) {
            binding.hsvCategories.gone()
            lastRenderedCategories = emptyList()
            lastRenderedSelectedCategory = null
            return
        }
        binding.hsvCategories.show()

        if (categories == lastRenderedCategories && selectedCategory == lastRenderedSelectedCategory) {
            return
        }

        val container = binding.llCategoryChips
        container.removeAllViews()
        lastRenderedCategories = categories
        lastRenderedSelectedCategory = selectedCategory

        val density = resources.displayMetrics.density
        val chipHeight = (30 * density).toInt()
        val padH = (12 * density).toInt()
        val gapEnd = (6 * density).toInt()
        val boldFont = ResourcesCompat.getFont(this, R.font.roboto_bold)
        val mediumFont = ResourcesCompat.getFont(this, R.font.roboto_medium)

        fun addChip(label: String, categoryKey: String?, isLast: Boolean) {
            val isSelected = (categoryKey == null && selectedCategory == null) ||
                (categoryKey != null && categoryKey.equals(selectedCategory, ignoreCase = true))

            val chip = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                maxLines = 1
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = if (isSelected) boldFont else mediumFont
                setTextColor(
                    compatColor(if (isSelected) R.color.brand_primary else R.color.text_secondary)
                )
                setBackgroundResource(
                    if (isSelected) R.drawable.bg_category_chip_selected else R.drawable.bg_category_chip_normal
                )
                setPadding(padH, 0, padH, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    chipHeight
                ).apply {
                    marginEnd = if (isLast) 0 else gapEnd
                }
                setOnClickListener {
                    selectedCategory = if (categoryKey != null && categoryKey.equals(selectedCategory, ignoreCase = true)) {
                        null
                    } else {
                        categoryKey
                    }
                    renderAll()
                }
            }
            container.addView(chip)
        }

        addChip(getString(R.string.all), null, categories.isEmpty())
        categories.forEachIndexed { idx, cat ->
            addChip(CategoryCatalog.localizedName(this, cat), cat, idx == categories.lastIndex)
        }
    }

    private fun openTransactionDetail(transaction: TransactionModel) {
        val intent = Intent(this, TransactionDetailActivity::class.java).apply {
            putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
        }
        transactionChangeLauncher.launch(intent)
    }

    private fun refreshFromStorage() {
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        allTransactions = transactionRepository.getTransactions()
        renderAll()
    }

    private fun loadNativeAd() {
        AdsManager.loadAndShowNative(
            activity = this,
            container = binding.frAds,
            placementKey = "native_all",
            adUnitIdRes = R.string.native_all,
            layoutRes = R.layout.layout_native_media,
            isEnabled = RemoteConfigs.native_all,
            requireFullAds = false
        )
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshFromStorage()
    }

    override fun onStop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTransactionUpdated(event: TransactionUpdateEvent) {
        allTransactions = event.transactionList
        renderAll()
    }

    companion object {
        const val EXTRA_YEAR = "extra_year"
        const val EXTRA_MONTH_INDEX = "extra_month_index"
        const val EXTRA_TYPE_FILTER = "extra_type_filter"
        const val EXTRA_ALL_TIME = "extra_all_time"

        private const val SCROLL_TOP_THRESHOLD = 6

        fun createIntent(
            context: Context,
            yearMonth: YearMonthKey = YearMonthKey.current(),
            typeFilter: TransactionType? = null,
            allTime: Boolean = false
        ): Intent = Intent(context, AllTransactionsActivity::class.java).apply {
            putExtra(EXTRA_YEAR, yearMonth.year)
            putExtra(EXTRA_MONTH_INDEX, yearMonth.monthIndex)
            if (typeFilter != null) {
                putExtra(EXTRA_TYPE_FILTER, typeFilter.value)
            }
            putExtra(EXTRA_ALL_TIME, allTime)
        }
    }
}
