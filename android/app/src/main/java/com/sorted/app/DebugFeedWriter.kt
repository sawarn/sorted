package com.sorted.app

import android.content.Context
import com.sorted.app.engine.Direction
import com.sorted.app.engine.ParsedTransaction
import com.sorted.app.engine.PaymentMode
import com.sorted.app.engine.TransactionStatus
import com.sorted.app.engine.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

object DebugFeedWriter {
    private const val FileName = "sorted-debug-feed.json"

    fun write(context: Context, records: List<SmsScanRecord>) {
        val parsed = records.map { it.parsed }
        val exportedRecords = records
            .filter { it.parsed.isTransaction || it.body.looksFinancial() }
            .filterNot { it.parsed.ignoreReason == "otp" }
            .map { it.toJson() }

        val payload = JSONObject()
            .put("totalMessagesParsed", parsed.size)
            .put("transactionsDetected", parsed.count { it.isTransaction })
            .put("ignoredMessages", parsed.count { !it.isTransaction })
            .put("exportedRecords", exportedRecords.size)
            .put("records", JSONArray(exportedRecords))

        File(context.cacheDir, FileName).writeText(payload.toString(2))
    }

    private fun SmsScanRecord.toJson(): JSONObject {
        return JSONObject()
            .put("smsId", smsId)
            .put("sourceAddress", sourceAddress.orEmpty().sanitizedForDebug())
            .put("receivedDate", receivedDate)
            .put("sourceHash", sourceHash)
            .put("bodyHash", body.sha256Short())
            .put("sanitizedBody", body.sanitizedForDebug())
            .put("parsed", parsed.toJson())
    }

    private fun ParsedTransaction.toJson(): JSONObject {
        return JSONObject()
            .put("isTransaction", isTransaction)
            .put("status", status.value())
            .put("amount", amount)
            .put("currency", currency)
            .put("direction", direction.value())
            .put("merchantRaw", merchantRaw)
            .put("merchantNormalized", merchantNormalized)
            .put("miscCategory", miscCategory)
            .put("departmentCategory", departmentCategory)
            .put("paymentMode", paymentMode.value())
            .put("accountHint", accountHint)
            .put("transactionDate", transactionDate)
            .put("transactionTime", transactionTime)
            .put("transactionType", transactionType.value())
            .put("ignoreReason", ignoreReason)
    }

    private fun TransactionStatus.value(): String = name.lowercase()
    private fun Direction.value(): String = name.lowercase()
    private fun PaymentMode.value(): String = name.lowercase()
    private fun TransactionType.value(): String = name.lowercase()

    private fun String.looksFinancial(): Boolean {
        val lower = lowercase()
        val markers = listOf(
            "inr",
            "rs.",
            "rs ",
            "debited",
            "credited",
            "spent",
            "sent",
            "deducted",
            "payment alert",
            "upi",
            "card",
            "refund",
            "mandate",
            "autopay",
            "a/c",
            "account"
        )
        return markers.any { it in lower }
    }

    private fun String.sanitizedForDebug(): String {
        return this
            .replace(Regex("""(?i)\b\d{4,8}\b(?=\s+is\s+(?:an?\s+)?one-time password|\s+is\s+otp)"""), "<otp>")
            .replace(Regex("""(?i)\b(?:ref|reference|upi|umrn|si hub id)[:\s-]*[A-Z0-9]{6,}\b"""), "<reference>")
            .replace(Regex("""(?i)\bPAN\s+[A-ZX]{3,}\d+[A-Z]\b"""), "PAN <masked>")
            .replace(Regex("""(?i)\bUAN\s+\d{6,}\b"""), "UAN <masked>")
            .replace(Regex("""(?i)\b(?:a/c|account|card)(?:\s+no)?\s*(?:x+|\*+)?\d{5,}\b"""), "<account>")
            .replace(Regex("""\b\d{10,}\b"""), "<number>")
            .replace(Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+"""), "<vpa>")
            .replace(Regex("""(?i)(call|sms|to)\s+\d{5,}"""), "$1 <number>")
            .replace(Regex("""[ \t]+"""), " ")
            .trim()
    }

    private fun String.sha256Short(): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }
        return digest.take(16)
    }
}

data class SmsScanRecord(
    val smsId: Long?,
    val sourceAddress: String?,
    val body: String,
    val receivedDate: String?,
    val sourceHash: String,
    val parsed: ParsedTransaction
)
