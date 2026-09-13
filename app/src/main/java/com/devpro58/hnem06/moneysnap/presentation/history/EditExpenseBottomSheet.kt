package com.devpro58.hnem06.moneysnap.presentation.history

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.VndAmountFormatter
import com.devpro58.hnem06.moneysnap.core.utils.enableVndAmountFormatting
import com.devpro58.hnem06.moneysnap.core.utils.localizedName
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethod
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Bottom sheet for editing an existing expense.
 *
 * The expense is passed through [arguments] rather than a setter. With a setter the field was
 * null after the system recreated the fragment — a configuration change or process death — and
 * the sheet silently dismissed itself, losing whatever the user had typed. Arguments survive
 * both.
 *
 * [onSave] stays a callback because the host fragment owns the ViewModel that performs the
 * update; it is re-attached by the host on every show.
 */
class EditExpenseBottomSheet : BottomSheetDialogFragment() {

    private var onSave: ((Expense) -> Unit)? = null
    private var paymentMethods: List<PaymentMethod> = emptyList()
    private var selectedSpentAtMillis: Long = 0L

    private val expense: Expense?
        get() = @Suppress("DEPRECATION") arguments?.getSerializable(ARG_EXPENSE) as? Expense

    override fun getTheme(): Int = R.style.App_BottomSheet_Transparent

    fun setOnSaveListener(listener: (Expense) -> Unit): EditExpenseBottomSheet {
        this.onSave = listener
        return this
    }

    /** Supplies the managed payment methods the dropdown offers. */
    fun setPaymentMethods(methods: List<PaymentMethod>): EditExpenseBottomSheet {
        this.paymentMethods = methods
        return this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_edit_expense, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exp = expense ?: run { dismiss(); return }

        val amountInput = view.findViewById<EditText>(R.id.editAmountInput)
        val titleInput = view.findViewById<EditText>(R.id.editTitleInput)
        val chipGroup = view.findViewById<ChipGroup>(R.id.editCategoryChipGroup)
        val paymentDropdown =
            view.findViewById<MaterialAutoCompleteTextView>(R.id.editPaymentMethodDropdown)
        val dateInput = view.findViewById<TextView>(R.id.editDateInput)
        val noteInput = view.findViewById<EditText>(R.id.editNoteInput)
        val cancelBtn = view.findViewById<MaterialButton>(R.id.editCancelButton)
        val saveBtn = view.findViewById<MaterialButton>(R.id.editSaveButton)

        // Restore an in-progress date edit across recreation rather than snapping back.
        selectedSpentAtMillis =
            savedInstanceState?.getLong(STATE_SPENT_AT, exp.spentAtMillis) ?: exp.spentAtMillis

        amountInput.enableVndAmountFormatting()
        amountInput.setText(VndAmountFormatter.format(exp.amount))
        amountInput.setSelection(amountInput.text.length)
        titleInput.setText(exp.title)
        noteInput.setText(exp.note.orEmpty())
        checkCategoryChip(chipGroup, exp.category)

        bindPaymentMethods(paymentDropdown, exp.paymentMethod)
        renderDate(dateInput)
        dateInput.setOnClickListener { showDatePicker(dateInput) }

        cancelBtn.setOnClickListener { dismiss() }

        saveBtn.setOnClickListener {
            val newAmount = VndAmountFormatter.parse(amountInput.text)
            if (newAmount == null || newAmount <= 0) {
                Toast.makeText(requireContext(), R.string.error_amount_empty, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val newTitle = titleInput.text.toString().trim()
            if (newTitle.isEmpty()) {
                Toast.makeText(requireContext(), R.string.error_title_empty, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // updatedAtMillis and syncStatus are set by ExpenseRepositoryImpl.updateExpense —
            // stamping them here would let a caller that forgets to do so write an edit that
            // sync treats as already-pushed.
            val updated = exp.copy(
                amount = newAmount,
                title = newTitle,
                category = selectedCategory(chipGroup),
                paymentMethod = paymentDropdown.text.toString().trim().ifEmpty { null },
                note = noteInput.text.toString().trim().ifEmpty { null },
                spentAtMillis = selectedSpentAtMillis
            )
            onSave?.invoke(updated)
            dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_SPENT_AT, selectedSpentAtMillis)
    }

    private fun bindPaymentMethods(
        dropdown: MaterialAutoCompleteTextView,
        current: String?
    ) {
        val labels = paymentMethods.map { it.localizedName(requireContext()) }
        dropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        )
        // Keep whatever the expense already carries even if that method has since been renamed
        // or deleted, so editing an old expense never silently rewrites its payment method.
        dropdown.setText(current.orEmpty(), false)
    }

    private fun renderDate(dateInput: TextView) {
        dateInput.text = DATE_FORMAT.format(selectedSpentAtMillis)
    }

    private fun showDatePicker(dateInput: TextView) {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedSpentAtMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedSpentAtMillis = Calendar.getInstance().apply {
                    timeInMillis = selectedSpentAtMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }.timeInMillis
                renderDate(dateInput)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // An expense cannot have been paid in the future.
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun checkCategoryChip(chipGroup: ChipGroup, category: ExpenseCategory) {
        val chipId = when (category) {
            ExpenseCategory.Food -> R.id.editCategoryFood
            ExpenseCategory.Transport -> R.id.editCategoryTransport
            ExpenseCategory.Shopping -> R.id.editCategoryShopping
            ExpenseCategory.Entertainment -> R.id.editCategoryEntertainment
            ExpenseCategory.Bills -> R.id.editCategoryBills
            ExpenseCategory.Travel -> R.id.editCategoryTravel
            // Uncategorized is the receipt parser's fallback, not a user choice.
            ExpenseCategory.Uncategorized,
            ExpenseCategory.Other -> R.id.editCategoryOther
        }
        chipGroup.check(chipId)
    }

    private fun selectedCategory(chipGroup: ChipGroup): ExpenseCategory =
        when (chipGroup.checkedChipId) {
            R.id.editCategoryFood -> ExpenseCategory.Food
            R.id.editCategoryTransport -> ExpenseCategory.Transport
            R.id.editCategoryShopping -> ExpenseCategory.Shopping
            R.id.editCategoryEntertainment -> ExpenseCategory.Entertainment
            R.id.editCategoryBills -> ExpenseCategory.Bills
            R.id.editCategoryTravel -> ExpenseCategory.Travel
            else -> ExpenseCategory.Other
        }

    companion object {
        const val TAG = "EditExpenseBottomSheet"
        private const val ARG_EXPENSE = "expense"
        private const val STATE_SPENT_AT = "spent_at"
        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun newInstance(expense: Expense): EditExpenseBottomSheet =
            EditExpenseBottomSheet().apply {
                arguments = Bundle().apply { putSerializable(ARG_EXPENSE, expense) }
            }
    }
}
