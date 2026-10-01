package com.sorted.app.engine

data class SpendAnalyticsEntry(
    val sourceHash: String,
    val transactionDate: String?,
    val merchant: String?,
    val category: String?,
    val amountInr: Double?,
    val status: TransactionStatus,
    val direction: Direction,
    val transactionType: TransactionType
)

data class SpendAnalyticsGroup(
    val label: String,
    val count: Int,
    val total: Double,
    val sourceHashes: List<String>,
    val category: String?
)

data class RecurringSpendCandidate(
    val merchant: String,
    val expectedAmount: Double,
    val count: Int,
    val distinctMonths: Int,
    val lastSeenDate: String,
    val confidence: RecurringConfidence,
    val sourceHashes: List<String>
)

enum class RecurringConfidence { LIKELY, POSSIBLE }

/** One calculation path for gross outgoing summaries and their supporting transactions. */
object SpendAnalytics {
    fun eligibleEntries(
        entries: List<SpendAnalyticsEntry>,
        monthKeys: Set<String>? = null
    ): List<SpendAnalyticsEntry> = entries.filter { entry ->
        OutflowPolicy.countsTowardSpent(
            status = entry.status,
            direction = entry.direction,
            amount = entry.amountInr,
            type = entry.transactionType
        ) && (monthKeys == null || entry.transactionDate?.take(7) in monthKeys)
    }

    fun total(entries: List<SpendAnalyticsEntry>): Double =
        entries.sumOf { it.amountInr ?: 0.0 }

    fun monthlyTotals(entries: List<SpendAnalyticsEntry>): Map<String, Double> =
        eligibleEntries(entries)
            .mapNotNull { entry -> entry.transactionDate?.take(7)?.let { it to (entry.amountInr ?: 0.0) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

    fun byCategory(entries: List<SpendAnalyticsEntry>): List<SpendAnalyticsGroup> =
        eligibleEntries(entries).groupedBy(
            label = { it.category.orEmpty().ifBlank { "Other" } },
            category = { it.category.orEmpty().ifBlank { "Other" } }
        )

    fun byMerchant(entries: List<SpendAnalyticsEntry>): List<SpendAnalyticsGroup> =
        eligibleEntries(entries).groupedBy(
            label = { it.merchant.orEmpty().ifBlank { "Unknown" } },
            category = { it.category.orEmpty().ifBlank { "Other" } }
        )

    /** Finds repeated monthly merchant activity without relying on merchant-specific keywords. */
    fun recurringCandidates(entries: List<SpendAnalyticsEntry>): List<RecurringSpendCandidate> =
        eligibleEntries(entries)
            .filter { !it.transactionDate.isNullOrBlank() && MerchantNormalizer.key(it.merchant).isNotBlank() }
            .groupBy { MerchantNormalizer.key(it.merchant) }
            .mapNotNull { (_, merchantEntries) ->
                val dated = merchantEntries.sortedByDescending { it.transactionDate.orEmpty() }
                val months = dated.mapNotNull { it.transactionDate?.take(7) }.distinct()
                if (months.size < 2) return@mapNotNull null

                val amounts = dated.mapNotNull { it.amountInr }
                if (amounts.isEmpty()) return@mapNotNull null
                val expected = amounts.average()
                val closeCount = amounts.count { amount ->
                    kotlin.math.abs(amount - expected) <= maxOf(20.0, expected * 0.12)
                }
                if (months.size < 3 && closeCount < 2) return@mapNotNull null

                RecurringSpendCandidate(
                    merchant = dated.first().merchant.orEmpty(),
                    expectedAmount = expected,
                    count = dated.size,
                    distinctMonths = months.size,
                    lastSeenDate = dated.first().transactionDate.orEmpty(),
                    confidence = if (months.size >= 3 && closeCount >= 2) RecurringConfidence.LIKELY else RecurringConfidence.POSSIBLE,
                    sourceHashes = dated.map { it.sourceHash }
                )
            }
            .sortedWith(
                compareByDescending<RecurringSpendCandidate> { it.confidence == RecurringConfidence.LIKELY }
                    .thenByDescending { it.distinctMonths }
                    .thenByDescending { it.expectedAmount }
                    .thenBy { it.merchant.lowercase() }
            )
            .take(8)

    private fun List<SpendAnalyticsEntry>.groupedBy(
        label: (SpendAnalyticsEntry) -> String,
        category: (SpendAnalyticsEntry) -> String
    ): List<SpendAnalyticsGroup> = groupBy(label)
        .map { (groupLabel, transactions) ->
            SpendAnalyticsGroup(
                label = groupLabel,
                count = transactions.size,
                total = total(transactions),
                sourceHashes = transactions.map { it.sourceHash },
                category = transactions.firstOrNull()?.let(category)
            )
        }
        .sortedWith(compareByDescending<SpendAnalyticsGroup> { it.total }.thenBy { it.label.lowercase() })
}
