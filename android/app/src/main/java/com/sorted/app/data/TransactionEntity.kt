package com.sorted.app.data

import com.sorted.app.engine.CategorySource
import com.sorted.app.engine.Direction
import com.sorted.app.engine.PaymentMode
import com.sorted.app.engine.TransactionStatus
import com.sorted.app.engine.TransactionType

data class TransactionEntity(
    val id: Long,
    val source: ImportSource,
    val sourceHash: String,
    val sourceReceivedDate: String?,
    val amount: Double?,
    val currency: String?,
    val direction: Direction,
    val merchantRaw: String?,
    val merchantNormalized: String?,
    val miscCategory: String?,
    val departmentCategory: String?,
    val paymentMode: PaymentMode,
    val accountHint: String?,
    val transactionDate: String?,
    val transactionTime: String?,
    val transactionType: TransactionType,
    val status: TransactionStatus,
    val categorySource: CategorySource,
    val confidence: Double,
    val createdAt: Long,
    val updatedAt: Long
)

