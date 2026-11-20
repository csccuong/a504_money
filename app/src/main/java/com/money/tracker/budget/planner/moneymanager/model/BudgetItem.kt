package com.money.tracker.budget.planner.moneymanager.model

import android.content.Context
import android.graphics.Color
import com.google.gson.annotations.SerializedName
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import java.util.UUID

data class BudgetItem(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("categoryName")
    val categoryName: String? = null,
    @SerializedName("totalAmount")
    val totalAmount: Double = 0.0,
    @SerializedName("spentAmount")
    var spentAmount: Double = 0.0,
    @SerializedName("color")
    val color: Int = defaultColorForCategory(categoryName)
) {
    val stableKey: String
        get() = id.ifBlank { name }

    val isCategoryLinked: Boolean
        get() = !categoryName.isNullOrBlank()

    fun displayTitle(context: Context): String =
        if (!categoryName.isNullOrBlank() && (name.isBlank() || name.equals(categoryName, ignoreCase = true))) {
            CategoryCatalog.localizedName(context, categoryName)
        } else {
            name.ifBlank { CategoryCatalog.localizedName(context, categoryName) }
        }

    val remainingAmount: Double
        get() = totalAmount - spentAmount

    val progress: Double
        get() = if (totalAmount <= 0.0) 0.0 else (spentAmount / totalAmount) * 100.0

    companion object {
        private val PALETTE = listOf(
            Color.parseColor("#5568D8"),
            Color.parseColor("#FF8A3D"),
            Color.parseColor("#12B76A"),
            Color.parseColor("#2E90FA"),
            Color.parseColor("#EE46BC"),
            Color.parseColor("#7A5AF8"),
            Color.parseColor("#06AED4"),
            Color.parseColor("#F79009"),
            Color.parseColor("#F04438"),
            Color.parseColor("#15B79E")
        )

        fun newId(): String = UUID.randomUUID().toString()

        fun defaultColorForCategory(categoryName: String?): Int {
            if (categoryName.isNullOrBlank()) return PALETTE.first()
            val index = (categoryName.hashCode() and Int.MAX_VALUE) % PALETTE.size
            return PALETTE[index]
        }
    }
}
