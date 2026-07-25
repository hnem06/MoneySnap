package com.devpro58.hnem06.moneysnap.presentation.history

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.load
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.MoneyFormatter
import com.devpro58.hnem06.moneysnap.core.utils.localizedLabel
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import java.io.File

class DayExpensesBottomSheet : BottomSheetDialogFragment() {

    private var dayLabel: String = ""
    private var expenses: List<Expense> = emptyList()
    private var onExpenseSelected: ((Expense) -> Unit)? = null

    override fun getTheme(): Int = R.style.App_BottomSheet_Transparent

    fun setDay(label: String, items: List<Expense>) = apply {
        dayLabel = label
        expenses = items
    }

    fun setOnExpenseSelected(listener: (Expense) -> Unit) = apply {
        onExpenseSelected = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_day_expenses, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.dayExpensesTitle).text =
            getString(R.string.history_day_expenses_title, dayLabel)
        view.findViewById<TextView>(R.id.dayExpensesTotal).text = getString(
            R.string.history_day_total,
            MoneyFormatter.formatVnd(expenses.sumOf { it.amount })
        )
        val list = view.findViewById<LinearLayout>(R.id.dayExpensesContainer)
        expenses.sortedByDescending { it.spentAtMillis }.forEach { expense ->
            list.addView(createExpenseRow(expense))
        }
    }

    private fun createExpenseRow(expense: Expense): View {
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(12).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = requireContext().getColor(R.color.border_gray)
            setCardBackgroundColor(requireContext().getColor(R.color.card_bg))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dismiss()
                onExpenseSelected?.invoke(expense)
            }
        }
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        row.addView(ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
            scaleType = ImageView.ScaleType.CENTER_CROP
            val local = expense.localReceiptPath?.let(::File)?.takeIf(File::exists)
            load(local ?: expense.remoteReceiptUrl) {
                placeholder(R.drawable.ic_camera)
                error(R.drawable.ic_camera)
                fallback(R.drawable.ic_camera)
            }
        })
        row.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            }
            addView(TextView(requireContext()).apply {
                text = expense.title
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.text_dark))
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(requireContext()).apply {
                text = expense.category.localizedLabel(requireContext())
                textSize = 12f
                setTextColor(requireContext().getColor(R.color.text_muted))
            })
        })
        row.addView(TextView(requireContext()).apply {
            text = MoneyFormatter.formatVnd(expense.amount)
            textSize = 14f
            setTextColor(requireContext().getColor(R.color.danger_red))
            setTypeface(typeface, Typeface.BOLD)
        })
        card.addView(row)
        return card
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "DayExpensesBottomSheet"
    }
}
