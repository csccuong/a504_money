package com.money.tracker.budget.planner.moneymanager.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.databinding.ItemCategoryBinding
import com.money.tracker.budget.planner.moneymanager.model.CategoryItem
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor

class CategoryBottomSheetAdapter(
    private val categories: List<CategoryItem>,
    private val selectedCategoryName: String? = null,
    private val onCategorySelected: (CategoryItem, Int) -> Unit
) : RecyclerView.Adapter<CategoryBottomSheetAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position)
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryItem, position: Int) {
            val ctx = binding.root.context
            val isSelected = !selectedCategoryName.isNullOrBlank() &&
                item.name.equals(selectedCategoryName, ignoreCase = true)

            binding.ivCategoryIcon.setImageResource(item.iconResource)
            binding.tvCategoryName.text = CategoryCatalog.localizedName(ctx, item.name)
            binding.root.setBackgroundResource(
                if (isSelected) R.drawable.bg_category_chip_selected else R.drawable.bg_category_chip_normal
            )
            binding.tvCategoryName.setTextColor(
                ctx.compatColor(if (isSelected) R.color.brand_primary else R.color.text_primary)
            )
            binding.tvCategoryName.typeface = ResourcesCompat.getFont(
                ctx,
                if (isSelected) R.font.roboto_bold else R.font.roboto_medium
            )
            binding.root.setOnClickListener { onCategorySelected(item, position) }
        }
    }
}
