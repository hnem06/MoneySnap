package com.devpro58.hnem06.moneysnap.core.utils

import android.content.Context
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory

/**
 * Maps [ExpenseCategory] to a localized display string.
 * Domain layer stays Android-free; this utility bridges domain → UI.
 */
fun ExpenseCategory.localizedLabel(context: Context): String =
    when (this) {
        ExpenseCategory.Food -> context.getString(R.string.category_food)
        ExpenseCategory.Transport -> context.getString(R.string.category_transport)
        ExpenseCategory.Shopping -> context.getString(R.string.category_shopping)
        ExpenseCategory.Entertainment -> context.getString(R.string.category_entertainment)
        ExpenseCategory.Bills -> context.getString(R.string.category_bills)
        ExpenseCategory.Travel -> context.getString(R.string.category_travel)
        ExpenseCategory.Uncategorized -> context.getString(R.string.category_uncategorized)
        ExpenseCategory.Other -> context.getString(R.string.category_other)
    }
