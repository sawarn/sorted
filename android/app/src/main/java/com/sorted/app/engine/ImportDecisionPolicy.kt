package com.sorted.app.engine

/** Separates transaction evidence from category confidence. */
object ImportDecisionPolicy {
    const val MinimumEvidenceConfidence = 0.65
    const val MinimumCategoryConfidence = 0.70

    fun assess(transaction: ParsedTransaction): ImportAssessment {
        if (!transaction.isTransaction || transaction.status == TransactionStatus.IGNORED) {
            return ImportAssessment(
                decision = ImportDecision.IGNORE,
                reason = transaction.ignoreReason ?: "not_a_transaction"
            )
        }
        if (transaction.status == TransactionStatus.FAILED) {
            return ImportAssessment(ImportDecision.IGNORE, "failed_transaction")
        }
        if (transaction.evidenceConfidence < MinimumEvidenceConfidence) {
            return ImportAssessment(ImportDecision.IGNORE, "insufficient_transaction_evidence")
        }
        if (transaction.status != TransactionStatus.COMPLETED) {
            return ImportAssessment(ImportDecision.REVIEW, "payment_status_unclear")
        }
        if (transaction.amount == null || transaction.amount <= 0.0) {
            return ImportAssessment(ImportDecision.REVIEW, "amount_unclear")
        }
        if (transaction.direction == Direction.UNKNOWN) {
            return ImportAssessment(ImportDecision.REVIEW, "payment_direction_unclear")
        }
        if (
            transaction.confidence < MinimumCategoryConfidence ||
            transaction.categorySource == CategorySource.FALLBACK ||
            transaction.merchantNormalized.isNullOrBlank()
        ) {
            return ImportAssessment(ImportDecision.REVIEW, "category_needs_review")
        }
        return ImportAssessment(ImportDecision.ACCEPT, "transaction_evidence_clear")
    }
}
