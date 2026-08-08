package com.sorted.app.fx

import android.content.Context
import com.sorted.app.data.FxRateRepository
import com.sorted.app.engine.Direction
import com.sorted.app.engine.ParsedTransaction

class FxRateImporter(context: Context) {
    private val repository = FxRateRepository(context.applicationContext)
    private val client = FxRateClient()

    fun refreshRatesFor(transactions: List<ParsedTransaction>): FxRefreshResult {
        val requests = transactions
            .asSequence()
            .filter { it.isTransaction }
            .filter { it.direction == Direction.DEBIT }
            .mapNotNull { transaction ->
                val currency = transaction.currency?.uppercase().orEmpty()
                val date = transaction.transactionDate
                if (currency.isBlank() || currency == "INR" || date.isNullOrBlank()) {
                    null
                } else {
                    FxRequest(baseCurrency = currency, quoteCurrency = "INR", requestedDate = date)
                }
            }
            .distinct()
            .toList()

        var updated = 0
        var failures = 0
        requests.forEach { request ->
            val cached = repository.find(request.requestedDate, request.baseCurrency, request.quoteCurrency)
            if (cached != null) return@forEach

            runCatching {
                client.fetchRate(request.baseCurrency, request.quoteCurrency, request.requestedDate)
            }.onSuccess { rate ->
                repository.upsert(rate)
                updated += 1
            }.onFailure {
                failures += 1
            }
        }

        return FxRefreshResult(ratesUpdated = updated, failures = failures)
    }

    private data class FxRequest(
        val baseCurrency: String,
        val quoteCurrency: String,
        val requestedDate: String
    )
}
