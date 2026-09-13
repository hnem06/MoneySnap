package com.devpro58.hnem06.moneysnap.domain.model

/**
 * Whether a record is money out or money in.
 *
 * Added to [Expense] rather than introducing a separate `Transaction` model: the two would have
 * shared 13 of 15 fields, and a second model means a second Firestore collection, a second sync
 * worker and a second reconcile — doubling the most fragile part of the codebase to buy a
 * discriminator one enum already provides.
 *
 * [Expense] is the default for every document and row written before this existed, which is not a
 * fallback but the correct reading: the app only tracked spending until now.
 */
enum class TransactionType {
    Expense,
    Income
}
