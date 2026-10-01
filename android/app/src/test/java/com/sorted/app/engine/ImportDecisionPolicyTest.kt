package com.sorted.app.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportDecisionPolicyTest {
    @Test
    fun `clear completed movement is accepted`() {
        val result = ImportDecisionPolicy.assess(
            transaction(
                evidenceConfidence = 0.96,
                confidence = 0.92,
                categorySource = CategorySource.KNOWN_MERCHANT_RULE
            )
        )

        assertEquals(ImportDecision.ACCEPT, result.decision)
    }

    @Test
    fun `clear movement with fallback category goes to review`() {
        val result = ImportDecisionPolicy.assess(
            transaction(
                evidenceConfidence = 0.96,
                confidence = 0.35,
                categorySource = CategorySource.FALLBACK,
                merchantNormalized = "Green Leaf Cafe"
            )
        )

        assertEquals(ImportDecision.REVIEW, result.decision)
        assertEquals("category_needs_review", result.reason)
    }

    @Test
    fun `weak movement evidence is ignored`() {
        val result = ImportDecisionPolicy.assess(
            transaction(evidenceConfidence = 0.35)
        )

        assertEquals(ImportDecision.IGNORE, result.decision)
        assertEquals("insufficient_transaction_evidence", result.reason)
    }

    private fun transaction(
        evidenceConfidence: Double,
        confidence: Double = 0.90,
        categorySource: CategorySource = CategorySource.KNOWN_MERCHANT_RULE,
        merchantNormalized: String? = "Cafe"
    ) = ParsedTransaction(
        isTransaction = true,
        status = TransactionStatus.COMPLETED,
        amount = 125.0,
        currency = "INR",
        direction = Direction.DEBIT,
        merchantRaw = merchantNormalized,
        merchantNormalized = merchantNormalized,
        miscCategory = "Food",
        departmentCategory = "Food",
        paymentMode = PaymentMode.UPI,
        accountHint = null,
        transactionDate = "2026-10-01",
        transactionTime = null,
        transactionType = TransactionType.EXPENSE,
        categorySource = categorySource,
        confidence = confidence,
        ignoreReason = null,
        evidenceConfidence = evidenceConfidence
    )
}
