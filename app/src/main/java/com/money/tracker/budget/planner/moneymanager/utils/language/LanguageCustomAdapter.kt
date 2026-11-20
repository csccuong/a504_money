package com.money.tracker.budget.planner.moneymanager.utils.language

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.databinding.ItemCustomLanguageBinding
import com.money.tracker.budget.planner.moneymanager.model.LanguageModel
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show

class LanguageCustomAdapter(
    private val items: MutableList<LanguageModel> = mutableListOf(),
    private val showHandHint: Boolean,
    private val onItemSelected: (position: Int, item: LanguageModel, adapter: LanguageCustomAdapter) -> Unit
) : RecyclerView.Adapter<LanguageCustomAdapter.LanguageViewHolder>() {

    private var checkedPosition: Int = RecyclerView.NO_POSITION
    private var showInitialHand: Boolean = true

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<LanguageModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun hideAnimHand() {
        showInitialHand = false
        if (items.size > 1) {
            notifyItemChanged(1)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun unselectAll() {
        checkedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectByCode(langCode: String): Boolean {
        val normalized = langCode.trim().lowercase()
        val base = normalized.substringBefore("-").substringBefore("_")
        val idx = items.indexOfFirst { it.isoLanguage.equals(normalized, ignoreCase = true) }
            .takeIf { it >= 0 }
            ?: items.indexOfFirst {
                it.isoLanguage.substringBefore("-").equals(base, ignoreCase = true)
            }
        return if (idx >= 0) {
            checkedPosition = idx
            showInitialHand = false
            notifyDataSetChanged()
            true
        } else {
            checkedPosition = RecyclerView.NO_POSITION
            notifyDataSetChanged()
            false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemCustomLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class LanguageViewHolder(
        private val binding: ItemCustomLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: LanguageModel, position: Int) {
            binding.tvTitle.text = model.languageName

            if (model.image == 0) {
                binding.ivAvatar.gone()
            } else {
                binding.ivAvatar.show()
                binding.ivAvatar.setImageResource(model.image)
            }

            if (position == 1 && showHandHint && showInitialHand) {
                binding.animHand.show()
            } else {
                binding.animHand.gone()
            }

            val isSelected = checkedPosition != RecyclerView.NO_POSITION && checkedPosition == bindingAdapterPosition
            binding.imgSelected.setImageResource(
                if (isSelected) R.drawable.ic_checked_language else R.drawable.ic_unchecked_language
            )

            binding.root.setOnClickListener {
                val adapterPos = bindingAdapterPosition
                if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener

                binding.imgSelected.setImageResource(R.drawable.ic_checked_language)
                showInitialHand = false
                binding.animHand.gone()

                if (checkedPosition != adapterPos) {
                    val previous = checkedPosition
                    checkedPosition = adapterPos
                    if (previous != RecyclerView.NO_POSITION) {
                        notifyItemChanged(previous)
                    }
                    onItemSelected(adapterPos, items[adapterPos], this@LanguageCustomAdapter)
                }
            }
        }
    }
}
