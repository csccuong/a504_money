package com.money.tracker.budget.planner.moneymanager.fragment

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.activity.MainActivity
import com.money.tracker.budget.planner.moneymanager.activity.StatisticDetailActivity
import com.money.tracker.budget.planner.moneymanager.activity.TransactionDetailActivity
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseFragment
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.CategoryBreakdownItem
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.data.PeriodCashflow
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.FragmentStatisticsBinding
import com.money.tracker.budget.planner.moneymanager.databinding.ItemStatisticsCategoryBinding
import com.money.tracker.budget.planner.moneymanager.databinding.ItemStatisticsSavedBinding
import com.money.tracker.budget.planner.moneymanager.model.BreakdownSortMode
import com.money.tracker.budget.planner.moneymanager.model.StatisticsFilter
import com.money.tracker.budget.planner.moneymanager.model.StatisticsPeriodType
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionType
import com.money.tracker.budget.planner.moneymanager.ui.common.MonthPickerDialog
import com.money.tracker.budget.planner.moneymanager.ui.common.PeriodStepperHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.StatisticsFilterSheetHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.TransactionListSheetHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.TransactionUpdateEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class StatisticsFragment : BaseFragment<FragmentStatisticsBinding>(FragmentStatisticsBinding::inflate) {

    private data class DonutSliceMeta(
        val label: String,
        val amount: Double,
        val percent: Float,
        val accentColor: Int,
        val isPositivePrefix: Boolean = false
    )

    private val preferences by lazy { AppPreferences.getInstance(requireContext()) }
    private val transactionRepository by lazy { TransactionRepository(preferences) }

    private var allTransactions: List<TransactionModel> = emptyList()
    // EXPENSE maps to the default "Overview" (Income Allocation) tab
    private var currentType: TransactionType = TransactionType.EXPENSE
    private var filter: StatisticsFilter = StatisticsFilter()
    private var currentCurrency: String = "$"

    private var currentSliceMetas: List<DonutSliceMeta> = emptyList()
    private var defaultCenterSpannable: CharSequence = ""

    override fun setupView(savedInstanceState: Bundle?) {
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        allTransactions = transactionRepository.getTransactions()

        setupListeners()
        updatePeriodHeaderUi()
        updatePrimaryTabsUi()
        renderStatistics()
    }

    private fun setupListeners() {
        binding.tabOverview.setOnClickListener { selectTransactionType(TransactionType.EXPENSE) }
        binding.tabIncome.setOnClickListener { selectTransactionType(TransactionType.INCOME) }
        binding.tabLoan.setOnClickListener { selectTransactionType(TransactionType.LOAN) }

        binding.btnPeriodMode.setOnClickListener { openPeriodModeSheet() }

        PeriodStepperHelper.bind(
            stepperBinding = binding.periodStepper,
            onPrevious = {
                filter = filter.stepPrevious()
                updatePeriodHeaderUi()
                renderStatistics()
            },
            onNext = {
                filter = filter.stepNext()
                updatePeriodHeaderUi()
                renderStatistics()
            },
            onLabelClick = {
                when (filter.periodType) {
                    StatisticsPeriodType.MONTH -> {
                        MonthPickerDialog.show(requireContext(), filter.anchorMonth) { selectedMonth ->
                            filter = filter.copy(
                                anchorMonth = selectedMonth,
                                anchorYear = selectedMonth.year
                            )
                            updatePeriodHeaderUi()
                            renderStatistics()
                        }
                    }
                    StatisticsPeriodType.CUSTOM -> openCustomRangePicker()
                    StatisticsPeriodType.WEEK, StatisticsPeriodType.YEAR -> openPeriodModeSheet()
                }
            }
        )

        binding.btnAddIncomeHint.setOnClickListener {
            (activity as? MainActivity)?.openAddTransaction(preselectType = TransactionType.INCOME)
        }

        binding.nextStatistic.setOnClickListener {
            binding.nextStatistic.isEnabled = false
            val hostActivity = activity ?: return@setOnClickListener
            AdsManager.loadAndShowInter(
                activity = hostActivity,
                adUnitIdRes = R.string.inter_all,
                isEnabled = RemoteConfigs.inter_all,
                requireFullAds = true
            ) {
                openStatisticDetail()
            }
        }
    }

    private fun openPeriodModeSheet() {
        StatisticsFilterSheetHelper.showPeriodSelector(
            context = requireContext(),
            currentFilter = filter
        ) { updatedFilter ->
            filter = updatedFilter
            updatePeriodHeaderUi()
            renderStatistics()
        }
    }

    private fun openCustomRangePicker() {
        StatisticsFilterSheetHelper.showDateRangePicker(
            context = requireContext(),
            currentFilter = filter
        ) { startLocal, endLocal ->
            filter = filter.copy(
                periodType = StatisticsPeriodType.CUSTOM,
                customStartMillis = startLocal,
                customEndMillis = endLocal
            )
            updatePeriodHeaderUi()
            renderStatistics()
        }
    }

    private fun openStatisticDetail() {
        if (!isAdded) return
        val intent = Intent(requireContext(), StatisticDetailActivity::class.java).apply {
            putExtra(StatisticDetailActivity.EXTRA_TRANSACTION_LIST, transactionRepository.toJson(allTransactions))
            putExtra(StatisticDetailActivity.EXTRA_TRANSACTION_TYPE, currentType.value)
            putExtra(StatisticDetailActivity.EXTRA_YEAR, filter.anchorYear)
        }
        startActivity(intent)
    }

    private fun selectTransactionType(type: TransactionType) {
        if (currentType == type) return
        currentType = type
        updatePrimaryTabsUi()
        renderStatistics()
    }

    private fun updatePrimaryTabsUi() {
        val ctx = requireContext()
        val boldFont = ResourcesCompat.getFont(ctx, R.font.roboto_bold)
        val mediumFont = ResourcesCompat.getFont(ctx, R.font.roboto_medium)
        val inactiveColor = ctx.compatColor(R.color.text_secondary)

        val tabs = listOf(
            Triple(binding.tabOverview, TransactionType.EXPENSE, R.color.brand_primary),
            Triple(binding.tabIncome, TransactionType.INCOME, R.color.semantic_income),
            Triple(binding.tabLoan, TransactionType.LOAN, R.color.brand_primary)
        )
        tabs.forEach { (view, type, activeColorRes) ->
            val active = currentType == type
            view.setBackgroundResource(
                if (active) R.drawable.bg_segmented_selected else android.R.color.transparent
            )
            view.setTextColor(if (active) ctx.compatColor(activeColorRes) else inactiveColor)
            view.typeface = if (active) boldFont else mediumFont
        }

        binding.tvChartTitle.text = when (currentType) {
            TransactionType.EXPENSE -> getString(R.string.income_allocation)
            TransactionType.INCOME -> getString(R.string.income)
            TransactionType.LOAN, TransactionType.BORROW -> getString(R.string.loan)
        }
    }

    private fun updatePeriodHeaderUi() {
        val modeLabelRes = when (filter.periodType) {
            StatisticsPeriodType.WEEK -> R.string.week
            StatisticsPeriodType.MONTH -> R.string.month
            StatisticsPeriodType.YEAR -> R.string.year
            StatisticsPeriodType.CUSTOM -> R.string.custom_range
        }
        binding.tvPeriodMode.setText(modeLabelRes)
        PeriodStepperHelper.setLabel(binding.periodStepper, filter.displayPeriodLabel(requireContext()))
    }

    private fun renderStatistics() {
        val (startMillis, endMillis) = filter.dateRangeMillis()
        val periodTransactions = transactionRepository.filterByDateRange(allTransactions, startMillis, endMillis)
        val cashflow = transactionRepository.calculateCashflow(periodTransactions)

        val filteredForTab = transactionRepository.filterForStatistics(allTransactions, currentType, filter)
        val isOverviewTab = currentType == TransactionType.EXPENSE
        val useIncomeDenominator = isOverviewTab && cashflow.income > 0.0

        val breakdown = transactionRepository.calculateCategoryBreakdown(
            transactions = filteredForTab,
            sortMode = BreakdownSortMode.AMOUNT_DESC,
            percentageDenominator = if (useIncomeDenominator) cashflow.income else null
        )

        renderIntegratedMetrics(cashflow)
        renderPreviousPeriodBadge(cashflow)

        val hasVisualizableData = when {
            isOverviewTab -> breakdown.isNotEmpty() || cashflow.income > 0.0
            else -> breakdown.isNotEmpty()
        }

        if (!hasVisualizableData) {
            binding.layoutNoData.show()
            binding.pieChart.gone()
            binding.btnAddIncomeHint.gone()
            binding.cardBreakdown.gone()
            binding.llCategoryList.removeAllViews()
            return
        }

        binding.layoutNoData.gone()
        binding.pieChart.show()

        if (isOverviewTab && cashflow.income <= 0.0 && breakdown.isNotEmpty()) {
            binding.btnAddIncomeHint.show()
        } else {
            binding.btnAddIncomeHint.gone()
        }

        if (isOverviewTab && cashflow.income > 0.0) {
            renderIncomeAllocationDonut(cashflow, breakdown)
        } else {
            renderStandardDonut(breakdown, filteredForTab.size)
        }

        renderBreakdownCard(cashflow, breakdown, useIncomeDenominator)
    }

    private fun renderIntegratedMetrics(cashflow: PeriodCashflow) {
        val ctx = requireContext()
        if (currentType == TransactionType.LOAN) {
            binding.tvMetric1Label.text = getString(R.string.lent)
            binding.tvMetric1Value.text = "+$currentCurrency${FormatUtils.formatCompactAmount(cashflow.loan)}"
            binding.tvMetric1Value.setTextColor(ctx.compatColor(R.color.semantic_income))
            binding.tvMetric1Sub.gone()

            binding.tvMetric2Label.text = getString(R.string.borrowed)
            binding.tvMetric2Value.text = "-$currentCurrency${FormatUtils.formatCompactAmount(cashflow.borrow)}"
            binding.tvMetric2Value.setTextColor(ctx.compatColor(R.color.semantic_expense))
            binding.tvMetric2Sub.gone()

            binding.tvMetric3Label.text = getString(R.string.net_loan)
            val netLoan = cashflow.netLoan
            val netPrefix = when {
                netLoan > 0.0 -> "+"
                netLoan < 0.0 -> "-"
                else -> ""
            }
            binding.tvTotalBalance.text = "$netPrefix$currentCurrency${FormatUtils.formatCompactAmount(abs(netLoan))}"
            binding.tvTotalBalance.setTextColor(
                ctx.compatColor(if (netLoan < 0.0) R.color.semantic_expense else R.color.brand_primary)
            )
            binding.tvMetric3Sub.gone()
            return
        }

        binding.tvMetric1Label.text = getString(R.string.income)
        binding.tvMetric1Value.text = "+$currentCurrency${FormatUtils.formatCompactAmount(cashflow.income)}"
        binding.tvMetric1Value.setTextColor(ctx.compatColor(R.color.semantic_income))
        binding.tvMetric1Sub.gone()

        binding.tvMetric2Label.text = getString(R.string.spent)
        binding.tvMetric2Value.text = "-$currentCurrency${FormatUtils.formatCompactAmount(cashflow.expense)}"
        binding.tvMetric2Value.setTextColor(ctx.compatColor(R.color.semantic_expense))

        val net = cashflow.netSavings
        val isOverspent = cashflow.income > 0.0 && net < 0.0
        binding.tvMetric3Label.text = getString(if (isOverspent) R.string.overspent else R.string.saved)

        val prefix = when {
            net > 0.0 -> "+"
            net < 0.0 -> "-"
            else -> ""
        }
        binding.tvTotalBalance.text = "$prefix$currentCurrency${FormatUtils.formatCompactAmount(abs(net))}"
        binding.tvTotalBalance.setTextColor(
            ctx.compatColor(
                when {
                    net > 0.0 -> R.color.semantic_income
                    net < 0.0 -> R.color.semantic_expense
                    else -> R.color.text_primary
                }
            )
        )

        if (cashflow.income > 0.0) {
            val spentPct = (cashflow.expense / cashflow.income) * 100.0
            binding.tvMetric2Sub.text = formatPercent(spentPct)
            binding.tvMetric2Sub.setTextColor(ctx.compatColor(R.color.text_secondary))
            binding.tvMetric2Sub.show()

            val savedPct = (net / cashflow.income) * 100.0
            binding.tvMetric3Sub.text = if (savedPct >= 0.0) {
                formatPercent(savedPct)
            } else {
                "-${formatPercent(abs(savedPct))}"
            }
            binding.tvMetric3Sub.setTextColor(
                ctx.compatColor(if (savedPct >= 0.0) R.color.semantic_income else R.color.semantic_expense)
            )
            binding.tvMetric3Sub.show()
        } else {
            binding.tvMetric2Sub.gone()
            binding.tvMetric3Sub.gone()
        }
    }

    private fun renderIncomeAllocationDonut(
        cashflow: PeriodCashflow,
        breakdown: List<CategoryBreakdownItem>
    ) {
        val ctx = requireContext()
        val income = cashflow.income
        val spentTotal = cashflow.expense
        val savedAmount = income - spentTotal
        val isOverspent = savedAmount < 0.0
        val showSavedSlice = !isOverspent && savedAmount > 0.0

        val entries = mutableListOf<PieEntry>()
        val sliceColors = mutableListOf<Int>()
        val sliceMetas = mutableListOf<DonutSliceMeta>()

        breakdown.forEachIndexed { idx, item ->
            val color = EXPENSE_SLICE_COLORS[idx % EXPENSE_SLICE_COLORS.size]
            val name = CategoryCatalog.localizedName(ctx, item.categoryName)
            entries.add(PieEntry(item.amount.toFloat(), name))
            sliceColors.add(color)
            sliceMetas.add(
                DonutSliceMeta(
                    label = name,
                    amount = item.amount,
                    percent = item.percentage,
                    accentColor = color
                )
            )
        }

        if (showSavedSlice) {
            val savedPct = ((savedAmount / income) * 100.0).toFloat()
            val savedColor = ctx.compatColor(R.color.semantic_income)
            val savedLabel = getString(R.string.saved)
            entries.add(PieEntry(savedAmount.toFloat(), savedLabel))
            sliceColors.add(savedColor)
            sliceMetas.add(
                DonutSliceMeta(
                    label = savedLabel,
                    amount = savedAmount,
                    percent = savedPct,
                    accentColor = savedColor,
                    isPositivePrefix = true
                )
            )
        }

        currentSliceMetas = sliceMetas

        defaultCenterSpannable = if (isOverspent) {
            val spentRatio = (spentTotal / income) * 100.0
            buildCenterSpannable(
                primaryLine = formatPercent(spentRatio),
                primaryColor = ctx.compatColor(R.color.semantic_expense),
                secondaryLine = getString(R.string.overspent),
                secondaryColor = ctx.compatColor(R.color.semantic_expense),
                tertiaryLine = "-$currentCurrency${FormatUtils.formatCompactAmount(abs(savedAmount))}",
                tertiaryColor = ctx.compatColor(R.color.text_primary)
            )
        } else {
            val savedRatio = (savedAmount / income) * 100.0
            buildCenterSpannable(
                primaryLine = formatPercent(savedRatio),
                primaryColor = ctx.compatColor(R.color.semantic_income),
                secondaryLine = getString(R.string.saved),
                secondaryColor = ctx.compatColor(R.color.text_secondary),
                tertiaryLine = "+$currentCurrency${FormatUtils.formatCompactAmount(savedAmount)}",
                tertiaryColor = ctx.compatColor(R.color.text_primary)
            )
        }

        applyPieData(entries, sliceColors, isIncomeAllocation = true)
    }

    private fun renderStandardDonut(
        breakdown: List<CategoryBreakdownItem>,
        totalCount: Int
    ) {
        val ctx = requireContext()
        val total = breakdown.sumOf { it.amount }
        val entries = mutableListOf<PieEntry>()
        val sliceColors = mutableListOf<Int>()
        val sliceMetas = mutableListOf<DonutSliceMeta>()

        breakdown.forEachIndexed { idx, item ->
            val palette = if (currentType == TransactionType.INCOME) INCOME_SLICE_COLORS else EXPENSE_SLICE_COLORS
            val color = palette[idx % palette.size]
            val name = CategoryCatalog.localizedName(ctx, item.categoryName)
            entries.add(PieEntry(item.amount.toFloat(), name))
            sliceColors.add(color)
            sliceMetas.add(
                DonutSliceMeta(
                    label = name,
                    amount = item.amount,
                    percent = item.percentage,
                    accentColor = color,
                    isPositivePrefix = currentType == TransactionType.INCOME
                )
            )
        }

        currentSliceMetas = sliceMetas

        val primaryColor = when (currentType) {
            TransactionType.INCOME -> ctx.compatColor(R.color.semantic_income)
            TransactionType.EXPENSE -> ctx.compatColor(R.color.semantic_expense)
            TransactionType.LOAN, TransactionType.BORROW -> ctx.compatColor(R.color.brand_primary)
        }
        defaultCenterSpannable = buildCenterSpannable(
            primaryLine = "$currentCurrency${FormatUtils.formatCompactAmount(total)}",
            primaryColor = primaryColor,
            secondaryLine = currentType.localizedLabel(ctx),
            secondaryColor = ctx.compatColor(R.color.text_secondary),
            tertiaryLine = getString(R.string.transactions_count, totalCount),
            tertiaryColor = ctx.compatColor(R.color.text_muted)
        )

        applyPieData(entries, sliceColors, isIncomeAllocation = false)
    }

    private fun applyPieData(
        entries: List<PieEntry>,
        sliceColors: List<Int>,
        isIncomeAllocation: Boolean
    ) {
        val ctx = requireContext()
        val dataSet = PieDataSet(entries, "").apply {
            colors = sliceColors
            sliceSpace = if (entries.size > 1) 2.8f else 0f
            selectionShift = 6f
            setDrawValues(false)
        }

        binding.pieChart.apply {
            clear()
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 70f
            transparentCircleRadius = 74f
            setHoleColor(Color.WHITE)
            setCenterTextSize(13f)
            centerText = defaultCenterSpannable
            setRotationEnabled(true)
            setHighlightPerTapEnabled(true)

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val idx = h?.x?.toInt() ?: return
                    val meta = currentSliceMetas.getOrNull(idx) ?: return
                    val prefix = if (meta.isPositivePrefix) "+" else ""
                    val pctText = if (isIncomeAllocation) {
                        getString(R.string.of_income_value, formatPercent(meta.percent.toDouble()))
                    } else {
                        formatPercent(meta.percent.toDouble())
                    }
                    centerText = buildCenterSpannable(
                        primaryLine = formatPercent(meta.percent.toDouble()),
                        primaryColor = meta.accentColor,
                        secondaryLine = meta.label,
                        secondaryColor = ctx.compatColor(R.color.text_primary),
                        tertiaryLine = "$prefix$currentCurrency${FormatUtils.formatCompactAmount(meta.amount)}\n$pctText",
                        tertiaryColor = ctx.compatColor(R.color.text_secondary)
                    )
                }

                override fun onNothingSelected() {
                    centerText = defaultCenterSpannable
                }
            })

            animateY(420)
            invalidate()
        }
    }

    private fun buildCenterSpannable(
        primaryLine: String,
        primaryColor: Int,
        secondaryLine: String,
        secondaryColor: Int,
        tertiaryLine: String,
        tertiaryColor: Int
    ): CharSequence {
        val builder = SpannableStringBuilder()

        val pStart = builder.length
        builder.append(primaryLine)
        builder.setSpan(StyleSpan(Typeface.BOLD), pStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(RelativeSizeSpan(1.55f), pStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(primaryColor), pStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        builder.append("\n")
        val sStart = builder.length
        builder.append(secondaryLine)
        builder.setSpan(StyleSpan(Typeface.BOLD), sStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(RelativeSizeSpan(0.92f), sStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(secondaryColor), sStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        builder.append("\n")
        val tStart = builder.length
        builder.append(tertiaryLine)
        builder.setSpan(RelativeSizeSpan(0.88f), tStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(tertiaryColor), tStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return builder
    }

    private fun renderBreakdownCard(
        cashflow: PeriodCashflow,
        breakdown: List<CategoryBreakdownItem>,
        useIncomeDenominator: Boolean
    ) {
        val ctx = requireContext()
        val inflater = LayoutInflater.from(ctx)
        binding.llCategoryList.removeAllViews()

        val isOverviewTab = currentType == TransactionType.EXPENSE
        val showSavedHighlight = isOverviewTab && cashflow.income > 0.0 && cashflow.netSavings > 0.0

        if (!showSavedHighlight && breakdown.isEmpty()) {
            binding.cardBreakdown.gone()
            return
        }

        binding.cardBreakdown.show()
        if (useIncomeDenominator) {
            binding.tvBreakdownBasis.text = getString(R.string.of_income_value, "%")
            binding.tvBreakdownBasis.show()
        } else {
            binding.tvBreakdownBasis.gone()
        }

        // 1. Highlighted Saved Row at top when user saved money from income
        if (showSavedHighlight) {
            val savedAmount = cashflow.netSavings
            val savedPct = (savedAmount / cashflow.income) * 100.0
            val savedBinding = ItemStatisticsSavedBinding.inflate(inflater, binding.llCategoryList, false)
            savedBinding.tvSavedTitle.text = getString(R.string.saved)
            savedBinding.tvSavedSubtitle.text = getString(R.string.of_income_value, formatPercent(savedPct))
            savedBinding.tvSavedAmount.text = "+$currentCurrency${FormatUtils.formatAmount(savedAmount)}"
            savedBinding.tvSavedPercent.text = formatPercent(savedPct)
            savedBinding.progressSavedShare.setProgressCompat(savedPct.roundToInt().coerceIn(1, 100), false)
            binding.llCategoryList.addView(savedBinding.root)
        }

        // 2. Category Breakdown Rows (Strictly matching donut clockwise order)
        val palette = if (currentType == TransactionType.INCOME) INCOME_SLICE_COLORS else EXPENSE_SLICE_COLORS
        breakdown.forEachIndexed { index, item ->
            val itemBinding = ItemStatisticsCategoryBinding.inflate(inflater, binding.llCategoryList, false)
            val color = palette[index % palette.size]
            val localizedName = CategoryCatalog.localizedName(ctx, item.categoryName)

            itemBinding.ivCategoryIcon.setImageResource(CategoryCatalog.iconForCategory(item.categoryName))
            itemBinding.tvCategoryName.text = localizedName
            itemBinding.tvCategoryMeta.text = getString(R.string.transactions_count, item.transactionCount)
            itemBinding.tvCategoryAmount.text = "$currentCurrency${FormatUtils.formatAmount(item.amount)}"
            itemBinding.tvCategoryPercent.text = formatPercent(item.percentage.toDouble())
            itemBinding.progressCategoryShare.setIndicatorColor(color)
            itemBinding.progressCategoryShare.setProgressCompat(item.percentage.roundToInt().coerceIn(1, 100), false)

            itemBinding.root.setOnClickListener {
                val periodText = filter.displayFullPeriodLabel(ctx)
                TransactionListSheetHelper.show(
                    context = ctx,
                    title = localizedName,
                    subtitle = "$periodText • ${getString(R.string.transactions_count, item.transactionCount)}",
                    totalText = "$currentCurrency${FormatUtils.formatAmount(item.amount)}",
                    iconRes = CategoryCatalog.iconForCategory(item.categoryName),
                    currency = currentCurrency,
                    transactions = item.transactions,
                    onTransactionClick = ::openTransactionDetail
                )
            }

            binding.llCategoryList.addView(itemBinding.root)
        }
    }

    private fun renderPreviousPeriodBadge(currentCashflow: PeriodCashflow) {
        if (currentType == TransactionType.LOAN) {
            binding.tvComparisonBadge.gone()
            return
        }
        val (prevStart, prevEnd) = filter.previousDateRangeMillis()
        val prevTransactions = transactionRepository.filterByDateRange(allTransactions, prevStart, prevEnd)
        val prevCashflow = transactionRepository.calculateCashflow(prevTransactions)

        val currentVal = if (currentType == TransactionType.EXPENSE) currentCashflow.expense else currentCashflow.income
        val prevVal = if (currentType == TransactionType.EXPENSE) prevCashflow.expense else prevCashflow.income

        if (prevVal <= 0.0) {
            binding.tvComparisonBadge.gone()
            return
        }

        val ctx = requireContext()
        val deltaPct = (((currentVal - prevVal) / prevVal) * 100.0).roundToInt()
        when {
            deltaPct == 0 -> {
                binding.tvComparisonBadge.text = getString(R.string.vs_prev_period_same)
                binding.tvComparisonBadge.setBackgroundResource(R.drawable.bg_badge_blue)
                binding.tvComparisonBadge.setTextColor(ctx.compatColor(R.color.semantic_info))
            }
            deltaPct > 0 -> {
                binding.tvComparisonBadge.text = getString(R.string.vs_prev_period_up, deltaPct)
                val isGood = currentType == TransactionType.INCOME
                binding.tvComparisonBadge.setBackgroundResource(if (isGood) R.drawable.bg_badge_green else R.drawable.bg_badge_red)
                binding.tvComparisonBadge.setTextColor(ctx.compatColor(if (isGood) R.color.semantic_income else R.color.semantic_expense))
            }
            else -> {
                binding.tvComparisonBadge.text = getString(R.string.vs_prev_period_down, abs(deltaPct))
                val isGood = currentType == TransactionType.EXPENSE
                binding.tvComparisonBadge.setBackgroundResource(if (isGood) R.drawable.bg_badge_green else R.drawable.bg_badge_red)
                binding.tvComparisonBadge.setTextColor(ctx.compatColor(if (isGood) R.color.semantic_income else R.color.semantic_expense))
            }
        }
        binding.tvComparisonBadge.show()
    }

    private fun formatPercent(value: Double): String {
        val rounded = String.format(Locale.US, "%.1f", value)
        return if (rounded.endsWith(".0")) "${rounded.dropLast(2)}%" else "$rounded%"
    }

    private fun openTransactionDetail(tx: TransactionModel) {
        val intent = Intent(requireContext(), TransactionDetailActivity::class.java).apply {
            putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, tx)
        }
        startActivity(intent)
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
        binding.nextStatistic.isEnabled = true
        currentCurrency = preferences.getSelectedCurrencyOrDefault()
        allTransactions = transactionRepository.getTransactions()
        renderStatistics()
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
            renderStatistics()
        }
    }

    private companion object {
        // Distinct expense colors synced to Figma palette (reserving pure green #34C759 for "Saved")
        val EXPENSE_SLICE_COLORS = listOf(
            Color.parseColor("#6155F5"), // Figma Indigo
            Color.parseColor("#FC5E82"), // Figma Coral Pink
            Color.parseColor("#F97316"), // Orange
            Color.parseColor("#3B82F6"), // Blue
            Color.parseColor("#9A93F6"), // Figma Periwinkle
            Color.parseColor("#F59E0B"), // Amber
            Color.parseColor("#EC4899"), // Pink
            Color.parseColor("#06B6D4"), // Cyan
            Color.parseColor("#8B5CF6"), // Violet
            Color.parseColor("#D946EF"), // Fuchsia
            Color.parseColor("#E11D48"), // Rose
            Color.parseColor("#0284C7")  // Sky
        )

        val INCOME_SLICE_COLORS = listOf(
            Color.parseColor("#34C759"), // Figma Green
            Color.parseColor("#6155F5"), // Figma Indigo
            Color.parseColor("#3FCD96"), // Figma Mint
            Color.parseColor("#9A93F6"), // Figma Periwinkle
            Color.parseColor("#06B6D4"), // Cyan
            Color.parseColor("#3B82F6")  // Blue
        )
    }
}
