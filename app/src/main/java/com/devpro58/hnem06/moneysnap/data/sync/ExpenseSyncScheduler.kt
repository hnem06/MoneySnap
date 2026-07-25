package com.devpro58.hnem06.moneysnap.data.sync

interface ExpenseSyncScheduler {
    fun enqueueExpenseSync(expenseId: String)
}
