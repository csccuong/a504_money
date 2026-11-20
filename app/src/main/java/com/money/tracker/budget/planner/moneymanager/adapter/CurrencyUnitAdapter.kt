package com.money.tracker.budget.planner.moneymanager.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.databinding.ItemCurencyUnitBinding
import com.money.tracker.budget.planner.moneymanager.model.CurrencyUnitModel

class CurrencyUnitAdapter(
    context: Context,
    private val allItems: List<CurrencyUnitModel>,
    private val onCurrencySelected: (CurrencyUnitModel) -> Unit
) : RecyclerView.Adapter<CurrencyUnitAdapter.CurrencyUnitViewHolder>() {

    private val preferences = AppPreferences.getInstance(context)
    private val filteredItems = allItems.toMutableList()
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    init {
        syncSelectedPosition()
    }

    fun getSelectedPosition(): Int = selectedPosition

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        val normalized = query.trim().lowercase()
        filteredItems.clear()
        if (normalized.isEmpty()) {
            filteredItems.addAll(allItems)
        } else {
            filteredItems.addAll(
                allItems.filter { currency ->
                    currency.code.lowercase().contains(normalized) ||
                        currency.languageName.lowercase().contains(normalized) ||
                        currency.symbol.lowercase().contains(normalized)
                }
            )
        }
        syncSelectedPosition()
        notifyDataSetChanged()
    }

    private fun syncSelectedPosition() {
        val savedCode = preferences.getSelectedCurrencyCode()
        val savedSymbol = preferences.getSelectedCurrencyRaw()
        selectedPosition = when {
            savedCode.isNotEmpty() -> {
                filteredItems.indexOfFirst { it.code.equals(savedCode, ignoreCase = true) }
            }
            savedSymbol == "$" -> {
                filteredItems.indexOfFirst { it.code == "USD" }
                    .takeIf { it >= 0 }
                    ?: filteredItems.indexOfFirst { it.symbol == savedSymbol }
            }
            savedSymbol.isNotEmpty() -> {
                filteredItems.indexOfFirst { it.code.equals(savedSymbol, ignoreCase = true) || it.symbol == savedSymbol }
            }
            else -> RecyclerView.NO_POSITION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyUnitViewHolder {
        val binding = ItemCurencyUnitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CurrencyUnitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrencyUnitViewHolder, position: Int) {
        if (position !in filteredItems.indices) return
        val item = filteredItems[position]
        holder.bind(item, position == selectedPosition)
    }

    override fun getItemCount(): Int = filteredItems.size

    inner class CurrencyUnitViewHolder(
        private val binding: ItemCurencyUnitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: CurrencyUnitModel, isSelected: Boolean) {
            val ctx = binding.root.context
            binding.ivAvatar.setImageDrawable(ContextCompat.getDrawable(ctx, data.image))
            binding.tvName.text = data.languageName
            binding.tvCode.text = if (data.symbol.equals(data.code, ignoreCase = true)) {
                data.code
            } else {
                "${data.code} (${data.symbol})"
            }
            binding.v2.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.rlItem.background = ContextCompat.getDrawable(
                ctx,
                if (isSelected) R.drawable.bg_item_currency_true else R.drawable.bg_item_currency
            )

            binding.rlItem.setOnClickListener {
                val adapterPos = bindingAdapterPosition
                if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener

                val previous = selectedPosition
                selectedPosition = adapterPos

                preferences.setSelectedCurrency(data.symbol)
                preferences.setSelectedCurrencyCode(data.code)
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(Intent(ACTION_CURRENCY_CHANGED))

                if (previous != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previous)
                }
                notifyItemChanged(selectedPosition)
                onCurrencySelected(data)
            }
        }
    }

    companion object {
        const val ACTION_CURRENCY_CHANGED = "CURRENCY_CHANGED"
    }
}
