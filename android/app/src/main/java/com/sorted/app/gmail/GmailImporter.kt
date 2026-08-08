package com.sorted.app.gmail

import android.content.Context
import com.sorted.app.data.ImportRecord
import com.sorted.app.data.ImportSource
import com.sorted.app.data.TransactionEntity
import com.sorted.app.data.TransactionRepository
import com.sorted.app.engine.Direction
import com.sorted.app.engine.ParsedTransaction
import kotlin.math.abs

data class GmailImportSummary(
    val messagesScanned: Int,
    val transactionsDetected: Int,
    val importedTransactions: Int,
    val skippedLowConfidence: Int,
    val skippedDuplicates: Int
)

class GmailImporter(context: Context) {
    private val appContext = context.applicationContext
    private val client = GmailImportClient()
    private val repository = TransactionRepository(appContext)

    fun importLatest(accessToken: String): GmailImportSummary {
        val rawMessages = client.fetchCandidateMessages(accessToken)
        val records = rawMessages.map { raw ->
            GmailScanRecord(
                raw = raw,
                sourceHash = "gmail:${raw.id}",
                parsed = GmailParser.parse(raw)
            )
        }
        GmailDebugFeedWriter.write(appContext, records)

        val existingTransactions = repository.listTransactions(limit = 5_000)
        val transactionRecords = records.filter { it.parsed.isTransaction }
        val highConfidenceRecords = transactionRecords
            .filter { it.parsed.confidence >= GmailImportPlan.MinImportConfidence }
        val importableRecords = highConfidenceRecords
            .filterNot { it.parsed.isLikelyDuplicateOf(it.sourceHash, existingTransactions) }

        repository.import(
            importableRecords.map { record ->
                ImportRecord(
                    source = ImportSource.GMAIL,
                    sourceHash = record.sourceHash,
                    sourceReceivedDate = record.raw.receivedDate,
                    parsed = record.parsed
                )
            }
        )

        return GmailImportSummary(
            messagesScanned = rawMessages.size,
            transactionsDetected = transactionRecords.size,
            importedTransactions = importableRecords.size,
            skippedLowConfidence = transactionRecords.size - highConfidenceRecords.size,
            skippedDuplicates = highConfidenceRecords.size - importableRecords.size
        )
    }

    private fun ParsedTransaction.isLikelyDuplicateOf(
        sourceHash: String,
        existingTransactions: List<TransactionEntity>
    ): Boolean {
        val parsedAmount = amount ?: return false
        val parsedDate = transactionDate ?: return false
        val parsedMerchant = merchantNormalized ?: merchantRaw ?: return false

        return existingTransactions.any { existing ->
            existing.sourceHash != sourceHash &&
                existing.transactionDate == parsedDate &&
                existing.direction == direction &&
                sameAmount(existing.amount, parsedAmount) &&
                sameMerchant(existing.merchantNormalized ?: existing.merchantRaw, parsedMerchant)
        }
    }

    private fun sameAmount(existingAmount: Double?, parsedAmount: Double): Boolean {
        return existingAmount != null && abs(existingAmount - parsedAmount) < 0.01
    }

    private fun sameMerchant(left: String?, right: String): Boolean {
        val leftKey = left.normalizedMerchantKey()
        val rightKey = right.normalizedMerchantKey()
        if (leftKey.length < 4 || rightKey.length < 4) return false
        return leftKey == rightKey || leftKey.contains(rightKey) || rightKey.contains(leftKey)
    }

    private fun String?.normalizedMerchantKey(): String {
        return orEmpty().uppercase().replace(Regex("""[^A-Z0-9]"""), "")
    }
}
