package com.money.tracker.budget.planner.moneymanager.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.adapter.MonthlyStatisticsAdapter
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.ActivityTransactionDetailsBinding
import com.money.tracker.budget.planner.moneymanager.model.MonthlyStatisticsModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.ui.common.TransactionListSheetHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.YearMonthKey
import java.util.Calendar
import kotlin.math.abs

class StatisticDetailActivity :
    BaseActivity<ActivityTransactionDetailsBinding>(ActivityTransactionDetailsBinding::inflate) {

    private val preferences by lazy { AppPreferences.getInstance(this) }
    private val transactionRepository by lazy { TransactionRepository(preferences) }

    private var allTransactions: List<TransactionModel> = emptyList()
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var tableMode: MonthlyStatisticsAdapter.DisplayMode = MonthlyStatisticsAdapter.DisplayMode.CASHFLOW
    private var currentCurrency: String = "$"

    private lateinit var monthlyAdapter: MonthlyStatisticsAdapter

    override fun setupView(savedInstanceState: Bundle?) {
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        selectedYear = intent.getIntExtra(EXTRA_YEAR, Calendar.getInstance().get(Calendar.YEAR))

        val transactionsJson = intent.getStringExtra(EXTRA_TRANSACTION_LIST)
        allTransactions = if (!transactionsJson.isNullOrEmpty()) {
            transactionRepository.parseListJson(transactionsJson)
        } else {
            transactionRepository.getTransactions()
        }

        monthlyAdapter = MonthlyStatisticsAdapter(::onMonthRowClicked)
        binding.rvMonthlyStatistics.layoutManager = LinearLayoutManager(this)
        binding.rvMonthlyStatistics.adapter = monthlyAdapter

        setupListeners()
        updateTableModeUi()
        renderYearReport()
        loadNativeAd()
    }

    override fun onResume() {
        super.onResume()
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        allTransactions = transactionRepository.getTransactions()
        renderYearReport()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnPrevYear.setOnClickListener {
            selectedYear -= 1
            renderYearReport()
        }
        binding.btnNextYear.setOnClickListener {
            selectedYear += 1
            renderYearReport()
        }

        binding.btnTableCashflow.setOnClickListener {
            if (tableMode != MonthlyStatisticsAdapter.DisplayMode.CASHFLOW) {
                tableMode = MonthlyStatisticsAdapter.DisplayMode.CASHFLOW
                updateTableModeUi()
                renderYearReport()
            }
        }
        binding.btnTableLoan.setOnClickListener {
            if (tableMode != MonthlyStatisticsAdapter.DisplayMode.LOAN) {
                tableMode = MonthlyStatisticsAdapter.DisplayMode.LOAN
                updateTableModeUi()
                renderYearReport()
            }
        }
    }

    private fun updateTableModeUi() {
        val isCashflow = tableMode == MonthlyStatisticsAdapter.DisplayMode.CASHFLOW
        binding.btnTableCashflow.apply {
            setBackgroundResource(if (isCashflow) R.drawable.bg_segmented_selected else android.R.color.transparent)
            setTextColor(compatColor(if (isCashflow) R.color.brand_primary else R.color.text_secondary))
        }
        binding.btnTableLoan.apply {
            setBackgroundResource(if (!isCashflow) R.drawable.bg_segmented_selected else android.R.color.transparent)
            setTextColor(compatColor(if (!isCashflow) R.color.brand_primary else R.color.text_secondary))
        }

        if (isCashflow) {
            binding.tvCol2Header.setText(R.string.income)
            binding.tvCol3Header.setText(R.string.expense)
            binding.tvCol4Header.setText(R.string.balance)
        } else {
            binding.tvCol2Header.setText(R.string.lent)
            binding.tvCol3Header.setText(R.string.borrowed)
            binding.tvCol4Header.setText(R.string.net_loan)
        }
    }

    private fun renderYearReport() {
        binding.tvYear.text = selectedYear.toString()

        val yearTransactions = transactionRepository.filterByYear(allTransactions, selectedYear)
        val cashflow = transactionRepository.calculateCashflow(yearTransactions)

        val net = cashflow.netSavings
        val netPrefix = if (net < 0) "-" else if (net > 0) "+" else ""
        binding.tvTotalBalance.text = "$netPrefix$currentCurrency${FormatUtils.formatAmount(abs(net))}"
        binding.tvTotalBalance.setTextColor(
            compatColor(
                when {
                    net > 0 -> R.color.semantic_income
                    net < 0 -> R.color.semantic_expense
                    else -> R.color.text_primary
                }
            )
        )
        binding.tvIncomeTotal.text = "+$currentCurrency${FormatUtils.formatAmount(cashflow.income)}"
        binding.tvExpendTotal.text = "-$currentCurrency${FormatUtils.formatAmount(cashflow.expense)}"

        val monthlyStatistics = transactionRepository.calculateYearlyMonthlyStatistics(
            context = this,
            transactions = allTransactions,
            targetYear = selectedYear
        )
        monthlyAdapter.submitData(monthlyStatistics, tableMode)
        renderYearlyCashflowChart(monthlyStatistics)
    }

    private fun renderYearlyCashflowChart(monthlyStats: List<MonthlyStatisticsModel>) {
        val incomeEntries = monthlyStats.mapIndexed { idx, m -> BarEntry(idx.toFloat(), m.income.toFloat()) }
        val expenseEntries = monthlyStats.mapIndexed { idx, m -> BarEntry(idx.toFloat(), m.expend.toFloat()) }
        val labels = monthlyStats.map { it.month }

        val incomeSet = BarDataSet(incomeEntries, getString(R.string.income)).apply {
            color = compatColor(R.color.semantic_income)
            setDrawValues(false)
        }
        val expenseSet = BarDataSet(expenseEntries, getString(R.string.expense)).apply {
            color = compatColor(R.color.semantic_expense)
            setDrawValues(false)
        }

        val groupSpace = 0.24f
        val barSpace = 0.04f
        val barWidth = 0.34f
        val barData = BarData(incomeSet, expenseSet).apply {
            this.barWidth = barWidth
        }

        binding.yearlyBarChart.apply {
            clear()
            data = barData
            description.isEnabled = false
            setScaleEnabled(false)
            setDrawGridBackground(false)
            legend.isEnabled = true
            legend.textColor = compatColor(R.color.text_secondary)
            legend.textSize = 11f

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setCenterAxisLabels(true)
                setDrawGridLines(false)
                granularity = 1f
                axisMinimum = 0f
                axisMaximum = 12f
                labelCount = 12
                textColor = compatColor(R.color.text_secondary)
                textSize = 9.5f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        labels.getOrElse(value.toInt()) { "" }
                }
            }
            axisLeft.apply {
                axisMinimum = 0f
                textColor = compatColor(R.color.text_secondary)
                textSize = 10f
                gridColor = compatColor(R.color.divider_light)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        FormatUtils.formatCompactAmount(value.toDouble())
                }
            }
            axisRight.isEnabled = false
            groupBars(0f, groupSpace, barSpace)
            animateY(450)
            invalidate()
        }
    }

    private fun onMonthRowClicked(monthIndex: Int, item: MonthlyStatisticsModel) {
        val monthKey = YearMonthKey(selectedYear, monthIndex)
        val monthTxs = transactionRepository.filterByMonth(allTransactions, monthKey)
            .sortedByDescending { FormatUtils.parseTransactionDate(it.date)?.time ?: 0L }

        val summaryTotal = if (tableMode == MonthlyStatisticsAdapter.DisplayMode.CASHFLOW) {
            val net = item.balance
            val prefix = if (net > 0) "+" else if (net < 0) "-" else ""
            "$prefix$currentCurrency${FormatUtils.formatAmount(abs(net))}"
        } else {
            val netLoan = item.loan - item.borrow
            val prefix = if (netLoan > 0) "+" else if (netLoan < 0) "-" else ""
            "$prefix$currentCurrency${FormatUtils.formatAmount(abs(netLoan))}"
        }

        TransactionListSheetHelper.show(
            context = this,
            title = monthKey.displayLabel(this),
            subtitle = getString(R.string.transactions_count, monthTxs.size),
            totalText = summaryTotal,
            currency = currentCurrency,
            transactions = monthTxs
        ) { tx ->
            val intent = Intent(this, TransactionDetailActivity::class.java).apply {
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, tx)
            }
            startActivity(intent)
        }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_TRANSACTION_LIST = "transactionList"
        const val EXTRA_TRANSACTION_TYPE = "currentTransactionType"
        const val EXTRA_MONTH = "currentMonth"
        const val EXTRA_YEAR = "currentYear"
    }
}
