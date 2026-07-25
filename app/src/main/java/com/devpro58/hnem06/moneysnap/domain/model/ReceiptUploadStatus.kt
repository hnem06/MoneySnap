package com.devpro58.hnem06.moneysnap.domain.model

enum class ReceiptUploadStatus {
    None,
    LocalOnly,
    Queued,
    Uploading,
    Uploaded,
    Failed
}
