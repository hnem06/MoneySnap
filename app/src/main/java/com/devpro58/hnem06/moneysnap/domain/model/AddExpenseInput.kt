package com.devpro58.hnem06.moneysnap.domain.model

data class AddExpenseInput(
    val amount: Long,
    val title: String,
    val category: ExpenseCategory,
    val paymentMethod: String? = null,
    val note: String? = null,
    val receiptSourceUri: String? = null,
    val spentAtMillis: Long,
    val type: TransactionType = TransactionType.Expense,
    /** Required when [type] is Income; ignored otherwise. */
    val incomeCategory: IncomeCategory? = null
)
