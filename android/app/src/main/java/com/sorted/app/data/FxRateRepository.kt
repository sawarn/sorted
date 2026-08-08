package com.sorted.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

data class FxRateKey(
    val requestedDate: String,
    val baseCurrency: String,
    val quoteCurrency: String
)

data class FxRateEntity(
    val requestedDate: String,
    val baseCurrency: String,
    val quoteCurrency: String,
    val rateDate: String,
    val rate: Double,
    val provider: String,
    val fetchedAt: Long
) {
    val key: FxRateKey
        get() = FxRateKey(requestedDate, baseCurrency, quoteCurrency)
}

class FxRateRepository(context: Context) {
    private val database = SortedDatabase(context)

    fun upsert(rate: FxRateEntity) {
        val db = database.writableDatabase
        db.insertWithOnConflict(
            "fx_rates",
            null,
            rate.toValues(),
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun find(requestedDate: String, baseCurrency: String, quoteCurrency: String): FxRateEntity? {
        val db = database.readableDatabase
        db.query(
            "fx_rates",
            null,
            "requested_date = ? AND base_currency = ? AND quote_currency = ?",
            arrayOf(requestedDate, baseCurrency.uppercase(), quoteCurrency.uppercase()),
            null,
            null,
            "fetched_at DESC",
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toFxRateEntity() else null
        }
    }

    fun listRates(): List<FxRateEntity> {
        val db = database.readableDatabase
        val rows = mutableListOf<FxRateEntity>()
        db.query(
            "fx_rates",
            null,
            null,
            null,
            null,
            null,
            "requested_date DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rows.add(cursor.toFxRateEntity())
            }
        }
        return rows
    }

    private fun FxRateEntity.toValues(): ContentValues {
        return ContentValues().apply {
            put("requested_date", requestedDate)
            put("base_currency", baseCurrency.uppercase())
            put("quote_currency", quoteCurrency.uppercase())
            put("rate_date", rateDate)
            put("rate", rate)
            put("provider", provider)
            put("fetched_at", fetchedAt)
        }
    }

    private fun Cursor.toFxRateEntity(): FxRateEntity {
        return FxRateEntity(
            requestedDate = string("requested_date").orEmpty(),
            baseCurrency = string("base_currency").orEmpty(),
            quoteCurrency = string("quote_currency").orEmpty(),
            rateDate = string("rate_date").orEmpty(),
            rate = double("rate"),
            provider = string("provider").orEmpty(),
            fetchedAt = long("fetched_at")
        )
    }

    private fun Cursor.string(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.double(column: String): Double {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getDouble(index) else 0.0
    }

    private fun Cursor.long(column: String): Long {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index) else 0L
    }
}
