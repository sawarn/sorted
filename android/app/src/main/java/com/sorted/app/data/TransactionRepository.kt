package com.sorted.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.sorted.app.engine.CategorySource
import com.sorted.app.engine.Direction
import com.sorted.app.engine.ParsedTransaction
import com.sorted.app.engine.PaymentMode
import com.sorted.app.engine.TransactionStatus
import com.sorted.app.engine.TransactionType
import java.security.MessageDigest

class TransactionRepository(context: Context) {
    private val database = SortedDatabase(context)

    fun import(records: List<ImportRecord>) {
        val now = System.currentTimeMillis()
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            records
                .forEach { record ->
                    if (record.parsed.isTransaction) {
                        db.insertWithOnConflict(
                            "transactions",
                            null,
                            record.toValues(now),
                            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                        )
                    } else {
                        db.delete(
                            "transactions",
                            "source_hash = ?",
                            arrayOf(record.sourceHash)
                        )
                    }
                }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun replaceSource(source: ImportSource, records: List<ImportRecord>) {
        val now = System.currentTimeMillis()
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                "transactions",
                "source = ?",
                arrayOf(source.value)
            )
            records
                .filter { it.parsed.isTransaction }
                .forEach { record ->
                    db.insertWithOnConflict(
                        "transactions",
                        null,
                        record.toValues(now),
                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun listTransactions(limit: Int = 1000): List<TransactionEntity> {
        val db = database.readableDatabase
        val rows = mutableListOf<TransactionEntity>()
        db.query(
            "transactions",
            null,
            null,
            null,
            null,
            null,
            "transaction_date DESC, id DESC",
            limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rows.add(cursor.toTransactionEntity())
            }
        }
        return rows
    }

    private fun ImportRecord.toValues(now: Long): ContentValues {
        val parsed = parsed
        return ContentValues().apply {
            put("source", source.value)
            put("source_hash", sourceHash)
            put("source_received_date", sourceReceivedDate)
            put("amount", parsed.amount)
            put("currency", parsed.currency)
            put("direction", parsed.direction.name)
            put("merchant_raw", parsed.merchantRaw)
            put("merchant_normalized", parsed.merchantNormalized)
            put("misc_category", parsed.miscCategory)
            put("department_category", parsed.departmentCategory)
            put("payment_mode", parsed.paymentMode.name)
            put("account_hint", parsed.accountHint)
            put("transaction_date", parsed.transactionDate)
            put("transaction_time", parsed.transactionTime)
            put("transaction_type", parsed.transactionType.name)
            put("status", parsed.status.name)
            put("category_source", parsed.categorySource.name)
            put("confidence", parsed.confidence)
            put("created_at", now)
            put("updated_at", now)
        }
    }

    private fun Cursor.toTransactionEntity(): TransactionEntity {
        return TransactionEntity(
            id = long("id"),
            source = ImportSource.entries.firstOrNull { it.value == string("source") } ?: ImportSource.SMS,
            sourceHash = string("source_hash").orEmpty(),
            sourceReceivedDate = string("source_received_date"),
            amount = doubleOrNull("amount"),
            currency = string("currency"),
            direction = enumValueOrDefault(string("direction"), Direction.UNKNOWN),
            merchantRaw = string("merchant_raw"),
            merchantNormalized = string("merchant_normalized"),
            miscCategory = string("misc_category"),
            departmentCategory = string("department_category"),
            paymentMode = enumValueOrDefault(string("payment_mode"), PaymentMode.UNKNOWN),
            accountHint = string("account_hint"),
            transactionDate = string("transaction_date"),
            transactionTime = string("transaction_time"),
            transactionType = enumValueOrDefault(string("transaction_type"), TransactionType.UNKNOWN),
            status = enumValueOrDefault(string("status"), TransactionStatus.UNKNOWN),
            categorySource = enumValueOrDefault(string("category_source"), CategorySource.NONE),
            confidence = doubleOrNull("confidence") ?: 0.0,
            createdAt = long("created_at"),
            updatedAt = long("updated_at")
        )
    }

    private fun Cursor.string(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.long(column: String): Long {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index) else 0L
    }

    private fun Cursor.doubleOrNull(column: String): Double? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getDouble(index) else null
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T {
        return value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
    }
}

fun String.stableHash(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
}
