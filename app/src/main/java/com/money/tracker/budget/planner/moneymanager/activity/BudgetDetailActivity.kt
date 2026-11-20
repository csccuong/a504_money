package com.money.tracker.budget.planner.moneymanager.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.adapter.SimpleTransactionRowAdapter
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.BudgetRepository
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.ActivityBudgetDetailBinding
import com.money.tracker.budget.planner.moneymanager.model.BudgetItem
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.ui.common.AppDialogHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.BudgetSheetHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.SystemBarsHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.ui.common.showToast
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.YearMonthKey
import kotlin.math.abs

class BudgetDetailActivity :
    BaseActivity<ActivityBudgetDetailBinding>(ActivityBudgetDetailBinding::inflate) {

    override val statusBarColorRes: Int = R.color.surface_card
    override val statusBarIconStyle: SystemBarsHelper.StatusBarIconStyle =
        SystemBarsHelper.StatusBarIconStyle.DARK

    private val preferences by lazy { AppPreferences.getInstance(this) }
    private val transactionRepository by lazy { TransactionRepository(preferences) }
    private val budgetRepository by lazy { BudgetRepository(preferences, transactionRepository) }

    private var currentBudgetKey: String = ""
    private var currentBudget: BudgetItem? = null
    private var currentMonth: YearMonthKey = YearMonthKey.current()
    private var currentCurrency: String = "$"

    private lateinit var txAdapter: SimpleTransactionRowAdapter

    private val refreshLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        renderBudgetDetail()
    }

    override fun setupView(savedInstanceState: Bundle?) {
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        currentBudgetKey = intent.getStringExtra(EXTRA_BUDGET_KEY).orEmpty()

        val year = intent.getIntExtra(EXTRA_YEAR, currentMonth.year)
        val monthIdx = intent.getIntExtra(EXTRA_MONTH_INDEX, currentMonth.monthIndex)
        currentMonth = YearMonthKey(year, monthIdx.coerceIn(0, 11))

        txAdapter = SimpleTransactionRowAdapter(currentCurrency, ::openTransactionDetail)
        binding.rvBudgets.layoutManager = LinearLayoutManager(this)
        binding.rvBudgets.adapter = txAdapter

        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnEditBudget.setOnClickListener { openEditBudgetSheet() }
        binding.btnDeleteBudget.setOnClickListener { confirmDeleteBudget() }
        binding.btnAddBudget.setOnClickListener { openAddExpenseForBudget() }

        renderBudgetDetail()
        loadNativeAd()
    }

    override fun onResume() {
        super.onResume()
        renderBudgetDetail()
    }

    private fun renderBudgetDetail() {
        val loaded = budgetRepository.findBudgetByKey(currentBudgetKey)
            ?: budgetRepository.getBudgetItems().firstOrNull()
        if (loaded == null) {
            finish()
            return
        }
        currentBudget = loaded
        currentBudgetKey = loaded.stableKey

        binding.tvTitle.text = loaded.displayTitle(this)
        binding.tvMonthBadge.text = currentMonth.displayLabel(this)

        val allTransactions = transactionRepository.getTransactions()
        val matching = budgetRepository.getTransactionsForBudget(allTransactions, loaded, currentMonth)
            .sortedByDescending { FormatUtils.parseTransactionDate(it.date)?.time ?: 0L }

        val limit = loaded.totalAmount
        val spent = matching.sumOf { it.amountValue }
        val remaining = limit - spent
        val isOver = limit > 0.0 && spent > limit
        val usedPercent = if (limit > 0.0) ((spent / limit) * 100.0).toInt() else 0

        binding.tvTotalBudget.text = getString(
            R.string.budget_colon_value,
            "$currentCurrency${FormatUtils.formatAmount(limit)}"
        )
        binding.tvExpenses.text = getString(
            R.string.expenses_colon_value,
            "$currentCurrency${FormatUtils.formatAmount(spent)}"
        )

        val accentColorRes = when {
            isOver -> R.color.semantic_expense
            usedPercent >= 80 -> R.color.semantic_warning
            else -> R.color.semantic_income
        }
        val accentColor = compatColor(accentColorRes)

        if (isOver) {
            binding.tvRemaining.text = getString(
                R.string.over_by_value,
                "$currentCurrency${FormatUtils.formatAmount(abs(remaining))}"
            )
            binding.tvRemaining.setTextColor(compatColor(R.color.semantic_expense))
        } else {
            binding.tvRemaining.text = getString(
                R.string.remain_colon_value,
                "$currentCurrency${FormatUtils.formatAmount(remaining)}"
            )
            binding.tvRemaining.setTextColor(compatColor(R.color.semantic_income))
        }

        val elapsedDays = currentMonth.elapsedDaysForAverage()
        val dailyAvg = if (elapsedDays > 0) spent / elapsedDays else 0.0
        binding.tvDailyAvg.text = "${getString(R.string.daily_avg)}: ${
            getString(R.string.per_day_value, "$currentCurrency${FormatUtils.formatCompactAmount(dailyAvg)}")
        }"

        binding.mainProgressView.apply {
            setProgress(usedPercent)
            setProgressColor(accentColor)
            setCenterSubLabel(if (isOver) getString(R.string.over_budget) else getString(R.string.spent))
        }

        binding.tvTransactionsHeading.text = "${getString(R.string.transactions)} (${matching.size})"

        if (matching.isEmpty()) {
            binding.rvBudgets.gone()
            binding.layoutEmptyTx.show()
        } else {
            binding.layoutEmptyTx.gone()
            binding.rvBudgets.show()
            txAdapter.submitList(matching)
        }
    }

    private fun openEditBudgetSheet() {
        val target = currentBudget ?: return
        BudgetSheetHelper.showCreateOrEdit(
            context = this,
            budgetRepository = budgetRepository,
            editingItem = target
        ) { updated ->
            currentBudgetKey = updated.stableKey
            renderBudgetDetail()
        }
    }

    private fun confirmDeleteBudget() {
        val target = currentBudget ?: return
        AppDialogHelper.showConfirmDialog(
            context = this,
            titleRes = R.string.delete_budget,
            messageRes = R.string.delete_budget_confirm,
            positiveTextRes = R.string.delete,
            negativeTextRes = R.string.cancel,
            isDestructive = true,
            iconRes = R.drawable.ic_delete_clean
        ) {
            budgetRepository.deleteBudgetItem(target.stableKey)
            showToast(getString(R.string.budget_deleted))
            finish()
        }
    }

    private fun openAddExpenseForBudget() {
        val target = currentBudget ?: return
        val intent = Intent(this, AddTransactionActivity::class.java).apply {
            if (!target.categoryName.isNullOrBlank()) {
                putExtra(AddTransactionActivity.EXTRA_PRESELECT_CATEGORY, target.categoryName)
            }
        }
        refreshLauncher.launch(intent)
    }

    private fun openTransactionDetail(transaction: TransactionModel) {
        val intent = Intent(this, TransactionDetailActivity::class.java).apply {
            putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
        }
        refreshLauncher.launch(intent)
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

    companion object {
        const val EXTRA_BUDGET_KEY = "extra_budget_key"
        const val EXTRA_YEAR = "extra_year"
        const val EXTRA_MONTH_INDEX = "extra_month_index"
    }
}
