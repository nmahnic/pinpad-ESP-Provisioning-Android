package com.espressif.model

data class PpPayment(
    val currency: String,
    val currencyCode: Int,
    val transactionType: String,
    val amount: Double
)