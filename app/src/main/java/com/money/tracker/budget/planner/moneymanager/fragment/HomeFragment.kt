package com.money.tracker.budget.planner.moneymanager.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.activity.AllTransactionsActivity
import com.money.tracker.budget.planner.moneymanager.activity.MainActivity
import com.money.tracker.budget.planner.moneymanager.activity.TransactionDetailActivity
import com.money.tracker.budget.planner.moneymanager.adapter.HomeRecentTransactionAdapter
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseFragment
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.HomeOverviewSeries
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.FragmentHomeBinding
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionType
import com.money.tracker.budget.planner.moneymanager.ui.common.MonthPickerDialog
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.TransactionUpdateEvent
import com.money.tracker.budget.planner.moneymanager.utils.YearMonthKey
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val preferences by lazy { AppPreferences.getInstance(requireContext()) }
    private val transactionRepository by lazy { TransactionRepository(preferences) }
    private val recentAdapter by lazy { HomeRecentTransactionAdapter(::openTransactionDetail) }

    private var allTransactions: List<TransactionModel> = emptyList()
    private var currentMonth: YearMonthKey = YearMonthKey.current()
    private var currentCurrency: String = "$"
    private var formattedBalanceText: String = "$0"
    private var isBalanceVisible: Boolean = true

    private val transactionDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshFromStorage()
        }
    }

    override fun setupView(savedInstanceState: Bundle?) {
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = recentAdapter
        binding.rvTransactions.itemAnimator = null

        configureOverviewChart()
        loadInitialTransactions()
        setupListeners()
        renderDashboard()
    }

    private fun configureOverviewChart() {
        val ctx = requireContext()
        binding.chartOverview.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setExtraOffsets(6f, 10f, 6f, 6f)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f
                isGranularityEnabled = true
                textSize = 11f
                textColor = ctx.compatColor(R.color.figma_nav_inactive)
                yOffset = 6f
            }

            axisLeft.apply {
                isEnabled = false
                axisMinimum = 0f
                spaceTop = 22f
            }
            axisRight.isEnabled = false
        }
    }

    private fun loadInitialTransactions() {
        val json = arguments?.getString(ARG_TRANSACTION_LIST)
        allTransactions = if (!json.isNullOrEmpty()) {
            transactionRepository.parseListJson(json)
        } else {
            transactionRepository.getTransactions()
        }
    }

    private fun setupListeners() {
        binding.ivEditBalance.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            renderBalanceVisibility()
        }

        binding.cardBentoExpense.setOnClickListener { openAllTransactions(TransactionType.EXPENSE) }
        binding.cardBentoIncome.setOnClickListener { openAllTransactions(TransactionType.INCOME) }
        binding.cardBentoLoan.setOnClickListener { openAllTransactions(TransactionType.LOAN) }

        binding.btnBalanceMonthPill.setOnClickListener { showMonthPicker() }
        binding.btnOverviewMonth.setOnClickListener { showMonthPicker() }

        binding.btnViewAll.setOnClickListener { openAllTransactions(null) }
        binding.btnViewAllFooter.setOnClickListener { openAllTransactions(null) }

        binding.btnEmptyAdd.setOnClickListener {
            (activity as? MainActivity)?.openAddTransaction()
        }
    }

    private fun showMonthPicker() {
        MonthPickerDialog.show(requireContext(), currentMonth) { selectedMonth ->
            currentMonth = selectedMonth
            renderDashboard()
        }
    }

    private fun renderDashboard() {
        renderHeroAndBentoCards()
        renderOverviewCard()
        renderRecentTransactions()
    }

    private fun renderHeroAndBentoCards() {
        val ctx = requireContext()
        val curSummary = transactionRepository.calculateSummary(allTransactions, currentMonth)
        val prevSummary = transactionRepository.calculateSummary(allTransactions, currentMonth.previous())

        formattedBalanceText = "$currentCurrency${FormatUtils.formatAmount(curSummary.totalBalance)}"
        renderBalanceVisibility()

        // Row 3: Comparison trend (+8.2% vs last month) when previous month has data,
        // otherwise frosted clickable month pill ([Nov 2026 ∨]) to keep card height balanced and allow quick month switching.
        val curNet = curSummary.monthIncome - curSummary.monthExpense
        val prevNet = prevSummary.monthIncome - prevSummary.monthExpense
        val hasComparisonData =
            abs(prevNet) > 0.0 && (curSummary.monthIncome > 0.0 || curSummary.monthExpense > 0.0)

        if (hasComparisonData) {
            val pct = ((curNet - prevNet) / abs(prevNet)) * 100.0
            val formattedPct = formatCompactPercent(abs(pct))
            binding.btnBalanceMonthPill.gone()
            binding.layoutBalanceTrend.show()
            binding.tvTrendCaption.text = getString(R.string.vs_last_month)
            binding.ivTrendArrow.show()
            if (pct >= 0.0) {
                binding.tvTrendPercent.text = "+$formattedPct"
                binding.tvTrendPercent.setTextColor(ctx.compatColor(R.color.figma_green))
                binding.ivTrendArrow.setColorFilter(ctx.compatColor(R.color.figma_green))
                binding.ivTrendArrow.scaleY = 1f
            } else {
                binding.tvTrendPercent.text = "-$formattedPct"
                binding.tvTrendPercent.setTextColor(ctx.compatColor(R.color.semantic_expense_on_dark))
                binding.ivTrendArrow.setColorFilter(ctx.compatColor(R.color.semantic_expense_on_dark))
                binding.ivTrendArrow.scaleY = -1f
            }
        } else {
            binding.layoutBalanceTrend.gone()
            binding.btnBalanceMonthPill.show()
            binding.tvBalanceMonthPill.text = currentMonth.shortDisplayLabel(ctx)
        }

        // 3 Pastel Bento Cards
        binding.tvBentoExpense.text = "$currentCurrency${FormatUtils.formatAmount(curSummary.monthExpense)}"
        binding.tvBentoIncome.text = "$currentCurrency${FormatUtils.formatAmount(curSummary.monthIncome)}"
        binding.tvBentoLoan.text = "$currentCurrency${FormatUtils.formatAmount(curSummary.monthLoan)}"
    }

    private fun formatCompactPercent(value: Double): String {
        val raw = String.format(Locale.US, "%.1f%%", value)
        return if (raw.endsWith(".0%")) raw.replace(".0%", "%") else raw
    }

    private fun renderOverviewCard() {
        val series = transactionRepository.calculateHomeOverviewSeries(allTransactions, currentMonth)

        // Keep Overview card always visible so the Month Picker pill is always reachable
        binding.cardOverview.show()
        binding.tvOverviewMonth.text = resolveOverviewMonthLabel()

        if (!series.hasData) {
            binding.layoutOverviewChartContent.gone()
            binding.layoutOverviewEmpty.show()
            return
        }

        binding.layoutOverviewEmpty.gone()
        binding.layoutOverviewChartContent.show()
        bindOverviewLineChart(series)
    }

    private fun resolveOverviewMonthLabel(): String {
        val now = YearMonthKey.current()
        return when (currentMonth) {
            now -> getString(R.string.this_month)
            now.previous() -> getString(R.string.last_month)
            else -> currentMonth.shortDisplayLabel(requireContext())
        }
    }

    private fun bindOverviewLineChart(series: HomeOverviewSeries) {
        val ctx = requireContext()
        val incomeColor = ctx.compatColor(R.color.figma_income_accent)
        val expenseColor = ctx.compatColor(R.color.figma_expense_accent)

        val incomeEntries = series.incomeValues.mapIndexed { i, v -> Entry(i.toFloat(), v) }
        val expenseEntries = series.expenseValues.mapIndexed { i, v -> Entry(i.toFloat(), v) }

        val incomePeakIdx = findPeakIndex(series.incomeValues)
        val expensePeakIdx = findPeakIndex(series.expenseValues)

        val incomeSet = LineDataSet(incomeEntries, getString(R.string.income)).apply {
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            cubicIntensity = 0.18f
            color = incomeColor
            lineWidth = 2.4f
            setDrawValues(false)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(ctx, R.drawable.bg_figma_chart_fill_income)
            setDrawHorizontalHighlightIndicator(false)
            highLightColor = incomeColor
            highlightLineWidth = 1.2f
            enableDashedHighlightLine(10f, 6f, 0f)
            applyPeakDotStyle(series.incomeValues.size, incomePeakIdx, incomeColor)
        }

        val expenseSet = LineDataSet(expenseEntries, getString(R.string.expense)).apply {
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            cubicIntensity = 0.18f
            color = expenseColor
            lineWidth = 2.4f
            setDrawValues(false)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(ctx, R.drawable.bg_figma_chart_fill_expense)
            setDrawHorizontalHighlightIndicator(false)
            highLightColor = expenseColor
            highlightLineWidth = 1.2f
            enableDashedHighlightLine(10f, 6f, 0f)
            applyPeakDotStyle(series.expenseValues.size, expensePeakIdx, expenseColor)
        }

        val maxVal = maxOf(
            series.incomeValues.maxOrNull() ?: 0f,
            series.expenseValues.maxOrNull() ?: 0f
        )

        binding.chartOverview.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(series.labels)
            xAxis.setLabelCount(series.labels.size, false)
            axisLeft.axisMaximum = if (maxVal > 0f) maxVal * 1.22f else 10f
            marker = OverviewMarkerView(ctx, series, currentCurrency)
            data = LineData(expenseSet, incomeSet)

            if (maxVal > 0f) {
                val maxIncome = series.incomeValues.maxOrNull() ?: 0f
                val maxExpense = series.expenseValues.maxOrNull() ?: 0f
                if (maxIncome >= maxExpense && incomePeakIdx >= 0) {
                    highlightValue(incomePeakIdx.toFloat(), 1, false)
                } else if (expensePeakIdx >= 0) {
                    highlightValue(expensePeakIdx.toFloat(), 0, false)
                }
            } else {
                highlightValues(null)
            }
            invalidate()
        }
    }

    private fun LineDataSet.applyPeakDotStyle(size: Int, peakIdx: Int, accentColor: Int) {
        if (peakIdx in 0 until size) {
            setDrawCircles(true)
            setDrawCircleHole(true)
            circleRadius = 5f
            circleHoleRadius = 2.5f
            circleHoleColor = Color.WHITE
            circleColors = List(size) { idx -> if (idx == peakIdx) accentColor else Color.TRANSPARENT }
        } else {
            setDrawCircles(false)
        }
    }

    private fun findPeakIndex(values: FloatArray): Int {
        var bestIdx = -1
        var bestVal = 0f
        values.forEachIndexed { idx, v ->
            if (v > bestVal) {
                bestVal = v
                bestIdx = idx
            }
        }
        return bestIdx
    }

    private fun renderRecentTransactions() {
        val monthTransactions = transactionRepository.filterTransactions(
            transactions = allTransactions,
            yearMonth = currentMonth,
            typeFilter = null
        )

        val totalCount = monthTransactions.size
        binding.tvViewAll.text = getString(R.string.view_all)

        if (monthTransactions.isEmpty()) {
            recentAdapter.submitList(emptyList())
            binding.rvTransactions.gone()
            binding.btnViewAllFooter.gone()
            binding.layoutNoData.show()
            return
        }

        binding.layoutNoData.gone()
        binding.rvTransactions.show()

        val recentSlice = monthTransactions.take(RECENT_LIMIT)
        recentAdapter.submitList(HomeRecentTransactionAdapter.mapItems(recentSlice, currentCurrency))

        if (totalCount > RECENT_LIMIT) {
            binding.btnViewAllFooter.text = getString(R.string.view_all_transactions_count, totalCount)
            binding.btnViewAllFooter.show()
        } else {
            binding.btnViewAllFooter.gone()
        }
    }

    private fun renderBalanceVisibility() {
        if (isBalanceVisible) {
            binding.tvTotalBalance.text = formattedBalanceText
            binding.ivEditBalance.setImageResource(R.drawable.ic_visibility_off)
        } else {
            binding.tvTotalBalance.text = "••••••"
            binding.ivEditBalance.setImageResource(R.drawable.ic_visibility)
        }
    }

    private fun openAllTransactions(typeFilter: TransactionType?) {
        val intent = AllTransactionsActivity.createIntent(
            context = requireContext(),
            yearMonth = currentMonth,
            typeFilter = typeFilter,
            allTime = false
        )
        startActivity(intent)
    }

    private fun openTransactionDetail(transaction: TransactionModel) {
        val intent = Intent(requireContext(), TransactionDetailActivity::class.java).apply {
            putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
        }
        transactionDetailLauncher.launch(intent)
    }

    private fun refreshFromStorage() {
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        allTransactions = transactionRepository.getTransactions()
        renderDashboard()
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
            layoutRes = R.layout.layout_native_small_home,
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
        refreshFromStorage()
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
            renderDashboard()
        }
    }

    private class OverviewMarkerView(
        context: Context,
        private val series: HomeOverviewSeries,
        private val currency: String
    ) : MarkerView(context, R.layout.view_overview_marker) {

        private val tvIncome: TextView = findViewById(R.id.tv_marker_income)
        private val tvExpense: TextView = findViewById(R.id.tv_marker_expense)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            val idx = e?.x?.roundToInt() ?: 0
            val inc = series.incomeValues.getOrElse(idx) { 0f }.toDouble()
            val exp = series.expenseValues.getOrElse(idx) { 0f }.toDouble()
            tvIncome.text = "+$currency${FormatUtils.formatCompactAmount(inc)}"
            tvExpense.text = "-$currency${FormatUtils.formatCompactAmount(exp)}"
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -height - 12f)
    }

    companion object {
        const val ARG_TRANSACTION_LIST = "transactionList"
        private const val RECENT_LIMIT = 6
    }
}
