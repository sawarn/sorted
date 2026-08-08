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
    }

    private companion object {
        const val DatabaseName = "sorted.db"
        const val DatabaseVersion = 3
    }
}
