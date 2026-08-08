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

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS transactions")
        onCreate(db)
    }

    private companion object {
        const val DatabaseName = "sorted.db"
        const val DatabaseVersion = 2
    }
}
