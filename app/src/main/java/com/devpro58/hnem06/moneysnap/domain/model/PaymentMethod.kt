package com.devpro58.hnem06.moneysnap.domain.model

data class PaymentMethod(
    val id: String,
    val userId: String,
    val name: String,
    val builtInKey: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    val isBuiltIn: Boolean get() = builtInKey != null

    companion object {
        const val BUILT_IN_CASH = "cash"
        const val BUILT_IN_BANK_TRANSFER = "bank_transfer"
    }
}
