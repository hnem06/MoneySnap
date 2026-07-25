package com.devpro58.hnem06.moneysnap.presentation.stats

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.MoneyFormatter
import com.devpro58.hnem06.moneysnap.core.utils.localizedLabel
import com.devpro58.hnem06.moneysnap.core.utils.setupTopAppBarNavigation
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class StatsFragment : Fragment() {

    private lateinit var viewModel: StatsViewModel

    private var weekTotals: Map<Int, Long> = emptyMap()
    private var selectedWeek: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[StatsViewModel::class.java]
        setupTopAppBarNavigation(
            root = view,
            toolbarId = R.id.statsTopAppBar,
            avatarId = R.id.statsProfileAvatar
        )

        val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        view.findViewById<TextView>(R.id.statsMonthLabel).text =
            monthFmt.format(Calendar.getInstance().time)

        selectedWeek = Calendar.getInstance().get(Calendar.WEEK_OF_MONTH)

        viewModel.expenses.observe(viewLifecycleOwner) { expenses ->
            bindSummary(view, expenses)
            bindMonthlyChart(view, expenses)
            bindCategoryBreakdown(view, expenses)
            bindWeeklyTrends(view, expenses)
        }
    }

    // region Summary (hero)

    private fun bindSummary(view: View, expenses: List<Expense>) {
        val (monthStart, monthEnd) = currentMonthRange()
        val cal = Calendar.getInstance().apply {
            timeInMillis = monthStart
            add(Calendar.MONTH, -1)
        }
        val lastMonthStart = cal.timeInMillis

        val thisMonth = expenses.filter { it.spentAtMillis in monthStart until monthEnd }
        val lastMonth = expenses.filter { it.spentAtMillis in lastMonthStart until monthStart }

        val total = thisMonth.sumOf { it.amount }
        val lastTotal = lastMonth.sumOf { it.amount }
        val daysElapsed = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val avgDaily = if (daysElapsed > 0) total / daysElapsed else 0L

        view.findViewById<TextView>(R.id.statsMonthlyTotal).text = MoneyFormatter.formatVnd(total)
        view.findViewById<TextView>(R.id.statsAvgDaily).text = MoneyFormatter.formatVnd(avgDaily)

        val vsLabel = view.findViewById<TextView>(R.id.statsVsLastMonth)
        if (lastTotal > 0) {
            val pct = ((total - lastTotal) * 100) / lastTotal
            val arrow = if (pct >= 0) "↑" else "↓"
            vsLabel.text = "$arrow ${kotlin.math.abs(pct)}%"
            vsLabel.setTextColor(
                color(if (pct <= 0) R.color.primary_green else R.color.danger_red)
            )
        } else {
            vsLabel.text = "—"
            vsLabel.setTextColor(color(R.color.text_muted))
        }

        val topCat = thisMonth.groupBy { it.category }
            .maxByOrNull { (_, items) -> items.sumOf { it.amount } }
        view.findViewById<TextView>(R.id.statsHighestCategory).text =
            topCat?.key?.localizedLabel(requireContext()) ?: "—"
    }

    // endregion

    // region Monthly chart

    private fun bindMonthlyChart(view: View, expenses: List<Expense>) {
        val chart = view.findViewById<SpendingLineChartView>(R.id.statsLineChart)
        val (monthStart, monthEnd) = currentMonthRange()
        val thisMonth = expenses.filter { it.spentAtMillis in monthStart until monthEnd }
        val daysElapsed = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val dailyTotals = MutableList(daysElapsed) { 0L }
        val expenseCalendar = Calendar.getInstance()

        thisMonth.forEach { expense ->
            expenseCalendar.timeInMillis = expense.spentAtMillis
            val dayIndex = expenseCalendar.get(Calendar.DAY_OF_MONTH) - 1
            if (dayIndex in dailyTotals.indices) {
                dailyTotals[dayIndex] += expense.amount
            }
        }

        chart.setValues(dailyTotals)
        bindChartAxisLabels(view, daysElapsed)
    }

    /**
     * X-axis ticks matching the chart's actual span (day 1 → today, stretched
     * across the full width). Tick days are taken at even fractions of that span
     * so they line up with the drawn points.
     */
    private fun bindChartAxisLabels(view: View, daysElapsed: Int) {
        val container = view.findViewById<LinearLayout>(R.id.statsChartAxisLabels)
        container.removeAllViews()

        val tickDays = listOf(0f, 1f / 3f, 2f / 3f, 1f)
            .map { fraction -> 1 + ((daysElapsed - 1) * fraction).toInt() }
            .distinct()

        tickDays.forEachIndexed { index, day ->
            val isToday = index == tickDays.lastIndex
            val label = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = if (isToday) getString(R.string.stats_chart_today)
                    else getString(R.string.stats_chart_day, day)
                textSize = 10f
                gravity = when (index) {
                    0 -> Gravity.START
                    tickDays.lastIndex -> Gravity.END
                    else -> Gravity.CENTER
                }
                if (isToday) {
                    setTextColor(color(R.color.secondary_green))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                } else {
                    setTextColor(color(R.color.text_muted))
                }
            }
            container.addView(label)
        }
    }

    // endregion

    // region Category breakdown

    private fun bindCategoryBreakdown(view: View, expenses: List<Expense>) {
        val container = view.findViewById<LinearLayout>(R.id.statsCategoryList)
        val emptyView = view.findViewById<TextView>(R.id.statsCategoryEmpty)
        val donutChart = view.findViewById<CategoryDonutChartView>(R.id.statsDonutChart)
        container.removeAllViews()

        val (monthStart, monthEnd) = currentMonthRange()
        val thisMonth = expenses.filter { it.spentAtMillis in monthStart until monthEnd }

        if (thisMonth.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            donutChart.setSegments(emptyList())
            return
        }
        emptyView.visibility = View.GONE

        val categoryTotals = thisMonth.groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }

        donutChart.setSegments(
            categoryTotals.map { (category, amount) ->
                CategoryDonutChartView.Segment(color(categoryColorRes(category)), amount)
            }
        )

        categoryTotals.take(4).forEach { (category, amount) ->
            container.addView(buildCategoryRow(category, amount))
        }
    }

    private fun buildCategoryRow(category: ExpenseCategory, amount: Long): View {
        val catColor = color(categoryColorRes(category))

        val item = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, dp(7))
        }

        val dot = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply {
                marginEnd = dp(10)
            }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle_green)
            background.setTint(catColor)
        }

        val label = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text = category.localizedLabel(requireContext())
            setTextColor(color(R.color.text_dark))
            textSize = 14f
            maxLines = 1
        }

        val value = TextView(requireContext()).apply {
            text = MoneyFormatter.formatVnd(amount)
            setTextColor(color(R.color.primary_green))
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        item.addView(dot)
        item.addView(label)
        item.addView(value)
        return item
    }

    /** Fixed color per category — color follows the entity, never its rank. */
    private fun categoryColorRes(category: ExpenseCategory): Int = when (category) {
        ExpenseCategory.Food -> R.color.cat_food
        ExpenseCategory.Transport -> R.color.cat_transport
        ExpenseCategory.Shopping -> R.color.cat_shopping
        ExpenseCategory.Entertainment -> R.color.cat_entertainment
        ExpenseCategory.Bills -> R.color.cat_bills
        ExpenseCategory.Travel -> R.color.cat_travel
        ExpenseCategory.Other -> R.color.cat_other
        ExpenseCategory.Uncategorized -> R.color.cat_uncategorized
    }

    // endregion

    // region Weekly trends

    private fun bindWeeklyTrends(view: View, expenses: List<Expense>) {
        val (monthStart, monthEnd) = currentMonthRange()
        val thisMonth = expenses.filter { it.spentAtMillis in monthStart until monthEnd }

        val cal = Calendar.getInstance()
        val totals = mutableMapOf<Int, Long>()
        for (exp in thisMonth) {
            cal.timeInMillis = exp.spentAtMillis
            val week = cal.get(Calendar.WEEK_OF_MONTH)
            totals[week] = (totals[week] ?: 0L) + exp.amount
        }
        weekTotals = totals

        renderWeeklyBars(view)
    }

    private fun renderWeeklyBars(view: View) {
        val barsContainer = view.findViewById<LinearLayout>(R.id.statsWeeklyBarsContainer)
        val labelsContainer = view.findViewById<LinearLayout>(R.id.statsWeeklyLabels)
        barsContainer.removeAllViews()
        labelsContainer.removeAllViews()

        val maxWeek = 5
        val maxAmount = weekTotals.values.maxOrNull() ?: 1L
        val maxBarHeight = dp(120)

        for (w in 1..maxWeek) {
            val amount = weekTotals[w] ?: 0L
            val ratio = if (maxAmount > 0) amount.toFloat() / maxAmount else 0f
            val barHeight = (ratio * maxBarHeight).toInt().coerceAtLeast(dp(4))
            val isSelected = w == selectedWeek

            // Cell: value label above the bar; tap to select
            val cell = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener {
                    if (selectedWeek != w) {
                        selectedWeek = w
                        renderWeeklyBars(view)
                    }
                }
            }

            val valueLabel = TextView(requireContext()).apply {
                text = compactVnd(amount)
                textSize = 10f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(color(R.color.primary_green))
                gravity = Gravity.CENTER
                visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(4) }
            }

            val bar = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, barHeight
                ).apply {
                    marginStart = dp(8)
                    marginEnd = dp(8)
                }
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_bar_rounded_top)
                background.setTint(
                    color(if (isSelected) R.color.primary_green else R.color.bar_inactive)
                )
            }

            cell.addView(valueLabel)
            cell.addView(bar)
            barsContainer.addView(cell)

            val weekLabel = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = getString(R.string.stats_week_label, w)
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(color(if (isSelected) R.color.primary_green else R.color.text_muted))
                if (isSelected) setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            labelsContainer.addView(weekLabel)
        }
    }

    /** Short money form for bar labels: 2.450.000 -> "2,45tr", 450.000 -> "450k". */
    private fun compactVnd(amount: Long): String = when {
        amount >= 1_000_000L -> {
            val millions = amount / 1_000_000.0
            val suffix = getString(R.string.stats_compact_million_suffix)
            if (millions % 1.0 == 0.0) "${millions.toLong()}$suffix"
            else String.format(Locale.getDefault(), "%.2f%s", millions, suffix)
        }
        amount >= 1_000L -> "${amount / 1_000}${getString(R.string.stats_compact_thousand_suffix)}"
        else -> amount.toString()
    }

    // endregion

    private fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun color(res: Int): Int = ContextCompat.getColor(requireContext(), res)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
