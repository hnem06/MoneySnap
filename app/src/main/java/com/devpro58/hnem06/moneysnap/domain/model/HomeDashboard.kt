package com.devpro58.hnem06.moneysnap.domain.model

/**
 * `monthlyTotal` was renamed to [monthlyExpenseTotal] deliberately: once income exists, "total"
 * is ambiguous, and an ambiguous name in financial code is worse than the handful of call sites
 * the rename touches.
 */
data class HomeDashboard(
    val monthlyExpenseTotal: Long,
    val monthlyIncomeTotal: Long,
    val todayExpenseTotal: Long,
    val monthlyExpenseCount: Int,
    /** Spending over the whole previous calendar month, for the month-over-month indicator. */
    val previousMonthExpenseTotal: Long,
    val recentTransactions: List<Expense>
) {
    /** Income minus spending for the current month. Negative means spending outpaced income. */
    val monthlyBalance: Long get() = monthlyIncomeTotal - monthlyExpenseTotal
}
