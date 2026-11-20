package com.money.tracker.budget.planner.moneymanager.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.BudgetRepository
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.databinding.BottomSheetAddBudgetBinding
import com.money.tracker.budget.planner.moneymanager.databinding.ItemBudgetCategoryChipBinding
import com.money.tracker.budget.planner.moneymanager.model.BudgetItem
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils

object BudgetSheetHelper {

    private data class CategoryOption(
        val categoryKey: String,
        val displayName: String,
        val iconRes: Int,
        val isAlreadyUsed: Boolean
    )

    fun showCreateOrEdit(
        context: Context,
        budgetRepository: BudgetRepository,
        editingItem: BudgetItem? = null,
        onSaved: (BudgetItem) -> Unit
    ) {
        val existingBudgets = budgetRepository.getBudgetItems()
        val usedCategories = existingBudgets
            .filter { editingItem == null || it.stableKey != editingItem.stableKey }
            .mapNotNull { it.categoryName?.lowercase() }
            .toSet()

        val options = CategoryCatalog.expenseCategories.map { cat ->
            CategoryOption(
                categoryKey = cat.name,
                displayName = CategoryCatalog.localizedName(context, cat.name),
                iconRes = cat.iconResource,
                isAlreadyUsed = usedCategories.contains(cat.name.lowercase())
            )
        }

        val defaultIndex = if (editingItem != null && !editingItem.categoryName.isNullOrBlank()) {
            options.indexOfFirst {
                it.categoryKey.equals(editingItem.categoryName, ignoreCase = true)
            }.takeIf { it >= 0 } ?: options.indexOfFirst { !it.isAlreadyUsed }
        } else {
            options.indexOfFirst { !it.isAlreadyUsed }
        }

        if (defaultIndex < 0) {
            context.showToast(context.getString(R.string.all_categories_have_budgets))
            return
        }

        val dialog = BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme)
        val binding = BottomSheetAddBudgetBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        val currency = AppPreferences.getInstance(context).getSelectedCurrencyOrDefault()
        binding.tvCurrencySymbol.text = currency
        binding.etBudgetAmount.attachThousandSeparator()

        var selectedIndex = defaultIndex

        if (editingItem != null) {
            binding.tvSheetTitle.text = context.getString(R.string.edit_budget)
            binding.etBudgetAmount.setText(FormatUtils.formatAmount(editingItem.totalAmount))
            binding.etBudgetAmount.setSelection(binding.etBudgetAmount.text?.length ?: 0)
        } else {
            binding.tvSheetTitle.text = context.getString(R.string.add_budget)
        }

        val chipAdapter = CategoryChipAdapter(
            options = options,
            selectedIndexProvider = { selectedIndex },
            onSelect = { index ->
                val option = options[index]
                if (option.isAlreadyUsed) {
                    context.showToast(context.getString(R.string.budget_already_exists))
                    return@CategoryChipAdapter
                }
                selectedIndex = index
            }
        )

        binding.rvBudgetCategories.layoutManager = GridLayoutManager(context, 4)
        binding.rvBudgetCategories.adapter = chipAdapter

        binding.btnCancelBudget.setOnClickListener { dialog.dismiss() }
        binding.btnSaveBudget.setOnClickListener {
            val selected = options[selectedIndex]
            if (selected.isAlreadyUsed) {
                context.showToast(context.getString(R.string.budget_already_exists))
                return@setOnClickListener
            }

            val amountStr = binding.etBudgetAmount.cleanNumericText()
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                context.showToast(context.getString(R.string.msg_invalid_amount))
                return@setOnClickListener
            }

            val categoryKey = selected.categoryKey
            val resultItem = if (editingItem != null) {
                val updated = editingItem.copy(
                    id = editingItem.id.ifBlank { BudgetItem.newId() },
                    name = categoryKey,
                    categoryName = categoryKey,
                    totalAmount = amount,
                    color = BudgetItem.defaultColorForCategory(categoryKey)
                )
                budgetRepository.updateBudgetItem(editingItem.stableKey, updated)
                updated
            } else {
                val created = BudgetItem(
                    id = BudgetItem.newId(),
                    name = categoryKey,
                    categoryName = categoryKey,
                    totalAmount = amount,
                    color = BudgetItem.defaultColorForCategory(categoryKey)
                )
                budgetRepository.saveBudgetItem(created)
                if (budgetRepository.getTotalBudget() <= 0.0) {
                    budgetRepository.setTotalBudget(budgetRepository.getTotalAllocatedBudget())
                }
                created
            }

            context.showToast(context.getString(R.string.budget_saved))
            dialog.dismiss()
            onSaved(resultItem)
        }

        dialog.setOnShowListener {
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.behavior.skipCollapsed = true
        }
        dialog.show()
    }

    private class CategoryChipAdapter(
        private val options: List<CategoryOption>,
        private val selectedIndexProvider: () -> Int,
        private val onSelect: (Int) -> Unit
    ) : RecyclerView.Adapter<CategoryChipAdapter.ChipViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
            val binding = ItemBudgetCategoryChipBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ChipViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
            holder.bind(options[position], position == selectedIndexProvider())
        }

        override fun getItemCount(): Int = options.size

        inner class ChipViewHolder(
            private val binding: ItemBudgetCategoryChipBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            @SuppressLint("NotifyDataSetChanged")
            fun bind(option: CategoryOption, isSelected: Boolean) {
                val ctx = binding.root.context
                binding.ivIcon.setImageResource(option.iconRes)
                binding.tvName.text = option.displayName

                binding.chipRoot.setBackgroundResource(
                    if (isSelected) R.drawable.bg_category_chip_selected
                    else R.drawable.bg_category_chip_normal
                )
                binding.chipRoot.alpha = if (option.isAlreadyUsed && !isSelected) 0.42f else 1.0f
                binding.tvName.setTextColor(
                    ctx.compatColor(if (isSelected) R.color.brand_primary else R.color.text_primary)
                )

                binding.root.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onSelect(pos)
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }
}
