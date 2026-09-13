package com.devpro58.hnem06.moneysnap.domain.model

/**
 * Categories for money coming in. Separate from [ExpenseCategory] because the two never overlap —
 * "Salary" is not a kind of spending and "Food" is not a source of income — and mixing them would
 * put nonsense options in both pickers.
 *
 * Both are persisted in the single `category` column, discriminated by [TransactionType]. That
 * keeps the schema at one column with one index, and leaves every existing
 * `when (expense.category)` in the UI compiling unchanged.
 */
enum class IncomeCategory(
    val label: String
) {
    Salary("Salary"),
    Bonus("Bonus"),
    Gift("Gift"),
    Investment("Investment"),
    Refund("Refund"),
    OtherIncome("Other")
}
