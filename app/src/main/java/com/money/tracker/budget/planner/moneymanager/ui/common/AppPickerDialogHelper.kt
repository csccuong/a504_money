package com.money.tracker.budget.planner.moneymanager.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.databinding.DialogDatePickerBinding
import com.money.tracker.budget.planner.moneymanager.databinding.DialogTimePickerBinding
import com.money.tracker.budget.planner.moneymanager.databinding.ItemCalendarDayBinding
import com.money.tracker.budget.planner.moneymanager.model.StatisticsFilter
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import com.money.tracker.budget.planner.moneymanager.utils.YearMonthKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppPickerDialogHelper {

    private data class CalendarCell(
        val dayMillis: Long,
        val year: Int,
        val monthIndex: Int,
        val dayOfMonth: Int,
        val isCurrentMonth: Boolean
    )

    fun showDatePicker(
        context: Context,
        initialDate: Date = Date(),
        onDateSelected: (year: Int, monthIndex: Int, dayOfMonth: Int) -> Unit
    ) {
        val binding = DialogDatePickerBinding.inflate(LayoutInflater.from(context))
        val dialog = AppDialogHelper.createStyledDialog(context, binding.root, widthRatio = 0.92f)

        val initCal = Calendar.getInstance().apply { time = initialDate }
        var displayedMonth = YearMonthKey(initCal.get(Calendar.YEAR), initCal.get(Calendar.MONTH))
        var selectedDayMillis = StatisticsFilter.normalizeStartOfDay(initialDate.time)
        val todayMillis = StatisticsFilter.normalizeStartOfDay(System.currentTimeMillis())

        binding.tvPickerTitle.text = context.getString(R.string.select_date)
        binding.btnConfirm.text = context.getString(R.string.save)
        binding.layoutRangePreview.gone()
        bindWeekdayHeaders(context, binding)

        lateinit var adapter: CalendarGridAdapter
        fun renderCalendar() {
            binding.tvMonthYear.text = displayedMonth.displayLabel(context)
            adapter.submit(
                cells = buildMonthGrid(displayedMonth),
                startMillis = selectedDayMillis,
                endMillis = selectedDayMillis,
                todayMillis = todayMillis,
                isRangeMode = false
            )
        }

        adapter = CalendarGridAdapter { cell ->
            selectedDayMillis = cell.dayMillis
            if (!cell.isCurrentMonth) {
                displayedMonth = YearMonthKey(cell.year, cell.monthIndex)
            }
            renderCalendar()
        }

        binding.rvCalendarDays.layoutManager = GridLayoutManager(context, 7)
        binding.rvCalendarDays.adapter = adapter
        binding.rvCalendarDays.itemAnimator = null
        renderCalendar()

        binding.btnPrevMonth.setOnClickListener {
            displayedMonth = displayedMonth.previous()
            renderCalendar()
        }
        binding.btnNextMonth.setOnClickListener {
            displayedMonth = displayedMonth.next()
            renderCalendar()
        }
        binding.btnJumpToday.setOnClickListener {
            displayedMonth = YearMonthKey.current()
            selectedDayMillis = todayMillis
            renderCalendar()
        }
        binding.btnCancel.setOnClickListener { dialog.dismiss() }
        binding.btnConfirm.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDayMillis }
            onDateSelected(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showDateRangePicker(
        context: Context,
        initialStartMillis: Long,
        initialEndMillis: Long,
        onRangeSelected: (startLocalMillis: Long, endLocalMillis: Long) -> Unit
    ) {
        val binding = DialogDatePickerBinding.inflate(LayoutInflater.from(context))
        val dialog = AppDialogHelper.createStyledDialog(context, binding.root, widthRatio = 0.92f)

        val todayMillis = StatisticsFilter.normalizeStartOfDay(System.currentTimeMillis())
        var startDayMillis = StatisticsFilter.normalizeStartOfDay(initialStartMillis)
        var endDayMillis: Long? = StatisticsFilter.normalizeStartOfDay(initialEndMillis)
        if (endDayMillis != null && endDayMillis < startDayMillis) {
            val tmp = startDayMillis
            startDayMillis = endDayMillis
            endDayMillis = tmp
        }

        val initCal = Calendar.getInstance().apply { timeInMillis = endDayMillis ?: startDayMillis }
        var displayedMonth = YearMonthKey(initCal.get(Calendar.YEAR), initCal.get(Calendar.MONTH))

        val locale = LocaleHelper.currentLocale(context)
        val rangeFmt = SimpleDateFormat("dd MMM yyyy", locale)

        binding.tvPickerTitle.text = context.getString(R.string.select_date_range)
        binding.btnConfirm.text = context.getString(R.string.apply)
        binding.layoutRangePreview.show()
        bindWeekdayHeaders(context, binding)

        lateinit var adapter: CalendarGridAdapter
        fun renderRangePreview() {
            binding.tvRangeStart.text = rangeFmt.format(Date(startDayMillis))
            val resolvedEnd = endDayMillis
            binding.tvRangeEnd.text = if (resolvedEnd != null) {
                rangeFmt.format(Date(resolvedEnd))
            } else {
                "…"
            }
        }

        fun renderCalendar() {
            binding.tvMonthYear.text = displayedMonth.displayLabel(context)
            renderRangePreview()
            adapter.submit(
                cells = buildMonthGrid(displayedMonth),
                startMillis = startDayMillis,
                endMillis = endDayMillis,
                todayMillis = todayMillis,
                isRangeMode = true
            )
        }

        adapter = CalendarGridAdapter { cell ->
            val tapped = cell.dayMillis
            if (endDayMillis != null) {
                startDayMillis = tapped
                endDayMillis = null
            } else {
                if (tapped < startDayMillis) {
                    endDayMillis = startDayMillis
                    startDayMillis = tapped
                } else {
                    endDayMillis = tapped
                }
            }
            if (!cell.isCurrentMonth) {
                displayedMonth = YearMonthKey(cell.year, cell.monthIndex)
            }
            renderCalendar()
        }

        binding.rvCalendarDays.layoutManager = GridLayoutManager(context, 7)
        binding.rvCalendarDays.adapter = adapter
        binding.rvCalendarDays.itemAnimator = null
        renderCalendar()

        binding.btnPrevMonth.setOnClickListener {
            displayedMonth = displayedMonth.previous()
            renderCalendar()
        }
        binding.btnNextMonth.setOnClickListener {
            displayedMonth = displayedMonth.next()
            renderCalendar()
        }
        binding.btnJumpToday.setOnClickListener {
            displayedMonth = YearMonthKey.current()
            startDayMillis = todayMillis
            endDayMillis = todayMillis
            renderCalendar()
        }
        binding.btnCancel.setOnClickListener { dialog.dismiss() }
        binding.btnConfirm.setOnClickListener {
            val finalStart = StatisticsFilter.normalizeStartOfDay(startDayMillis)
            val finalEnd = StatisticsFilter.normalizeEndOfDay(endDayMillis ?: startDayMillis)
            onRangeSelected(finalStart, finalEnd)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showTimePicker(
        context: Context,
        initialHour: Int,
        initialMinute: Int,
        onTimeSelected: (hourOfDay: Int, minute: Int) -> Unit
    ) {
        val binding = DialogTimePickerBinding.inflate(LayoutInflater.from(context))
        val dialog = AppDialogHelper.createStyledDialog(context, binding.root, widthRatio = 0.86f)

        var hour = initialHour.coerceIn(0, 23)
        var minute = initialMinute.coerceIn(0, 59)

        val quickChips = listOf(
            binding.chipMin00 to 0,
            binding.chipMin15 to 15,
            binding.chipMin30 to 30,
            binding.chipMin45 to 45
        )

        fun renderTime() {
            binding.tvHourValue.text = String.format(Locale.US, "%02d", hour)
            binding.tvMinuteValue.text = String.format(Locale.US, "%02d", minute)
            quickChips.forEach { (chip, presetMin) ->
                val active = minute == presetMin
                chip.setBackgroundResource(
                    if (active) R.drawable.bg_category_chip_selected else R.drawable.bg_category_chip_normal
                )
                chip.setTextColor(
                    context.compatColor(if (active) R.color.brand_primary else R.color.text_secondary)
                )
            }
        }

        renderTime()

        binding.btnHourUp.setOnClickListener {
            hour = (hour + 1) % 24
            renderTime()
        }
        binding.btnHourDown.setOnClickListener {
            hour = (hour + 23) % 24
            renderTime()
        }
        binding.btnMinuteUp.setOnClickListener {
            minute = (minute + 1) % 60
            renderTime()
        }
        binding.btnMinuteDown.setOnClickListener {
            minute = (minute + 59) % 60
            renderTime()
        }

        quickChips.forEach { (chip, presetMin) ->
            chip.setOnClickListener {
                minute = presetMin
                renderTime()
            }
        }

        binding.btnNow.setOnClickListener {
            val now = Calendar.getInstance()
            hour = now.get(Calendar.HOUR_OF_DAY)
            minute = now.get(Calendar.MINUTE)
            renderTime()
        }

        binding.btnCancel.setOnClickListener { dialog.dismiss() }
        binding.btnConfirm.setOnClickListener {
            onTimeSelected(hour, minute)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun bindWeekdayHeaders(context: Context, binding: DialogDatePickerBinding) {
        val locale = LocaleHelper.currentLocale(context)
        val fmt = SimpleDateFormat("EEE", locale)
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val views = listOf(
            binding.tvWd0, binding.tvWd1, binding.tvWd2, binding.tvWd3,
            binding.tvWd4, binding.tvWd5, binding.tvWd6
        )
        views.forEach { tv ->
            val label = fmt.format(cal.time)
            tv.text = if (label.length > 3) label.take(3) else label
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun buildMonthGrid(month: YearMonthKey): List<CalendarCell> {
        val firstDay = Calendar.getInstance().apply {
            set(Calendar.YEAR, month.year)
            set(Calendar.MONTH, month.monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dow = firstDay.get(Calendar.DAY_OF_WEEK)
        // Monday-first offset: Mon=0, Tue=1, ..., Sun=6
        val leadingDays = (dow + 5) % 7
        firstDay.add(Calendar.DAY_OF_MONTH, -leadingDays)

        val cells = ArrayList<CalendarCell>(42)
        repeat(42) {
            val y = firstDay.get(Calendar.YEAR)
            val m = firstDay.get(Calendar.MONTH)
            val d = firstDay.get(Calendar.DAY_OF_MONTH)
            cells.add(
                CalendarCell(
                    dayMillis = firstDay.timeInMillis,
                    year = y,
                    monthIndex = m,
                    dayOfMonth = d,
                    isCurrentMonth = (y == month.year && m == month.monthIndex)
                )
            )
            firstDay.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cells
    }

    private class CalendarGridAdapter(
        private val onDayClick: (CalendarCell) -> Unit
    ) : RecyclerView.Adapter<CalendarGridAdapter.DayViewHolder>() {

        private var cells: List<CalendarCell> = emptyList()
        private var startMillis: Long = 0L
        private var endMillis: Long? = null
        private var todayMillis: Long = 0L
        private var isRangeMode: Boolean = false

        @SuppressLint("NotifyDataSetChanged")
        fun submit(
            cells: List<CalendarCell>,
            startMillis: Long,
            endMillis: Long?,
            todayMillis: Long,
            isRangeMode: Boolean
        ) {
            this.cells = cells
            this.startMillis = startMillis
            this.endMillis = endMillis
            this.todayMillis = todayMillis
            this.isRangeMode = isRangeMode
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val binding = ItemCalendarDayBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return DayViewHolder(binding)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            holder.bind(cells[position])
        }

        override fun getItemCount(): Int = cells.size

        inner class DayViewHolder(
            private val binding: ItemCalendarDayBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(cell: CalendarCell) {
                val ctx = binding.root.context
                val boldFont = ResourcesCompat.getFont(ctx, R.font.roboto_bold)
                val mediumFont = ResourcesCompat.getFont(ctx, R.font.roboto_medium)

                binding.tvDay.text = cell.dayOfMonth.toString()

                val isStart = cell.dayMillis == startMillis
                val isEnd = endMillis != null && cell.dayMillis == endMillis
                val isSelectedEndpoint = isStart || isEnd
                val isInBetween = isRangeMode &&
                    endMillis != null &&
                    cell.dayMillis > startMillis &&
                    cell.dayMillis < endMillis!!
                val isToday = cell.dayMillis == todayMillis

                if (isInBetween) {
                    binding.viewRangeBand.show()
                } else {
                    binding.viewRangeBand.gone()
                }

                when {
                    isSelectedEndpoint -> {
                        binding.tvDay.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                        binding.tvDay.setTextColor(ctx.compatColor(R.color.white))
                        binding.tvDay.typeface = boldFont
                        binding.tvDay.alpha = 1.0f
                    }
                    isToday -> {
                        binding.tvDay.setBackgroundResource(R.drawable.bg_calendar_day_today)
                        binding.tvDay.setTextColor(ctx.compatColor(R.color.brand_primary))
                        binding.tvDay.typeface = boldFont
                        binding.tvDay.alpha = if (cell.isCurrentMonth) 1.0f else 0.55f
                    }
                    isInBetween -> {
                        binding.tvDay.setBackgroundResource(0)
                        binding.tvDay.setTextColor(ctx.compatColor(R.color.brand_primary_dark))
                        binding.tvDay.typeface = boldFont
                        binding.tvDay.alpha = if (cell.isCurrentMonth) 1.0f else 0.55f
                    }
                    else -> {
                        binding.tvDay.setBackgroundResource(0)
                        binding.tvDay.setTextColor(
                            ctx.compatColor(if (cell.isCurrentMonth) R.color.text_primary else R.color.text_muted)
                        )
                        binding.tvDay.typeface = mediumFont
                        binding.tvDay.alpha = if (cell.isCurrentMonth) 1.0f else 0.48f
                    }
                }

                binding.cellContainer.setOnClickListener { onDayClick(cell) }
            }
        }
    }
}
