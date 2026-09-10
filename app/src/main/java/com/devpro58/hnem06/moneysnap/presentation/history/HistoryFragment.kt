package com.devpro58.hnem06.moneysnap.presentation.history

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.VndAmountFormatter
import com.devpro58.hnem06.moneysnap.core.utils.enableVndAmountFormatting
import com.devpro58.hnem06.moneysnap.core.utils.localizedLabel
import com.devpro58.hnem06.moneysnap.core.utils.MoneyFormatter
import com.devpro58.hnem06.moneysnap.core.utils.setupTopAppBarNavigation
import com.devpro58.hnem06.moneysnap.databinding.FragmentHistoryBinding
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.devpro58.hnem06.moneysnap.presentation.expense.add.AddExpenseFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HistoryViewModel
    private val displayedMonth = Calendar.getInstance()
    private var allExpenses: List<Expense> = emptyList()
    private var selectedCategory: ExpenseCategory? = null
    private var selectedDateRange: Pair<Long, Long>? = null
    private var selectedAmountRange: Pair<Long?, Long?>? = null
    private var historyMode = HistoryMode.Calendar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        setupTopAppBarNavigation(view)

        displayedMonth.set(Calendar.DAY_OF_MONTH, 1)
        binding.previousMonthButton.setOnClickListener { changeMonth(-1) }
        binding.nextMonthButton.setOnClickListener { changeMonth(1) }
        binding.calendarModeButton.isSelected = true
        binding.calendarModeButton.setOnClickListener { setHistoryMode(HistoryMode.Calendar) }
        binding.searchModeButton.setOnClickListener { setHistoryMode(HistoryMode.Search) }
        setupSearchAndFilters()

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HistoryUiState.Content -> {
                    allExpenses = state.expenses
                    renderCurrentMode()
                }
                is HistoryUiState.Error -> Toast.makeText(
                    requireContext(), R.string.error_history_load_failed, Toast.LENGTH_SHORT
                ).show()
                HistoryUiState.Loading -> Unit
            }
        }
        observeResults()
    }

    private fun setHistoryMode(mode: HistoryMode) {
        if (historyMode == mode) return
        historyMode = mode
        val showCalendar = mode == HistoryMode.Calendar
        binding.calendarModeButton.isSelected = showCalendar
        binding.searchModeButton.isSelected = !showCalendar
        binding.monthNavigationContainer.visibility = if (showCalendar) View.VISIBLE else View.GONE
        binding.monthSummaryContainer.visibility = if (showCalendar) View.VISIBLE else View.GONE
        binding.weekdaysContainer.visibility = if (showCalendar) View.VISIBLE else View.GONE
        binding.calendarGrid.visibility = if (showCalendar) View.VISIBLE else View.GONE
        binding.historySearchInputLayout.visibility = if (showCalendar) View.GONE else View.VISIBLE
        binding.historyFilterScroll.visibility = if (showCalendar) View.GONE else View.VISIBLE
        binding.searchResultsContainer.visibility = if (showCalendar) View.GONE else View.VISIBLE

        if (showCalendar) {
            binding.historySearchInput.setText("")
            clearFilters()
            binding.historySearchInput.clearFocus()
        }
        renderCurrentMode()
    }

    private fun renderCurrentMode() {
        when (historyMode) {
            HistoryMode.Calendar -> renderMonth()
            HistoryMode.Search -> renderSearchResults()
        }
    }

    private fun setupSearchAndFilters() {
        binding.historySearchInput.doAfterTextChanged { editable ->
            viewModel.updateSearch(editable?.toString().orEmpty())
        }
        binding.allFilterChip.setOnClickListener { clearFilters() }
        binding.dateFilterChip.setOnClickListener { showDateRangePicker() }
        binding.categoryFilterChip.setOnClickListener { showCategoryPicker() }
        binding.amountFilterChip.setOnClickListener { showAmountRangeDialog() }
    }

    private fun clearFilters() {
        selectedCategory = null
        selectedDateRange = null
        selectedAmountRange = null
        viewModel.clearFilters()
        binding.dateFilterChip.apply {
            text = getString(R.string.history_filter_date)
            isChecked = false
        }
        binding.categoryFilterChip.apply {
            text = getString(R.string.history_filter_category)
            isChecked = false
        }
        binding.amountFilterChip.apply {
            text = getString(R.string.history_filter_amount)
            isChecked = false
        }
        updateAllFilterChip()
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.history_date_title)
            .apply { selectedDateRange?.let { setSelection(androidx.core.util.Pair(it.first, it.second)) } }
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first ?: return@addOnPositiveButtonClickListener
            val end = selection.second ?: return@addOnPositiveButtonClickListener
            val endOfDay = end + DAY_MILLIS - 1
            selectedDateRange = start to end
            displayedMonth.timeInMillis = start
            displayedMonth.set(Calendar.DAY_OF_MONTH, 1)
            viewModel.updateDateRange(start, endOfDay)
            binding.dateFilterChip.apply {
                text = getString(
                    R.string.history_date_value,
                    formatFilterDate(start),
                    formatFilterDate(end)
                )
                isChecked = true
            }
            updateAllFilterChip()
        }
        picker.show(childFragmentManager, DATE_PICKER_TAG)
    }

    private fun showCategoryPicker() {
        val categories = ExpenseCategory.entries
        val labels = categories.map { it.localizedLabel(requireContext()) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.history_category_title)
            .setSingleChoiceItems(labels, selectedCategory?.let(categories::indexOf) ?: -1) { dialog, which ->
                selectedCategory = categories[which]
                viewModel.updateCategory(selectedCategory)
                binding.categoryFilterChip.apply {
                    text = labels[which]
                    isChecked = true
                }
                updateAllFilterChip()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAmountRangeDialog() {
        val fields = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }
        val minInput = amountInput(R.string.history_amount_min).apply {
            selectedAmountRange?.first?.let { setText(VndAmountFormatter.format(it)) }
        }
        val maxInput = amountInput(R.string.history_amount_max).apply {
            selectedAmountRange?.second?.let { setText(VndAmountFormatter.format(it)) }
        }
        fields.addView(minInput)
        fields.addView(maxInput, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) })

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.history_amount_title)
            .setView(fields)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.history_filter_clear, null)
            .setPositiveButton(R.string.history_filter_apply, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                selectedAmountRange = null
                viewModel.updateAmountRange(null, null)
                binding.amountFilterChip.apply {
                    text = getString(R.string.history_filter_amount)
                    isChecked = false
                }
                updateAllFilterChip()
                dialog.dismiss()
            }
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val min = VndAmountFormatter.parse(minInput.text)
                val max = VndAmountFormatter.parse(maxInput.text)
                if (min != null && max != null && max < min) {
                    maxInput.error = getString(R.string.history_amount_invalid)
                    return@setOnClickListener
                }
                selectedAmountRange = min to max
                viewModel.updateAmountRange(min, max)
                binding.amountFilterChip.apply {
                    text = when {
                        min != null && max != null -> getString(R.string.history_amount_value, min, max)
                        min != null -> getString(R.string.history_amount_from, min)
                        max != null -> getString(R.string.history_amount_to, max)
                        else -> getString(R.string.history_filter_amount)
                    }
                    isChecked = min != null || max != null
                }
                updateAllFilterChip()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun amountInput(hintRes: Int) = EditText(requireContext()).apply {
        hint = getString(hintRes)
        inputType = InputType.TYPE_CLASS_NUMBER
        setTextColor(color(R.color.text_dark))
        setHintTextColor(color(R.color.text_muted))
        background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_input_rounded)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        enableVndAmountFormatting()
    }

    private fun updateAllFilterChip() {
        binding.allFilterChip.isChecked =
            selectedCategory == null && selectedDateRange == null && selectedAmountRange == null
    }

    private fun formatFilterDate(timestamp: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp)

    private fun changeMonth(offset: Int) {
        displayedMonth.add(Calendar.MONTH, offset)
        renderMonth()
    }

    private fun renderSearchResults() {
        binding.searchResultsContainer.removeAllViews()
        if (allExpenses.isEmpty()) {
            binding.searchResultsContainer.addView(TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(96)
                )
                gravity = Gravity.CENTER
                text = getString(R.string.history_search_empty)
                textSize = 14f
                setTextColor(color(R.color.text_muted))
            })
            return
        }

        allExpenses.forEachIndexed { index, expense ->
            binding.searchResultsContainer.addView(createSearchResultRow(expense))
            if (index < allExpenses.lastIndex) {
                binding.searchResultsContainer.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                    ).apply {
                        marginStart = dp(16)
                        marginEnd = dp(16)
                    }
                    setBackgroundColor(color(R.color.border_gray))
                })
            }
        }
    }

    private fun createSearchResultRow(expense: Expense): View =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(76)
            )
            setOnClickListener { showExpenseDetail(expense) }

            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                addView(TextView(requireContext()).apply {
                    text = expense.title
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(color(R.color.text_dark))
                    maxLines = 1
                })
                addView(TextView(requireContext()).apply {
                    text = getString(
                        R.string.history_search_item_meta,
                        expense.category.localizedLabel(requireContext()),
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expense.spentAtMillis)
                    )
                    textSize = 12f
                    setTextColor(color(R.color.text_muted))
                    maxLines = 1
                })
            })
            addView(TextView(requireContext()).apply {
                text = MoneyFormatter.formatVnd(expense.amount)
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color(R.color.danger_red))
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                maxLines = 1
            })
        }

    private fun renderMonth() {
        val monthStart = displayedMonth.clone() as Calendar
        monthStart.set(Calendar.DAY_OF_MONTH, 1)
        monthStart.set(Calendar.HOUR_OF_DAY, 0)
        monthStart.set(Calendar.MINUTE, 0)
        monthStart.set(Calendar.SECOND, 0)
        monthStart.set(Calendar.MILLISECOND, 0)
        val nextMonth = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val monthExpenses = allExpenses.filter {
            it.spentAtMillis in monthStart.timeInMillis until nextMonth.timeInMillis
        }

        binding.monthTitle.text = SimpleDateFormat("MMMM, yyyy", Locale.getDefault())
            .format(monthStart.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        binding.expenseTotal.text = getString(
            R.string.history_expense_total_value,
            MoneyFormatter.formatVnd(monthExpenses.sumOf { it.amount })
        )

        val byDay = monthExpenses.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.spentAtMillis }
                .get(Calendar.DAY_OF_MONTH)
        }
        renderCalendarGrid(monthStart, byDay)
    }

    private fun renderCalendarGrid(
        monthStart: Calendar,
        expensesByDay: Map<Int, List<Expense>>
    ) {
        binding.calendarGrid.removeAllViews()
        val mondayBasedOffset = (monthStart.get(Calendar.DAY_OF_WEEK) + 5) % 7
        repeat(mondayBasedOffset) { binding.calendarGrid.addView(createEmptyGridCell()) }

        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            binding.calendarGrid.addView(createDayCell(monthStart, day, expensesByDay[day].orEmpty()))
        }
        val trailingCells = (7 - ((mondayBasedOffset + daysInMonth) % 7)) % 7
        repeat(trailingCells) { binding.calendarGrid.addView(createEmptyGridCell()) }
    }

    private fun createDayCell(month: Calendar, day: Int, expenses: List<Expense>): View {
        val isToday = isToday(month, day)
        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(78)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
        }
        val preview = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
            background = roundedBackground(
                if (isToday) R.color.calendar_today_bg else R.color.icon_square_bg,
                if (isToday) R.color.calendar_today_border else null
            )
            clipToOutline = true
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (expenses.isEmpty()) {
                    navigateToAddExpense(month, day)
                } else {
                    openDay(expenses, month, day)
                }
            }
        }

        val receiptSource = expenses.firstNotNullOfOrNull { expense ->
            expense.localReceiptPath?.let(::File)?.takeIf(File::exists)
                ?: expense.remoteReceiptUrl
        }
        if (receiptSource != null) {
            preview.addView(ImageView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                load(receiptSource) { error(R.drawable.ic_camera) }
            })
        } else {
            preview.addView(TextView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                text = "+"
                textSize = 27f
                setTextColor(color(if (isToday) R.color.calendar_today_accent else R.color.text_muted))
            })
        }
        if (expenses.size > 1) preview.addView(createCountBadge(expenses.size))

        val dayLabel = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
            text = day.toString()
            textSize = 12f
            gravity = Gravity.CENTER
            setTypeface(typeface, if (isToday) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(color(if (isToday) R.color.calendar_today_accent else R.color.text_muted))
        }
        column.addView(preview)
        column.addView(dayLabel)
        return column
    }

    private fun navigateToAddExpense(month: Calendar, day: Int) {
        val selectedDay = (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, day)
        }
        findNavController().navigate(
            R.id.addExpenseFragment,
            Bundle().apply {
                putLong(AddExpenseFragment.ARG_SPENT_AT_MILLIS, selectedDay.timeInMillis)
            }
        )
    }

    private fun createCountBadge(count: Int) = TextView(requireContext()).apply {
        layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(2)
            marginEnd = dp(2)
        }
        gravity = Gravity.CENTER
        text = count.toString()
        textSize = 10f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color(R.color.accent_green))
        }
    }

    private fun createEmptyGridCell() = View(requireContext()).apply {
        layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(78)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }
    }

    private fun openDay(expenses: List<Expense>, month: Calendar, day: Int) {
        if (expenses.size == 1) {
            showExpenseDetail(expenses.first())
            return
        }
        val date = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.time
        DayExpensesBottomSheet()
            .setDay(SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault()).format(date), expenses)
            .setOnExpenseSelected(::showExpenseDetail)
            .show(childFragmentManager, DayExpensesBottomSheet.TAG)
    }

    private fun showExpenseDetail(expense: Expense) {
        ExpenseDetailBottomSheet()
            .setExpense(expense)
            .setOnDeleteListener(viewModel::deleteExpense)
            .setOnEditListener(::showEditSheet)
            .setOnRetrySyncListener(viewModel::retrySync)
            .show(childFragmentManager, ExpenseDetailBottomSheet.TAG)
    }

    private fun showEditSheet(expense: Expense) {
        EditExpenseBottomSheet()
            .setExpense(expense)
            .setOnSaveListener(viewModel::updateExpense)
            .show(childFragmentManager, EditExpenseBottomSheet.TAG)
    }

    private fun observeResults() {
        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            Toast.makeText(
                requireContext(),
                if (result.isSuccess) R.string.delete_success else R.string.delete_failed,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.consumeDeleteResult()
        }
        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            Toast.makeText(
                requireContext(),
                if (result.isSuccess) R.string.edit_success else R.string.edit_failed,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.consumeUpdateResult()
        }
    }

    private fun isToday(month: Calendar, day: Int): Boolean {
        val today = Calendar.getInstance()
        return day == today.get(Calendar.DAY_OF_MONTH) &&
            month.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
            month.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    }

    private fun roundedBackground(fillColor: Int, borderColor: Int?): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(color(fillColor))
            borderColor?.let { setStroke(dp(2), color(it)) }
        }

    private fun color(colorRes: Int) = ContextCompat.getColor(requireContext(), colorRes)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        const val DATE_PICKER_TAG = "history-date-range"
    }

    private enum class HistoryMode {
        Calendar,
        Search
    }
}
