package com.devpro58.hnem06.moneysnap.domain.model

data class HistoryFilter(
    val query: String = "",
    val category: ExpenseCategory? = null,
    val dateRangeStart: Long? = null,
    val dateRangeEnd: Long? = null,
    val amountMin: Long? = null,
    val amountMax: Long? = null,
    /** null shows both directions. */
    val type: TransactionType? = null
)
