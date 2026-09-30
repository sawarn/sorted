package com.sorted.app.data

import com.sorted.app.engine.TransactionType

data class TransactionCorrection(
    val transactionId: Long,
    val merchantNormalized: String,
    val miscCategory: String,
    val departmentCategory: String,
    val transactionType: TransactionType,
    val rememberRule: Boolean,
    val amount: Double?,
    val transactionDate: String?,
    val note: String?
)
