package com.money.tracker.budget.planner.moneymanager.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.BudgetRepository
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.ActivityTransactionDetailBinding
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionType
import com.money.tracker.budget.planner.moneymanager.ui.common.AppDialogHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.SystemBarsHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.ui.common.showToast
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.TransactionUpdateEvent
import org.greenrobot.eventbus.EventBus

class TransactionDetailActivity :
    BaseActivity<ActivityTransactionDetailBinding>(ActivityTransactionDetailBinding::inflate) {

    override val statusBarColorRes: Int = R.color.surface_card
    override val statusBarIconStyle: SystemBarsHelper.StatusBarIconStyle =
        SystemBarsHelper.StatusBarIconStyle.DARK

    private val preferences by lazy { AppPreferences.getInstance(this) }
    private val transactionRepository by lazy { TransactionRepository(preferences) }
    private val budgetRepository by lazy { BudgetRepository(preferences, transactionRepository) }

    private var transaction: TransactionModel? = null
    private var transactionIndex: Int = -1

    private val editTransactionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val json = result.data?.getStringExtra(AddTransactionActivity.RESULT_TRANSACTION_DATA)
        val updated = transactionRepository.fromJson(json) ?: return@registerForActivityResult

        transaction = updated
        transactionIndex = transactionRepository.findTransactionIndex(updated)
        renderTransaction(updated)
        setResult(RESULT_OK)
    }

    override fun setupView(savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val loaded = IntentCompat.getSerializableExtra(
            intent,
            EXTRA_TRANSACTION,
            TransactionModel::class.java
        )
        if (loaded == null) {
            showErrorAndFinish(getString(R.string.msg_transaction_details_not_found))
            return
        }

        transaction = loaded
        transactionIndex = transactionRepository.findTransactionIndex(loaded)
        renderTransaction(loaded)
        setupActionButtons()
        loadNativeAd()
    }

    private fun setupActionButtons() {
        binding.btnDelete.setOnClickListener { showDeleteConfirmationDialog() }
        binding.tvEdit.setOnClickListener {
            val current = transaction ?: return@setOnClickListener
            if (transactionIndex == -1) {
                transactionIndex = transactionRepository.findTransactionIndex(current)
            }
            val intent = Intent(this, AddTransactionActivity::class.java).apply {
                putExtra(AddTransactionActivity.EXTRA_TRANSACTION, current)
                putExtra(AddTransactionActivity.EXTRA_POSITION, transactionIndex)
            }
            editTransactionLauncher.launch(intent)
        }
    }

    private fun renderTransaction(tx: TransactionModel) {
        binding.ivCategoryIcon.setImageResource(CategoryCatalog.iconForCategory(tx.categoryName))
        binding.tvCategory.text = CategoryCatalog.localizedName(this, tx.categoryName)
        binding.tvDate.text = listOf(tx.date, tx.time).filter { it.isNotBlank() }.joinToString(" • ")

        val note = tx.note.trim()
        if (note.isNotEmpty()) {
            binding.tvNote.text = note
            binding.tvNote.setTextColor(compatColor(R.color.text_primary))
        } else {
            binding.tvNote.text = getString(R.string.no_note)
            binding.tvNote.setTextColor(compatColor(R.color.text_muted))
        }

        val isBorrow = TransactionRepository.isBorrowTransaction(tx)
        val currency = preferences.getSelectedCurrencyOrDefault()
        val (prefix, colorRes, badgeBgRes, typeLabel) = when {
            tx.type == TransactionType.INCOME -> Quad(
                "+", R.color.semantic_income, R.drawable.bg_badge_green, getString(R.string.income)
            )
            tx.type == TransactionType.EXPENSE -> Quad(
                "-", R.color.semantic_expense, R.drawable.bg_badge_red, getString(R.string.expense)
            )
            isBorrow -> Quad(
                "-", R.color.semantic_expense, R.drawable.bg_badge_red, getString(R.string.borrow)
            )
            else -> Quad(
                "+", R.color.semantic_info, R.drawable.bg_badge_blue, getString(R.string.loan)
            )
        }

        binding.tvAmount.text = "$prefix$currency${FormatUtils.formatAmount(tx.amountValue)}"
        binding.tvAmount.setTextColor(compatColor(colorRes))
        binding.tvTransactionType.text = typeLabel
        binding.tvTransactionType.setTextColor(compatColor(colorRes))
        binding.tvTransactionType.setBackgroundResource(badgeBgRes)

        when {
            tx.type == TransactionType.EXPENSE -> {
                val linkedBudget = budgetRepository.findCategoryBudget(tx.categoryName)?.displayTitle(this)
                    ?: tx.budget.takeIf { it.isNotBlank() && !it.equals("None", ignoreCase = true) }
                if (!linkedBudget.isNullOrBlank()) {
                    binding.rowExtra.show()
                    binding.tvExtraLabel.text = getString(R.string.budget)
                    binding.tvExtraValue.text = linkedBudget
                } else {
                    binding.rowExtra.gone()
                }
            }
            !tx.lender.isNullOrBlank() -> {
                binding.rowExtra.show()
                binding.tvExtraLabel.text = getString(R.string.lender)
                binding.tvExtraValue.text = tx.lender
            }
            else -> binding.rowExtra.gone()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AppDialogHelper.showConfirmDialog(
            context = this,
            titleRes = R.string.delete_transaction,
            messageRes = R.string.delete_transaction_confirm,
            positiveTextRes = R.string.delete,
            negativeTextRes = R.string.cancel,
            isDestructive = true,
            iconRes = R.drawable.ic_delete_clean,
            onConfirm = ::deleteTransaction
        )
    }

    private fun deleteTransaction() {
        val current = transaction ?: return showErrorAndFinish(getString(R.string.msg_unable_delete_transaction))
        val deletedIndex = transactionRepository.deleteTransaction(current)
        if (deletedIndex == -1) {
            showErrorAndFinish(getString(R.string.msg_transaction_not_found))
            return
        }

        if (current.type == TransactionType.EXPENSE && current.budget != "None") {
            budgetRepository.updateBudgetExpenses(current.budget)
        }

        EventBus.getDefault().post(TransactionUpdateEvent(transactionRepository.getTransactions()))

        val resultIntent = Intent().apply {
            putExtra(RESULT_DELETED_POSITION, deletedIndex)
        }
        setResult(RESULT_OK, resultIntent)
        showToast(getString(R.string.msg_transaction_deleted))
        finish()
    }

    private fun showErrorAndFinish(message: String) {
        showToast(message)
        finish()
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

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        const val EXTRA_TRANSACTION = "transaction"
        const val RESULT_DELETED_POSITION = "deleted_position"
    }
}
