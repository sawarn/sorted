package com.sorted.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendAnalyticsTest {
    @Test
    fun `gross totals and groups include transfers and investments only for completed debits`() {
        val entries = listOf(
            entry("food-1", "2026-10-01", "Cafe", "Food", 100.0),
            entry("investment-1", "2026-10-02", "Groww", "Investment", 1_000.0, type = TransactionType.INVESTMENT),
            entry("transfer-1", "2026-10-03", "Own account", "Transfer", 300.0, type = TransactionType.TRANSFER),
            entry("pending-1", "2026-10-04", "Cafe", "Food", 200.0, status = TransactionStatus.PENDING),
            entry("credit-1", "2026-10-05", "Refund", "Refund", 80.0, direction = Direction.CREDIT),
            entry("unknown-fx-1", "2026-10-06", "Travel", "Travel", null)
        )

        val eligible = SpendAnalytics.eligibleEntries(entries)
        val merchants = SpendAnalytics.byMerchant(entries)
        val categories = SpendAnalytics.byCategory(entries)

        assertEquals(3, eligible.size)
        assertEquals(1_400.0, SpendAnalytics.total(eligible), 0.0)
        assertEquals(listOf("investment-1"), merchants.first { it.label == "Groww" }.sourceHashes)
        assertEquals(1_000.0, merchants.first { it.label == "Groww" }.total, 0.0)
        assertEquals(1_000.0, categories.first { it.label == "Investment" }.total, 0.0)
        assertEquals(300.0, categories.first { it.label == "Transfer" }.total, 0.0)
    }

    @Test
    fun `period filtering uses the same eligible transaction set as the period total`() {
        val entries = listOf(
            entry("oct-1", "2026-10-01", "Cafe", "Food", 100.0),
            entry("sep-1", "2026-09-30", "Groww", "Investment", 800.0, type = TransactionType.INVESTMENT),
            entry("no-date", null, "Unknown", "Other", 50.0)
        )

        val monthEntries = SpendAnalytics.eligibleEntries(entries, setOf("2026-10"))

        assertEquals(listOf("oct-1"), monthEntries.map { it.sourceHash })
        assertEquals(100.0, SpendAnalytics.total(monthEntries), 0.0)
        assertEquals(mapOf("2026-10" to 100.0, "2026-09" to 800.0), SpendAnalytics.monthlyTotals(entries))
    }

    @Test
    fun `group hashes account for every eligible input exactly once`() {
        val entries = listOf(
            entry("a", "2026-10-01", "Cafe", "Food", 20.0),
            entry("b", "2026-10-02", "Cafe", "Food", 30.0),
            entry("c", "2026-10-03", "Cafe", "Food", 40.0, status = TransactionStatus.FAILED)
        )

        val group = SpendAnalytics.byMerchant(entries).single()

        assertEquals(2, group.count)
        assertEquals(50.0, group.total, 0.0)
        assertEquals(setOf("a", "b"), group.sourceHashes.toSet())
        assertFalse("c" in group.sourceHashes)
        assertTrue(group.total == group.sourceHashes.sumOf { hash -> entries.first { it.sourceHash == hash }.amountInr ?: 0.0 })
    }

    @Test
    fun `recurring candidates use normalized merchant and only completed dated debits`() {
        val entries = listOf(
            entry("net-jan", "2026-01-05", "UPI / NetFlix.com", "Entertainment", 649.0),
            entry("net-feb", "2026-02-05", "Netflix", "Entertainment", 649.0),
            entry("net-mar", "2026-03-05", "Netflix", "Entertainment", 699.0),
            entry("same-month-a", "2026-03-06", "Cafe", "Food", 200.0),
            entry("same-month-b", "2026-03-10", "Cafe", "Food", 205.0),
            entry("pending-apr", "2026-04-05", "Netflix", "Entertainment", 649.0, status = TransactionStatus.PENDING),
            entry("refund-apr", "2026-04-06", "Netflix", "Entertainment", 649.0, direction = Direction.CREDIT)
        )

        val candidate = SpendAnalytics.recurringCandidates(entries).single()

        assertEquals("Netflix", candidate.merchant)
        assertEquals(3, candidate.count)
        assertEquals(3, candidate.distinctMonths)
        assertEquals(665.6666666666666, candidate.expectedAmount, 0.0001)
        assertEquals(RecurringConfidence.LIKELY, candidate.confidence)
        assertEquals(setOf("net-jan", "net-feb", "net-mar"), candidate.sourceHashes.toSet())
        assertEquals(candidate.count, candidate.sourceHashes.size)
    }

    private fun entry(
        sourceHash: String,
        date: String?,
        merchant: String?,
        category: String?,
        amount: Double?,
        status: TransactionStatus = TransactionStatus.COMPLETED,
        direction: Direction = Direction.DEBIT,
        type: TransactionType = TransactionType.EXPENSE
    ) = SpendAnalyticsEntry(
        sourceHash = sourceHash,
        transactionDate = date,
        merchant = merchant,
        category = category,
        amountInr = amount,
        status = status,
        direction = direction,
        transactionType = type
    )
}
