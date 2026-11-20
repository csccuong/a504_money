package com.money.tracker.budget.planner.moneymanager.fragment

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.activity.BudgetDetailActivity
import com.money.tracker.budget.planner.moneymanager.adapter.BudgetAdapter
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseFragment
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.BudgetCardState
import com.money.tracker.budget.planner.moneymanager.data.BudgetRepository
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.DialogEditBudgetBinding
import com.money.tracker.budget.planner.moneymanager.databinding.FragmentBudgetBinding
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.ui.common.AppDialogHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.BudgetSheetHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.MonthPickerDialog
import com.money.tracker.budget.planner.moneymanager.ui.common.PeriodStepperHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.attachThousandSeparator
import com.money.tracker.budget.planner.moneymanager.ui.common.cleanNumericText
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.ui.common.showToast
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.TransactionUpdateEvent
import com.money.tracker.budget.planner.moneymanager.utils.YearMonthKey
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.abs

class BudgetFragment : BaseFragment<FragmentBudgetBinding>(FragmentBudgetBinding::inflate) {

    private val preferences by lazy { AppPreferences.getInstance(requireContext()) }
    private val transactionRepository by lazy { TransactionRepository(preferences) }
    private val budgetRepository by lazy { BudgetRepository(preferences, transactionRepository) }

    private val budgetAdapter by lazy { BudgetAdapter(::openBudgetDetail) }

    private var allTransactions: List<TransactionModel> = emptyList()
    private var currentMonth: YearMonthKey = YearMonthKey.current()
    private var currentCurrency: String = "$"

    override fun setupView(savedInstanceState: Bundle?) {
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        binding.rvBudgets.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBudgets.adapter = budgetAdapter
        updateMonthLabel()

        setupListeners()
        loadInitialTransactions()
        renderBudgetDashboard()

        if (arguments?.getBoolean(ARG_OPEN_ADD_BUDGET_SHEET, false) == true) {
            arguments?.remove(ARG_OPEN_ADD_BUDGET_SHEET)
            openCreateBudgetSheetIfReady()
        }
    }

    private fun setupListeners() {
        binding.ivEditBalance.setOnClickListener { showEditMonthlyBudgetDialog() }
        binding.btnSetMonthlyBudget.setOnClickListener { showEditMonthlyBudgetDialog() }

        binding.btnAddBudget.setOnClickListener { openCreateBudgetSheet() }
        binding.btnEmptyAddBudget.setOnClickListener { openCreateBudgetSheet() }

        PeriodStepperHelper.bind(
            stepperBinding = binding.periodStepper,
            onPrevious = {
                currentMonth = currentMonth.previous()
                updateMonthLabel()
                renderBudgetDashboard()
            },
            onNext = {
                currentMonth = currentMonth.next()
                updateMonthLabel()
                renderBudgetDashboard()
            },
            onLabelClick = {
                MonthPickerDialog.show(requireContext(), currentMonth) { selectedMonth ->
                    currentMonth = selectedMonth
                    updateMonthLabel()
                    renderBudgetDashboard()
                }
            }
        )
    }

    private fun updateMonthLabel() {
        PeriodStepperHelper.setLabel(binding.periodStepper, currentMonth.shortDisplayLabel(requireContext()))
    }

    private fun loadInitialTransactions() {
        val json = arguments?.getString(HomeFragment.ARG_TRANSACTION_LIST)
        allTransactions = if (!json.isNullOrEmpty()) {
            transactionRepository.parseListJson(json)
        } else {
            transactionRepository.getTransactions()
        }
        budgetRepository.recalculateTotalExpenses(allTransactions, currentMonth)
    }

    fun openCreateBudgetSheetIfReady() {
        val root = bindingOrNull?.root ?: return
        root.post {
            if (isAdded && bindingOrNull != null) {
                openCreateBudgetSheet()
            }
        }
    }

    private fun openCreateBudgetSheet() {
        BudgetSheetHelper.showCreateOrEdit(
            context = requireContext(),
            budgetRepository = budgetRepository,
            editingItem = null
        ) {
            renderBudgetDashboard()
        }
    }

    private fun openBudgetDetail(state: BudgetCardState) {
        val intent = Intent(requireContext(), BudgetDetailActivity::class.java).apply {
            putExtra(BudgetDetailActivity.EXTRA_BUDGET_KEY, state.item.stableKey)
            putExtra(BudgetDetailActivity.EXTRA_YEAR, currentMonth.year)
            putExtra(BudgetDetailActivity.EXTRA_MONTH_INDEX, currentMonth.monthIndex)
        }
        startActivity(intent)
    }

    private fun showEditMonthlyBudgetDialog() {
        val dialogBinding = DialogEditBudgetBinding.inflate(layoutInflater)
        dialogBinding.tvCurrencySymbol.text = currentCurrency
        dialogBinding.inputBudget.attachThousandSeparator()

        val existing = budgetRepository.getTotalBudget()
        if (existing > 0.0) {
            dialogBinding.inputBudget.setText(FormatUtils.formatAmount(existing))
            dialogBinding.inputBudget.setSelection(dialogBinding.inputBudget.text?.length ?: 0)
        }

        val dialog = AppDialogHelper.createStyledDialog(
            context = requireContext(),
            contentView = dialogBinding.root,
            widthRatio = 0.90f
        )

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnSave.setOnClickListener {
            val amount = dialogBinding.inputBudget.cleanNumericText().toDoubleOrNull()
            if (amount == null || amount < 0.0) {
                requireContext().showToast(getString(R.string.msg_invalid_amount))
                return@setOnClickListener
            }
            budgetRepository.setTotalBudget(amount)
            renderBudgetDashboard()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun renderBudgetDashboard() {
        val ctx = requireContext()
        val totalBudget = budgetRepository.getTotalBudget()
        val monthExpenses = budgetRepository.recalculateTotalExpenses(allTransactions, currentMonth)
        val remaining = totalBudget - monthExpenses
        val isOver = totalBudget > 0.0 && remaining < 0.0
        val usedPercent = if (totalBudget > 0.0) ((monthExpenses / totalBudget) * 100.0).toInt() else 0

        binding.tvTotalBudget.text = "$currentCurrency${FormatUtils.formatAmount(totalBudget)}"
        binding.tvExpenses.text = "$currentCurrency${FormatUtils.formatAmount(monthExpenses)}"

        if (totalBudget <= 0.0) {
            binding.tvRemainingHero.text = "$currentCurrency${FormatUtils.formatAmount(monthExpenses)}"
            binding.tvRemainingHero.setTextColor(ctx.compatColor(R.color.text_primary))
            binding.tvRemainingSubtitle.text = getString(R.string.spent)
            binding.tvBudgetStatusBadge.gone()
            binding.progressMonthly.setProgressCompat(0, false)
            binding.tvDailyLimit.text = "-"
            binding.btnSetMonthlyBudget.show()
        } else {
            binding.btnSetMonthlyBudget.gone()
            binding.tvBudgetStatusBadge.show()

            val (accentColorRes, badgeBgRes) = when {
                isOver -> R.color.semantic_expense to R.drawable.bg_badge_red
                usedPercent >= 80 -> R.color.semantic_warning to R.drawable.bg_badge_amber
                else -> R.color.semantic_income to R.drawable.bg_badge_green
            }
            val accentColor = ctx.compatColor(accentColorRes)

            binding.tvBudgetStatusBadge.text = getString(R.string.used_percent, usedPercent)
            binding.tvBudgetStatusBadge.setTextColor(accentColor)
            binding.tvBudgetStatusBadge.setBackgroundResource(badgeBgRes)

            binding.progressMonthly.setIndicatorColor(accentColor)
            binding.progressMonthly.setProgressCompat(usedPercent.coerceIn(0, 100), false)

            if (isOver) {
                binding.tvRemainingHero.text = getString(
                    R.string.over_by_value,
                    "$currentCurrency${FormatUtils.formatAmount(abs(remaining))}"
                )
                binding.tvRemainingHero.setTextColor(ctx.compatColor(R.color.semantic_expense))
                binding.tvRemainingSubtitle.text = getString(R.string.over_budget)
            } else {
                binding.tvRemainingHero.text = "$currentCurrency${FormatUtils.formatAmount(remaining)}"
                binding.tvRemainingHero.setTextColor(ctx.compatColor(R.color.text_primary))
                binding.tvRemainingSubtitle.text = getString(R.string.remaining)
            }

            val remainingDays = currentMonth.remainingDaysInclusive()
            if (remainingDays > 0 && remaining > 0.0) {
                val perDay = remaining / remainingDays
                binding.tvDailyLimit.text = getString(
                    R.string.per_day_value,
                    "$currentCurrency${FormatUtils.formatCompactAmount(perDay)}"
                )
            } else {
                binding.tvDailyLimit.text = getString(R.string.per_day_value, "${currentCurrency}0")
            }
        }

        // Soft non-blocking hint if category allocations exceed monthly limit
        val totalAllocated = budgetRepository.getTotalAllocatedBudget()
        if (totalBudget > 0.0 && totalAllocated > totalBudget) {
            binding.tvAllocationHint.text = getString(
                R.string.budget_allocated_exceeds_hint,
                "$currentCurrency${FormatUtils.formatAmount(totalAllocated)}",
                "$currentCurrency${FormatUtils.formatAmount(totalBudget)}"
            )
            binding.tvAllocationHint.show()
        } else {
            binding.tvAllocationHint.gone()
        }

        // Category Budget Cards
        val cardStates = budgetRepository.buildBudgetCardStates(allTransactions, currentMonth)
        if (cardStates.isEmpty()) {
            binding.rvBudgets.gone()
            binding.layoutEmptyBudgets.show()
        } else {
            binding.layoutEmptyBudgets.gone()
            binding.rvBudgets.show()
            budgetAdapter.submitStates(cardStates, currentCurrency)
        }
    }

    private fun loadNativeAd() {
        if (!isAdded || bindingOrNull == null) return
        val hostActivity = activity ?: return
        binding.frAds.removeAllViews()
        AdsManager.loadAndShowNative(
            activity = hostActivity,
            container = binding.frAds,
            placementKey = "native_all",
            adUnitIdRes = R.string.native_all,
            layoutRes = R.layout.layout_native_small,
            isEnabled = RemoteConfigs.native_all,
            requireFullAds = true
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
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        allTransactions = transactionRepository.getTransactions()
        renderBudgetDashboard()
        loadNativeAd()
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
        if (bindingOrNull != null) {
            renderBudgetDashboard()
        }
    }

    companion object {
        const val ARG_OPEN_ADD_BUDGET_SHEET = "arg_open_add_budget_sheet"
    }
}
