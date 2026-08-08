package com.sorted.app.gmail

import android.content.Context
import com.sorted.app.data.ImportRecord
import com.sorted.app.data.ImportSource
import com.sorted.app.data.TransactionEntity
import com.sorted.app.data.TransactionRepository
import com.sorted.app.engine.ParsedTransaction
import com.sorted.app.engine.TransactionType
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
            .filterNot { it.isLikelyDuplicateOf(existingTransactions) }
            .dedupeWithinBatch()

        repository.replaceSource(
            ImportSource.GMAIL,
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

    private fun List<GmailScanRecord>.dedupeWithinBatch(): List<GmailScanRecord> {
        val kept = mutableListOf<GmailScanRecord>()
        sortedWith(
            compareByDescending<GmailScanRecord> { it.importPriority() }
                .thenByDescending { it.parsed.confidence }
        ).forEach { record ->
            if (kept.none { keptRecord -> record.isLikelyDuplicateOf(keptRecord) }) {
                kept.add(record)
            }
        }
        return kept
    }

    private fun GmailScanRecord.isLikelyDuplicateOf(
        existingTransactions: List<TransactionEntity>
    ): Boolean {
        val parsedTransaction = parsed
        val parsedAmount = parsedTransaction.amount ?: return false
        val parsedDate = parsedTransaction.transactionDate ?: return false
        val parsedMerchant = parsedTransaction.merchantNormalized ?: parsedTransaction.merchantRaw ?: return false
        val gmailLooksAuthoritative = raw.looksLikeAuthoritativeFinancialAlert()

        return existingTransactions.any { existing ->
            existing.sourceHash != sourceHash &&
                existing.transactionDate == parsedDate &&
                existing.direction == parsedTransaction.direction &&
                sameCurrency(existing.currency, parsedTransaction.currency) &&
                sameAmount(existing.amount, parsedAmount) &&
                (
                    sameMerchant(existing.merchantNormalized ?: existing.merchantRaw, parsedMerchant) ||
                        (gmailLooksAuthoritative && existing.source == ImportSource.SMS) ||
                        sameTransactionFamily(existing.transactionType, parsedTransaction.transactionType)
                )
        }
    }

    private fun GmailScanRecord.isLikelyDuplicateOf(existing: GmailScanRecord): Boolean {
        val parsedTransaction = parsed
        val parsedAmount = parsedTransaction.amount ?: return false
        val parsedDate = parsedTransaction.transactionDate ?: return false
        val parsedMerchant = parsedTransaction.merchantNormalized ?: parsedTransaction.merchantRaw ?: return false
        val existingParsed = existing.parsed
        val existingAmount = existingParsed.amount ?: return false
        val existingMerchant = existingParsed.merchantNormalized ?: existingParsed.merchantRaw ?: return false

        return existingParsed.transactionDate == parsedDate &&
            existingParsed.direction == parsedTransaction.direction &&
            sameCurrency(existingParsed.currency, parsedTransaction.currency) &&
            sameAmount(existingAmount, parsedAmount) &&
            (
                sameMerchant(existingMerchant, parsedMerchant) ||
                    sameTransactionFamily(existingParsed.transactionType, parsedTransaction.transactionType)
            )
    }

    private fun GmailScanRecord.importPriority(): Int {
        val sourceText = "${raw.from.orEmpty()} ${raw.subject.orEmpty()}".uppercase()
        return when {
            "VESTED FINANCE" in sourceText -> 100
            raw.looksLikeAuthoritativeFinancialAlert() -> 90
            parsed.categorySource.name == "KNOWN_MERCHANT_RULE" -> 80
            else -> (parsed.confidence * 100).toInt()
        }
    }

    private fun sameAmount(existingAmount: Double?, parsedAmount: Double): Boolean {
        return existingAmount != null && abs(existingAmount - parsedAmount) < 0.01
    }

    private fun sameCurrency(existingCurrency: String?, parsedCurrency: String?): Boolean {
        return existingCurrency.normalizedCurrency() == parsedCurrency.normalizedCurrency()
    }

    private fun sameMerchant(left: String?, right: String): Boolean {
        val leftKey = left.normalizedMerchantKey()
        val rightKey = right.normalizedMerchantKey()
        if (leftKey.length < 4 || rightKey.length < 4) return false
        return leftKey == rightKey || leftKey.contains(rightKey) || rightKey.contains(leftKey)
    }

    private fun sameTransactionFamily(left: TransactionType, right: TransactionType): Boolean {
        return left == right && left in setOf(
            TransactionType.INVESTMENT,
            TransactionType.SUBSCRIPTION,
            TransactionType.TRANSFER,
            TransactionType.REFUND,
            TransactionType.REWARD
        )
    }

    private fun GmailRawMessage.looksLikeAuthoritativeFinancialAlert(): Boolean {
        val fromKey = from.normalizedMerchantKey()
        val subjectKey = subject.normalizedMerchantKey()
        return listOf("HDFC", "ICICI", "SBIBANK", "PNB", "AXIS", "KOTAK", "CANARA", "YESBANK", "NACHAUTOEMAILER")
            .any { it in fromKey || it in subjectKey }
    }

    private fun String?.normalizedMerchantKey(): String {
        return orEmpty().uppercase().replace(Regex("""[^A-Z0-9]"""), "")
    }

    private fun String?.normalizedCurrency(): String {
        return orEmpty().ifBlank { "INR" }.uppercase()
    }
}
