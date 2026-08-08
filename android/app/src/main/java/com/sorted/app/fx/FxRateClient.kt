package com.sorted.app.fx

import com.sorted.app.data.FxRateEntity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FxRateClient {
    fun fetchRate(
        baseCurrency: String,
        quoteCurrency: String,
        requestedDate: String
    ): FxRateEntity {
        val base = baseCurrency.uppercase()
        val quote = quoteCurrency.uppercase()
        val url = "$ApiBase/v2/rate/$base/$quote?date=$requestedDate"
        val json = getJson(url)
        val rate = json.optDouble("rate", 0.0)
        if (rate <= 0.0) {
            throw IllegalStateException("Missing FX rate for $base/$quote on $requestedDate")
        }

        return FxRateEntity(
            requestedDate = requestedDate,
            baseCurrency = json.optString("base", base),
            quoteCurrency = json.optString("quote", quote),
            rateDate = json.optString("date", requestedDate),
            rate = rate,
            provider = Provider,
            fetchedAt = System.currentTimeMillis()
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (code !in 200..299) {
            throw IllegalStateException("FX API error $code: ${body.take(160)}")
        }

        return JSONObject(body)
    }

    private companion object {
        const val ApiBase = "https://api.frankfurter.dev"
        const val Provider = "Frankfurter"
    }
}

data class FxRefreshResult(
    val ratesUpdated: Int,
    val failures: Int
)

