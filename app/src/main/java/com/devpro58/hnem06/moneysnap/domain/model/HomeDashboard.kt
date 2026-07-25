package com.devpro58.hnem06.moneysnap.domain.model

data class HomeDashboard(
    val monthlyTotal: Long,
    val todayTotal: Long,
    val monthlyExpenseCount: Int,
    val recentExpenses: List<Expense>
)
