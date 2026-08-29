package com.sorted.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SortedDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DatabaseName,
    null,
    DatabaseVersion
) {
    override fun onCreate(db: SQLiteDatabase) {
        createTransactionsTable(db)
        createFxRatesTable(db)
        createCategoryRulesTable(db)
        createUserCorrectionsTable(db)
        createIgnoredTransactionsTable(db)
    }

    private fun createTransactionsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source TEXT NOT NULL,
                source_hash TEXT NOT NULL UNIQUE,
                source_received_date TEXT,
                amount REAL,
                currency TEXT,
                direction TEXT NOT NULL,
                merchant_raw TEXT,
                merchant_normalized TEXT,
                misc_category TEXT,
                department_category TEXT,
                payment_mode TEXT NOT NULL,
                account_hint TEXT,
                transaction_date TEXT,
                transaction_time TEXT,
                transaction_type TEXT NOT NULL,
                status TEXT NOT NULL,
                category_source TEXT NOT NULL,
                confidence REAL NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_transactions_date ON transactions(transaction_date)")
        db.execSQL("CREATE INDEX idx_transactions_merchant ON transactions(merchant_normalized)")
        db.execSQL("CREATE INDEX idx_transactions_category ON transactions(department_category)")
        db.execSQL("CREATE INDEX idx_transactions_source ON transactions(source)")
    }

    private fun createFxRatesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS fx_rates (
                requested_date TEXT NOT NULL,
                base_currency TEXT NOT NULL,
                quote_currency TEXT NOT NULL,
                rate_date TEXT NOT NULL,
                rate REAL NOT NULL,
                provider TEXT NOT NULL,
                fetched_at INTEGER NOT NULL,
                PRIMARY KEY (requested_date, base_currency, quote_currency, provider)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_fx_rates_lookup ON fx_rates(requested_date, base_currency, quote_currency)")
    }

    private fun createCategoryRulesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS category_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pattern TEXT NOT NULL,
                match_type TEXT NOT NULL,
                merchant_normalized TEXT,
                misc_category TEXT,
                department_category TEXT,
                transaction_type TEXT NOT NULL,
                priority INTEGER NOT NULL,
                source TEXT NOT NULL,
                enabled INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                UNIQUE(pattern, match_type, source)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_category_rules_enabled ON category_rules(enabled, priority, updated_at)")
    }

    private fun createUserCorrectionsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_corrections (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                transaction_id INTEGER NOT NULL,
                source_hash TEXT NOT NULL,
                old_merchant_normalized TEXT,
                new_merchant_normalized TEXT,
                old_misc_category TEXT,
                new_misc_category TEXT,
                old_department_category TEXT,
                new_department_category TEXT,
                old_transaction_type TEXT,
                new_transaction_type TEXT,
                created_rule_id INTEGER,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_corrections_source_hash ON user_corrections(source_hash)")
    }

    private fun createIgnoredTransactionsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ignored_transactions (
                source_hash TEXT PRIMARY KEY,
                source TEXT NOT NULL,
                merchant_normalized TEXT,
                amount REAL,
                currency TEXT,
                ignored_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ignored_transactions_source ON ignored_transactions(source)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS transactions")
            db.execSQL("DROP TABLE IF EXISTS fx_rates")
            onCreate(db)
            return
        }

        if (oldVersion < 3) {
            createFxRatesTable(db)
        }

        if (oldVersion < 4) {
            createCategoryRulesTable(db)
            createUserCorrectionsTable(db)
            createIgnoredTransactionsTable(db)
        }
    }

    private companion object {
        const val DatabaseName = "sorted.db"
        const val DatabaseVersion = 4
    }
}
