package com.money.tracker.budget.planner.moneymanager.activity

import android.content.Intent
import android.os.Bundle
import androidx.core.content.IntentCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.adapter.CategoryBottomSheetAdapter
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.BudgetRepository
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.AddTransactionActivityBinding
import com.money.tracker.budget.planner.moneymanager.databinding.BottomSheetCategoryBinding
import com.money.tracker.budget.planner.moneymanager.model.CategoryItem
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionType
import com.money.tracker.budget.planner.moneymanager.ui.common.AppPickerDialogHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.SystemBarsHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.TransactionTabHelper
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class AddTransactionActivity :
    BaseActivity<AddTransactionActivityBinding>(AddTransactionActivityBinding::inflate) {

    override val statusBarColorRes: Int = R.color.surface_card
    override val statusBarIconStyle: SystemBarsHelper.StatusBarIconStyle =
        SystemBarsHelper.StatusBarIconStyle.DARK

    private val preferences by lazy { AppPreferences.getInstance(this) }
    private val transactionRepository by lazy { TransactionRepository(preferences) }
    private val budgetRepository by lazy { BudgetRepository(preferences, transactionRepository) }

    private var currentType: TransactionType = TransactionType.EXPENSE
    private var selectedCategory: CategoryItem? = null
    private var selectedDate: String = FormatUtils.formatInputDate()
    private var selectedTime: String = FormatUtils.formatInputTime()
    private var editPosition: Int = -1
    private var currentCurrency: String = "$"

    override fun setupView(savedInstanceState: Bundle?) {
        currentCurrency = preferences.getSelectedCurrencyOrDefault(defaultSymbol = "$")
        binding.etAmount.attachThousandSeparator()
        binding.tvCurrency.text = currentCurrency
        binding.tvDate.text = selectedDate
        binding.tvTime.text = selectedTime

        setupListeners()

        val editingTransaction = IntentCompat.getSerializableExtra(
            intent,
            EXTRA_TRANSACTION,
            TransactionModel::class.java
        )
        if (editingTransaction != null) {
            binding.tvTitle.text = getString(R.string.edit_transaction)
            editPosition = intent.getIntExtra(EXTRA_POSITION, -1)
            populateTransaction(editingTransaction)
        } else {
            binding.tvTitle.text = getString(R.string.add_transaction)
            val preselectTypeValue = intent.getStringExtra(EXTRA_PRESELECT_TYPE)
            val initialType = if (!preselectTypeValue.isNullOrBlank()) {
                TransactionType.fromValue(preselectTypeValue)
            } else {
                TransactionType.EXPENSE
            }
            selectTransactionType(initialType)

            val preselectCategory = intent.getStringExtra(EXTRA_PRESELECT_CATEGORY)
            if (!preselectCategory.isNullOrBlank()) {
                CategoryCatalog.categoriesFor(initialType)
                    .firstOrNull { it.name.equals(preselectCategory, ignoreCase = true) }
                    ?.let(::applySelectedCategory)
            }
        }

        loadNativeAd()
    }

    private fun setupListeners() {
        binding.tvCancel.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.llSave.isEnabled = false
        binding.llSave.setOnClickListener {
            val amount = binding.etAmount.cleanNumericText()
            if (amount.isEmpty() || (amount.toDoubleOrNull() ?: 0.0) <= 0.0) {
                showToast(getString(R.string.msg_enter_amount))
                return@setOnClickListener
            }
            if (selectedCategory == null) {
                showToast(getString(R.string.msg_select_category))
                return@setOnClickListener
            }
            AdsManager.loadAndShowInter(
                activity = this,
                adUnitIdRes = R.string.inter_all,
                isEnabled = RemoteConfigs.inter_all,
                requireFullAds = false
            ) {
                saveTransaction()
            }
        }

        TransactionTabHelper.bindListeners(binding.layoutTabs, ::selectTransactionType)

        binding.btnDate.setOnClickListener { showDatePicker() }
        binding.btnTime.setOnClickListener { showTimePicker() }

        binding.categoryView.setOnClickListener {
            showCategoryBottomSheet(CategoryCatalog.categoriesFor(currentType))
        }
    }

    private fun selectTransactionType(type: TransactionType) {
        currentType = type
        TransactionTabHelper.updateSelection(binding.layoutTabs, type)

        when (type) {
            TransactionType.EXPENSE -> {
                binding.layoutLender.gone()
            }
            TransactionType.INCOME -> {
                binding.layoutLender.gone()
                binding.tvAutoBudgetHint.gone()
            }
            TransactionType.LOAN, TransactionType.BORROW -> {
                binding.layoutLender.show()
                binding.tvAutoBudgetHint.gone()
            }
        }

        CategoryCatalog.categoriesFor(type).firstOrNull()?.let(::applySelectedCategory)
    }

    private fun applySelectedCategory(category: CategoryItem) {
        selectedCategory = category
        binding.tvSelectedCategory.text = CategoryCatalog.localizedName(this, category.name)
        binding.ivSelectedCategoryIcon.setImageResource(category.iconResource)
        updateAutoBudgetBadge()
    }

    private fun updateAutoBudgetBadge() {
        if (currentType != TransactionType.EXPENSE) {
            binding.tvAutoBudgetHint.gone()
            return
        }
        val categoryName = selectedCategory?.name ?: run {
            binding.tvAutoBudgetHint.gone()
            return
        }
        val matchedBudget = budgetRepository.findCategoryBudget(categoryName)
        if (matchedBudget == null || matchedBudget.totalAmount <= 0.0) {
            binding.tvAutoBudgetHint.gone()
            return
        }

        val targetMonth = FormatUtils.parseTransactionDate(selectedDate)
            ?.let(YearMonthKey::fromDate)
            ?: YearMonthKey.current()
        val spent = budgetRepository.getTransactionsForBudget(
            transactionRepository.getTransactions(),
            matchedBudget,
            targetMonth
        ).sumOf { it.amountValue }
        val limit = matchedBudget.totalAmount
        val remaining = limit - spent
        val isOver = remaining < 0.0
        val usedPercent = ((spent / limit) * 100.0).toInt()

        val (colorRes, bgRes) = when {
            isOver -> R.color.semantic_expense to R.drawable.bg_badge_red
            usedPercent >= 80 -> R.color.semantic_warning to R.drawable.bg_badge_amber
            else -> R.color.semantic_income to R.drawable.bg_badge_green
        }

        val statusText = if (isOver) {
            getString(R.string.over_by_value, "$currentCurrency${FormatUtils.formatCompactAmount(abs(remaining))}")
        } else {
            "${getString(R.string.left)} $currentCurrency${FormatUtils.formatCompactAmount(remaining)}"
        }

        binding.tvAutoBudgetHint.text = "${matchedBudget.displayTitle(this)} • $statusText"
        binding.tvAutoBudgetHint.setTextColor(compatColor(colorRes))
        binding.tvAutoBudgetHint.setBackgroundResource(bgRes)
        binding.tvAutoBudgetHint.show()
    }

    private fun populateTransaction(transaction: TransactionModel) {
        binding.etAmount.setText(transaction.amount)
        binding.etNote.setText(transaction.note)

        if (transaction.date.isNotEmpty()) {
            selectedDate = transaction.date
            binding.tvDate.text = selectedDate
        }
        if (transaction.time.isNotEmpty()) {
            selectedTime = transaction.time
            binding.tvTime.text = selectedTime
        }

        val type = if (transaction.type == TransactionType.BORROW) TransactionType.LOAN else transaction.type
        selectTransactionType(type)

        if (type == TransactionType.LOAN || type == TransactionType.BORROW) {
            transaction.lender?.let { binding.etLender.setText(it) }
        }

        CategoryCatalog.categoriesFor(type)
            .firstOrNull { it.name.equals(transaction.categoryName, ignoreCase = true) }
            ?.let(::applySelectedCategory)
    }

    private fun showDatePicker() {
        val initial = FormatUtils.parseTransactionDate(selectedDate) ?: Date()
        AppPickerDialogHelper.showDatePicker(this, initial) { year, monthIndex, dayOfMonth ->
            selectedDate = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, monthIndex + 1, year)
            binding.tvDate.text = selectedDate
            updateAutoBudgetBadge()
        }
    }

    private fun showTimePicker() {
        val parts = selectedTime.split(":")
        val now = Calendar.getInstance()
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: now.get(Calendar.HOUR_OF_DAY)
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: now.get(Calendar.MINUTE)
        AppPickerDialogHelper.showTimePicker(this, initialHour, initialMinute) { hour, minute ->
            selectedTime = String.format(Locale.US, "%02d:%02d", hour, minute)
            binding.tvTime.text = selectedTime
        }
    }

    private fun showCategoryBottomSheet(categories: List<CategoryItem>) {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        val sheetBinding = BottomSheetCategoryBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        sheetBinding.rvCategoriesBottomSheet.layoutManager = GridLayoutManager(this, 4)
        sheetBinding.rvCategoriesBottomSheet.adapter = CategoryBottomSheetAdapter(
            categories = categories,
            selectedCategoryName = selectedCategory?.name
        ) { category, _ ->
            applySelectedCategory(category)
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.setOnShowListener {
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetDialog.behavior.skipCollapsed = true
        }
        bottomSheetDialog.show()
    }

    private fun saveTransaction() {
        val amount = binding.etAmount.cleanNumericText()
        if (amount.isEmpty()) {
            showToast(getString(R.string.msg_enter_amount))
            return
        }

        val category = selectedCategory ?: run {
            showToast(getString(R.string.msg_select_category))
            return
        }

        val currency = preferences.getSelectedCurrencyOrDefault(defaultSymbol = "$")
        val resolvedBudget = if (currentType == TransactionType.EXPENSE) {
            budgetRepository.findCategoryBudget(category.name)?.name ?: BUDGET_NONE
        } else {
            BUDGET_NONE
        }

        val transaction = TransactionModel(
            transactionType = currentType.value,
            amount = amount,
            currency = currency,
            categoryName = category.name,
            categoryIcon = category.iconResource,
            budget = resolvedBudget,
            note = binding.etNote.text?.toString().orEmpty().trim(),
            date = selectedDate,
            time = selectedTime,
            lender = if (currentType == TransactionType.LOAN) {
                binding.etLender.text?.toString()?.trim()?.ifEmpty { null }
            } else {
                null
            }
        )

        val updatedList = transactionRepository.addOrUpdateTransaction(transaction, editPosition)
        EventBus.getDefault().post(TransactionUpdateEvent(updatedList))

        if (currentType == TransactionType.EXPENSE && resolvedBudget != BUDGET_NONE) {
            budgetRepository.updateBudgetExpenses(resolvedBudget)
        }

        val resultIntent = Intent().apply {
            putExtra(RESULT_TRANSACTION_DATA, transactionRepository.toJson(transaction))
        }
        setResult(RESULT_OK, resultIntent)
        showToast(getString(R.string.msg_transaction_saved))
        finish()
    }

    private fun loadNativeAd() {
        AdsManager.loadAndShowNative(
            activity = this,
            container = binding.frAds,
            placementKey = "native_all",
            adUnitIdRes = R.string.native_all,
            layoutRes = R.layout.layout_native_small,
            isEnabled = RemoteConfigs.native_all,
            requireFullAds = false
        )
    }

    override fun onResume() {
        super.onResume()
        binding.llSave.isEnabled = true
    }

    companion object {
        const val EXTRA_TRANSACTION = "transaction"
        const val EXTRA_POSITION = "position"
        const val EXTRA_PRESELECT_CATEGORY = "preselect_category"
        const val EXTRA_PRESELECT_TYPE = "preselect_type"
        const val RESULT_TRANSACTION_DATA = "transactionData"
        private const val BUDGET_NONE = "None"
    }
}
