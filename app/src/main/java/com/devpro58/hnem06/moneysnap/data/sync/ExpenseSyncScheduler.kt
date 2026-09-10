package com.devpro58.hnem06.moneysnap.data.sync

interface ExpenseSyncScheduler {
    /** Pushes a single expense as soon as the network allows. */
    fun enqueueExpenseSync(expenseId: String)

    /** Immediately sweeps every row that still owes the backend work. Safe to call repeatedly. */
    fun enqueuePendingSync()

    /** Installs the recurring sweep. Idempotent — an existing schedule is kept, not replaced. */
    fun ensurePeriodicSync()

    /** Cancels all scheduled sync work. Used on sign-out so one user's queue cannot run under another. */
    fun cancelAllSync()
}
