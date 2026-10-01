package com.sorted.app.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutflowPolicyTest {
    @Test
    fun `all completed positive outgoing debits count regardless of transaction type`() {
        TransactionType.entries.forEach { type ->
            assertTrue(
                "Expected $type debit to count",
                OutflowPolicy.countsTowardSpent(
                    status = TransactionStatus.COMPLETED,
                    direction = Direction.DEBIT,
                    amount = 125.0,
                    type = type
                )
            )
        }
    }

    @Test
    fun `credits and non completed payments do not count`() {
        assertFalse(counts(status = TransactionStatus.COMPLETED, direction = Direction.CREDIT))
        assertFalse(counts(status = TransactionStatus.PENDING))
        assertFalse(counts(status = TransactionStatus.FAILED))
        assertFalse(counts(status = TransactionStatus.IGNORED))
        assertFalse(counts(status = TransactionStatus.UNKNOWN))
    }

    @Test
    fun `missing and non positive amounts do not count`() {
        assertFalse(counts(amount = null))
        assertFalse(counts(amount = 0.0))
        assertFalse(counts(amount = -1.0))
    }

    private fun counts(
        status: TransactionStatus = TransactionStatus.COMPLETED,
        direction: Direction = Direction.DEBIT,
        amount: Double? = 125.0,
        type: TransactionType = TransactionType.EXPENSE
    ) = OutflowPolicy.countsTowardSpent(status, direction, amount, type)
}
