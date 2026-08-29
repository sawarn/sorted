package com.sorted.app.data

import com.sorted.app.engine.TransactionType

data class CategoryRuleEntity(
    val id: Long,
    val pattern: String,
    val matchType: String,
    val merchantNormalized: String?,
    val miscCategory: String?,
    val departmentCategory: String?,
    val transactionType: TransactionType,
    val priority: Int,
    val source: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

