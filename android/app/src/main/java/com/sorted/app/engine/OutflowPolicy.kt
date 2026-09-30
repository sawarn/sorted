package com.sorted.app.engine

/** Determines whether a transaction contributes to the "Spent this month" total. */
object OutflowPolicy {
    fun countsTowardSpent(
        status: TransactionStatus,
        direction: Direction,
        amount: Double?
    ): Boolean =
        status == TransactionStatus.COMPLETED &&
            direction == Direction.DEBIT &&
            amount != null &&
            amount > 0.0
}
