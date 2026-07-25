package com.devpro58.hnem06.moneysnap.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.VndAmountFormatter
import com.devpro58.hnem06.moneysnap.core.utils.enableVndAmountFormatting
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup

/**
 * Bottom sheet for editing an existing expense.
 * Pre-fills fields from the provided [Expense] and calls
 * [onSave] with the updated copy.
 */
class EditExpenseBottomSheet : BottomSheetDialogFragment() {

    private var expense: Expense? = null
    private var onSave: ((Expense) -> Unit)? = null

    override fun getTheme(): Int = R.style.App_BottomSheet_Transparent

    fun setExpense(expense: Expense): EditExpenseBottomSheet {
        this.expense = expense
        return this
    }

    fun setOnSaveListener(listener: (Expense) -> Unit): EditExpenseBottomSheet {
        this.onSave = listener
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
        val paymentInput = view.findViewById<EditText>(R.id.editPaymentMethodInput)
        val noteInput = view.findViewById<EditText>(R.id.editNoteInput)
        val cancelBtn = view.findViewById<MaterialButton>(R.id.editCancelButton)
        val saveBtn = view.findViewById<MaterialButton>(R.id.editSaveButton)

        // Pre-fill
        amountInput.enableVndAmountFormatting()
        amountInput.setText(VndAmountFormatter.format(exp.amount))
        amountInput.setSelection(amountInput.text.length)
        titleInput.setText(exp.title)
        paymentInput.setText(exp.paymentMethod.orEmpty())
        noteInput.setText(exp.note.orEmpty())
        checkCategoryChip(chipGroup, exp.category)

        cancelBtn.setOnClickListener { dismiss() }

        saveBtn.setOnClickListener {
            val newAmount = VndAmountFormatter.parse(amountInput.text)
            if (newAmount == null || newAmount <= 0) {
                Toast.makeText(requireContext(), R.string.error_amount_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newTitle = titleInput.text.toString().trim()
            if (newTitle.isEmpty()) {
                Toast.makeText(requireContext(), R.string.error_title_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updated = exp.copy(
                amount = newAmount,
                title = newTitle,
                category = selectedCategory(chipGroup),
                paymentMethod = paymentInput.text.toString().trim().ifEmpty { null },
                note = noteInput.text.toString().trim().ifEmpty { null },
                updatedAtMillis = System.currentTimeMillis()
            )
            onSave?.invoke(updated)
            dismiss()
        }
    }

    private fun checkCategoryChip(chipGroup: ChipGroup, category: ExpenseCategory) {
        val chipId = when (category) {
            ExpenseCategory.Food -> R.id.editCategoryFood
            ExpenseCategory.Transport -> R.id.editCategoryTransport
            ExpenseCategory.Shopping -> R.id.editCategoryShopping
            ExpenseCategory.Entertainment -> R.id.editCategoryEntertainment
            ExpenseCategory.Bills -> R.id.editCategoryBills
            else -> R.id.editCategoryOther
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
            else -> ExpenseCategory.Other
        }

    companion object {
        const val TAG = "EditExpenseBottomSheet"
    }
}
