package com.devpro58.hnem06.moneysnap.domain.model

enum class ExpenseCategory(
    val label: String
) {
    Food("Food"),
    Transport("Transport"),
    Shopping("Shopping"),
    Entertainment("Entertainment"),
    Bills("Bills"),
    Travel("Travel"),
    Uncategorized("Uncategorized"),
    Other("Other")
}
