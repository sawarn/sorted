package com.sorted.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
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
            val ignoredHashes = ignoredSourceHashes(db)
            val rules = categoryRules(db)
            val corrections = userCorrections(db)
            records
                .forEach { record ->
                    if (record.sourceHash in ignoredHashes) {
                        db.delete(
                            "transactions",
                            "source_hash = ?",
                            arrayOf(record.sourceHash)
                        )
                        return@forEach
                    }

                    val parsed = record.parsed
                        .applyCategoryRules(rules)
                        .applyUserCorrection(corrections[record.sourceHash])
                    if (parsed.isTransaction) {
                        db.insertWithOnConflict(
                            "transactions",
                            null,
                            record.toValues(now, parsed),
                            SQLiteDatabase.CONFLICT_REPLACE
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
            val ignoredHashes = ignoredSourceHashes(db)
            val rules = categoryRules(db)
            val corrections = userCorrections(db)
            db.delete(
                "transactions",
                "source = ?",
                arrayOf(source.value)
            )
            records
                .filter { it.parsed.isTransaction && it.sourceHash !in ignoredHashes }
                .forEach { record ->
                    val parsed = record.parsed
                        .applyCategoryRules(rules)
                        .applyUserCorrection(corrections[record.sourceHash])
                    db.insertWithOnConflict(
                        "transactions",
                        null,
                        record.toValues(now, parsed),
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateTransaction(correction: TransactionCorrection): Boolean {
        val now = System.currentTimeMillis()
        val db = database.writableDatabase
        val existing = findTransaction(correction.transactionId) ?: return false
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("merchant_normalized", correction.merchantNormalized)
                put("misc_category", correction.miscCategory)
                put("department_category", correction.departmentCategory)
                put("transaction_type", correction.transactionType.name)
                put("category_source", CategorySource.USER_RULE.name)
                put("confidence", maxOf(existing.confidence, 0.98))
                put("updated_at", now)
            }
            val updated = db.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(correction.transactionId.toString())
            )
            if (updated == 0) {
                return false
            }

            val ruleId = if (correction.rememberRule) {
                upsertCategoryRule(
                    db = db,
                    existing = existing,
                    correction = correction,
                    now = now
                )
            } else {
                null
            }
            insertCorrectionAudit(
                db = db,
                existing = existing,
                correction = correction,
                ruleId = ruleId,
                now = now
            )
            db.setTransactionSuccessful()
            return true
        } finally {
            if (db.inTransaction()) {
                db.endTransaction()
            }
        }
    }

    fun ignoreTransaction(sourceHash: String): Boolean {
        if (sourceHash.isBlank()) return false
        val now = System.currentTimeMillis()
        val db = database.writableDatabase
        val existing = findTransactionBySourceHash(sourceHash)
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("source_hash", sourceHash)
                put("source", existing?.source?.value ?: "unknown")
                put("merchant_normalized", existing?.merchantNormalized)
                put("amount", existing?.amount)
                put("currency", existing?.currency)
                put("ignored_at", now)
            }
            db.insertWithOnConflict(
                "ignored_transactions",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            val deleted = db.delete(
                "transactions",
                "source_hash = ?",
                arrayOf(sourceHash)
            )
            db.setTransactionSuccessful()
            return deleted > 0
        } finally {
            db.endTransaction()
        }
    }

    fun listTransactions(limit: Int? = null): List<TransactionEntity> {
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
            limit?.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rows.add(cursor.toTransactionEntity())
            }
        }
        return rows
    }

    fun listCategoryRules(limit: Int? = null): List<CategoryRuleEntity> {
        return categoryRules(database.readableDatabase, limit)
    }

    private fun findTransaction(id: Long): TransactionEntity? {
        return querySingleTransaction("id = ?", arrayOf(id.toString()))
    }

    private fun findTransactionBySourceHash(sourceHash: String): TransactionEntity? {
        return querySingleTransaction("source_hash = ?", arrayOf(sourceHash))
    }

    private fun querySingleTransaction(selection: String, args: Array<String>): TransactionEntity? {
        val db = database.readableDatabase
        db.query(
            "transactions",
            null,
            selection,
            args,
            null,
            null,
            "id DESC",
            "1"
        ).use { cursor ->
            return if (cursor.moveToNext()) cursor.toTransactionEntity() else null
        }
    }

    private fun ImportRecord.toValues(now: Long, parsed: ParsedTransaction = this.parsed): ContentValues {
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

    private fun upsertCategoryRule(
        db: SQLiteDatabase,
        existing: TransactionEntity,
        correction: TransactionCorrection,
        now: Long
    ): Long? {
        val pattern = existing.rulePattern() ?: return null
        val existingRule = db.query(
            "category_rules",
            arrayOf("id", "created_at"),
            "pattern = ? AND match_type = ? AND source = ?",
            arrayOf(pattern, RuleMatchExact, RuleSourceUser),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.long("id") to cursor.long("created_at")
            } else {
                null
            }
        }

        val values = ContentValues().apply {
            put("pattern", pattern)
            put("match_type", RuleMatchExact)
            put("merchant_normalized", correction.merchantNormalized)
            put("misc_category", correction.miscCategory)
            put("department_category", correction.departmentCategory)
            put("transaction_type", correction.transactionType.name)
            put("priority", UserRulePriority)
            put("source", RuleSourceUser)
            put("enabled", 1)
            put("created_at", existingRule?.second ?: now)
            put("updated_at", now)
        }

        return if (existingRule != null) {
            db.update(
                "category_rules",
                values,
                "id = ?",
                arrayOf(existingRule.first.toString())
            )
            existingRule.first
        } else {
            db.insert("category_rules", null, values).takeIf { it > 0 }
        }
    }

    private fun insertCorrectionAudit(
        db: SQLiteDatabase,
        existing: TransactionEntity,
        correction: TransactionCorrection,
        ruleId: Long?,
        now: Long
    ) {
        val values = ContentValues().apply {
            put("transaction_id", existing.id)
            put("source_hash", existing.sourceHash)
            put("old_merchant_normalized", existing.merchantNormalized)
            put("new_merchant_normalized", correction.merchantNormalized)
            put("old_misc_category", existing.miscCategory)
            put("new_misc_category", correction.miscCategory)
            put("old_department_category", existing.departmentCategory)
            put("new_department_category", correction.departmentCategory)
            put("old_transaction_type", existing.transactionType.name)
            put("new_transaction_type", correction.transactionType.name)
            put("created_rule_id", ruleId)
            put("created_at", now)
        }
        db.insert("user_corrections", null, values)
    }

    private fun ParsedTransaction.applyCategoryRules(rules: List<CategoryRuleEntity>): ParsedTransaction {
        if (!isTransaction || rules.isEmpty()) return this
        val merchantKeys = listOfNotNull(merchantNormalized, merchantRaw)
            .mapNotNull { it.ruleKey().takeIf(String::isNotBlank) }
            .distinct()
        if (merchantKeys.isEmpty()) return this

        val rule = rules
            .asSequence()
            .filter { it.enabled }
            .filter { rule ->
                merchantKeys.any { key ->
                    when (rule.matchType) {
                        RuleMatchExact -> key == rule.pattern
                        RuleMatchContains -> key.contains(rule.pattern)
                        else -> false
                    }
                }
            }
            .sortedWith(
                compareByDescending<CategoryRuleEntity> { if (it.matchType == RuleMatchExact) 1 else 0 }
                    .thenByDescending { it.priority }
                    .thenByDescending { it.updatedAt }
            )
            .firstOrNull()
            ?: return this

        return copy(
            merchantNormalized = rule.merchantNormalized ?: merchantNormalized,
            miscCategory = rule.miscCategory ?: miscCategory,
            departmentCategory = rule.departmentCategory ?: departmentCategory,
            transactionType = rule.transactionType,
            categorySource = CategorySource.USER_RULE,
            confidence = maxOf(confidence, 0.98)
        )
    }

    private fun ParsedTransaction.applyUserCorrection(correction: StoredCorrection?): ParsedTransaction {
        if (!isTransaction || correction == null) return this
        return copy(
            merchantNormalized = correction.merchantNormalized,
            miscCategory = correction.miscCategory,
            departmentCategory = correction.departmentCategory,
            transactionType = correction.transactionType,
            categorySource = CategorySource.USER_RULE,
            confidence = maxOf(confidence, 0.99)
        )
    }

    private fun ignoredSourceHashes(db: SQLiteDatabase): Set<String> {
        return buildSet {
            db.query(
                "ignored_transactions",
                arrayOf("source_hash"),
                null,
                null,
                null,
                null,
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    cursor.string("source_hash")?.let(::add)
                }
            }
        }
    }

    private fun categoryRules(db: SQLiteDatabase, limit: Int? = null): List<CategoryRuleEntity> {
        val rows = mutableListOf<CategoryRuleEntity>()
        db.query(
            "category_rules",
            null,
            "enabled = ?",
            arrayOf("1"),
            null,
            null,
            "priority DESC, updated_at DESC",
            limit?.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rows.add(cursor.toCategoryRuleEntity())
            }
        }
        return rows
    }

    private fun userCorrections(db: SQLiteDatabase): Map<String, StoredCorrection> {
        val rows = linkedMapOf<String, StoredCorrection>()
        db.query(
            "user_corrections",
            arrayOf(
                "source_hash",
                "new_merchant_normalized",
                "new_misc_category",
                "new_department_category",
                "new_transaction_type"
            ),
            null,
            null,
            null,
            null,
            "created_at ASC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val sourceHash = cursor.string("source_hash") ?: continue
                rows[sourceHash] = StoredCorrection(
                    merchantNormalized = cursor.string("new_merchant_normalized").orEmpty(),
                    miscCategory = cursor.string("new_misc_category").orEmpty(),
                    departmentCategory = cursor.string("new_department_category").orEmpty(),
                    transactionType = enumValueOrDefault(
                        cursor.string("new_transaction_type"),
                        TransactionType.UNKNOWN
                    )
                )
            }
        }
        return rows
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

    private fun Cursor.toCategoryRuleEntity(): CategoryRuleEntity {
        return CategoryRuleEntity(
            id = long("id"),
            pattern = string("pattern").orEmpty(),
            matchType = string("match_type").orEmpty(),
            merchantNormalized = string("merchant_normalized"),
            miscCategory = string("misc_category"),
            departmentCategory = string("department_category"),
            transactionType = enumValueOrDefault(string("transaction_type"), TransactionType.UNKNOWN),
            priority = int("priority"),
            source = string("source").orEmpty(),
            enabled = int("enabled") == 1,
            createdAt = long("created_at"),
            updatedAt = long("updated_at")
        )
    }

    private fun TransactionEntity.rulePattern(): String? {
        return listOfNotNull(merchantNormalized, merchantRaw)
            .firstNotNullOfOrNull { it.ruleKey().takeIf(String::isNotBlank) }
    }

    private fun String.ruleKey(): String {
        return uppercase()
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun Cursor.string(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.int(column: String): Int {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getInt(index) else 0
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

    private companion object {
        const val RuleMatchExact = "exact"
        const val RuleMatchContains = "contains"
        const val RuleSourceUser = "user"
        const val UserRulePriority = 100
    }
}

private data class StoredCorrection(
    val merchantNormalized: String,
    val miscCategory: String,
    val departmentCategory: String,
    val transactionType: TransactionType
)

fun String.stableHash(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
}
