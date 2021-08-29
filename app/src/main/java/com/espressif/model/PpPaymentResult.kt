package com.espressif.model

data class PpPaymentResult(
    val transactionResult: String,
    val errorCode: String?,
    val issuer: String?,
    val installments: Int?,
    val approvedCode: String?,
    val rrn: String?,
    val maskedCardNo: String?
)