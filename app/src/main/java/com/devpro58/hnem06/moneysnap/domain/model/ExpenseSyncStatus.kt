package com.devpro58.hnem06.moneysnap.domain.model

enum class ExpenseSyncStatus {
    LocalOnly,
    PendingUpload,
    Synced,
    Failed,
    PendingDelete
}
