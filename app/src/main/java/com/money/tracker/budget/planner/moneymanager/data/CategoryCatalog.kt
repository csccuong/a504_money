package com.money.tracker.budget.planner.moneymanager.data

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.model.CategoryItem
import com.money.tracker.budget.planner.moneymanager.model.TransactionType

object CategoryCatalog {

    val expenseCategories: List<CategoryItem> = listOf(
        CategoryItem(R.drawable.ic_food, "Food"),
        CategoryItem(R.drawable.ic_social, "Social"),
        CategoryItem(R.drawable.ic_trafic, "Traffic"),
        CategoryItem(R.drawable.ic_shopping, "Shopping"),
        CategoryItem(R.drawable.ic_grocery, "Grocery"),
        CategoryItem(R.drawable.ic_education, "Education"),
        CategoryItem(R.drawable.ic_bills, "Bills"),
        CategoryItem(R.drawable.ic_rentals, "Rentals"),
        CategoryItem(R.drawable.ic_medical, "Medical"),
        CategoryItem(R.drawable.ic_investment, "Investment"),
        CategoryItem(R.drawable.ic_gift, "Gift"),
        CategoryItem(R.drawable.ic_other, "Other")
    )

    val incomeCategories: List<CategoryItem> = listOf(
        CategoryItem(R.drawable.ic_salary, "Salary"),
        CategoryItem(R.drawable.ic_invest, "Invest"),
        CategoryItem(R.drawable.ic_business, "Business"),
        CategoryItem(R.drawable.ic_interest, "Interest"),
        CategoryItem(R.drawable.ic_extra_income, "Extra Income"),
        CategoryItem(R.drawable.ic_other, "Other")
    )

    val loanCategories: List<CategoryItem> = listOf(
        CategoryItem(R.drawable.ic_loan, "Loan"),
        CategoryItem(R.drawable.ic_borrow, "Borrow")
    )

    private val categoryIconMap: Map<String, Int> = buildMap {
        (expenseCategories + incomeCategories + loanCategories).forEach {
            put(it.name, it.iconResource)
        }
        put("Others", R.drawable.ic_other)
    }

    private val categoryLabelMap: Map<String, Int> = mapOf(
        "Food" to R.string.category_food,
        "Social" to R.string.category_social,
        "Traffic" to R.string.category_traffic,
        "Shopping" to R.string.category_shopping,
        "Grocery" to R.string.category_grocery,
        "Education" to R.string.category_education,
        "Bills" to R.string.category_bills,
        "Rentals" to R.string.category_rentals,
        "Medical" to R.string.category_medical,
        "Investment" to R.string.category_investment,
        "Gift" to R.string.category_gift,
        "Other" to R.string.category_other,
        "Others" to R.string.category_other,
        "Salary" to R.string.category_salary,
        "Invest" to R.string.category_invest,
        "Business" to R.string.category_business,
        "Interest" to R.string.category_interest,
        "Extra Income" to R.string.category_extra_income,
        "Loan" to R.string.category_loan,
        "Borrow" to R.string.category_borrow
    )

    fun categoriesFor(type: TransactionType): List<CategoryItem> = when (type) {
        TransactionType.EXPENSE -> expenseCategories
        TransactionType.INCOME -> incomeCategories
        TransactionType.LOAN, TransactionType.BORROW -> loanCategories
    }

    @DrawableRes
    fun iconForCategory(categoryName: String?): Int =
        categoryIconMap[categoryName] ?: R.drawable.ic_other

    @StringRes
    fun labelResForCategory(categoryName: String?): Int? = categoryLabelMap[categoryName]

    fun localizedName(context: Context, categoryName: String?): String {
        if (categoryName.isNullOrBlank()) return context.getString(R.string.category)
        val resId = categoryLabelMap[categoryName]
        return if (resId != null) context.getString(resId) else categoryName
    }
}
