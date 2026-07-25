package com.devpro58.hnem06.moneysnap.core.utils

import android.content.Context
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethod

fun PaymentMethod.localizedName(context: Context): String =
    when (builtInKey) {
        PaymentMethod.BUILT_IN_CASH -> context.getString(R.string.payment_method_cash)
        PaymentMethod.BUILT_IN_BANK_TRANSFER -> context.getString(R.string.payment_method_bank_transfer)
        else -> name
    }
