package com.sorted.app.gmail

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

object GmailDebugFeedWriter {
    private const val FileName = "sorted-gmail-debug-feed.json"

    fun write(context: Context, records: List<GmailScanRecord>) {
        val payload = JSONObject()
            .put("messagesScanned", records.size)
            .put("transactionsDetected", records.count { it.parsed.isTransaction })
            .put("records", JSONArray(records.map { it.toJson() }))

        File(context.cacheDir, FileName).writeText(payload.toString(2))
    }

    private fun GmailScanRecord.toJson(): JSONObject {
        val visibleText = listOfNotNull(raw.subject, raw.snippet, raw.bodyText)
            .joinToString(" ")
            .sanitizedForDebug()

        return JSONObject()
            .put("messageIdHash", raw.id.sha256Short())
            .put("receivedDate", raw.receivedDate)
            .put("from", raw.from.sanitizedForDebug())
            .put("subject", raw.subject.sanitizedForDebug())
            .put("bodyHash", visibleText.sha256Short())
            .put("sanitizedText", visibleText.take(700))
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

    private fun String?.sanitizedForDebug(): String {
        return orEmpty()
            .replace(Regex("""(?i)\b\d{4,8}\b(?=\s+is\s+(?:an?\s+)?one-time password|\s+is\s+otp)"""), "<otp>")
            .replace(Regex("""(?i)\b(?:ref|reference|upi|umrn|transaction id|order id|invoice)[:\s-]*[A-Z0-9]{6,}\b"""), "<reference>")
            .replace(Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+"""), "<email>")
            .replace(Regex("""\b\d{10,}\b"""), "<number>")
            .replace(Regex("""(?i)\b(?:a/c|account|card)(?:\s+no)?\s*(?:x+|\*+)?\d{4,}\b"""), "<account>")
            .replace(Regex("""[ \t\r\n]+"""), " ")
            .trim()
    }

    private fun String.sha256Short(): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }
        return digest.take(16)
    }
}

data class GmailScanRecord(
    val raw: GmailRawMessage,
    val sourceHash: String,
    val parsed: ParsedTransaction
)
