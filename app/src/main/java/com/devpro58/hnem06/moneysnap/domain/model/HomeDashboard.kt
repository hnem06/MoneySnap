package com.devpro58.hnem06.moneysnap.domain.model

data class HomeDashboard(
    val monthlyTotal: Long,
    val todayTotal: Long,
    val monthlyExpenseCount: Int,
    /** Spend over the whole previous calendar month, for the month-over-month indicator. */
    val previousMonthTotal: Long,
    val recentExpenses: List<Expense>
)
