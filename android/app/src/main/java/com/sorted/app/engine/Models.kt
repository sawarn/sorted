package com.sorted.app.engine

enum class TransactionStatus {
    COMPLETED,
    IGNORED,
    PENDING,
    FAILED,
    UNKNOWN
}

enum class Direction {
    DEBIT,
    CREDIT,
    UNKNOWN
}

enum class PaymentMode {
    UPI,
    UPI_MANDATE,
    CARD,
    NET_BANKING,
    NEFT,
    IMPS,
    RTGS,
    NACH,
    ECS,
    BANK_TRANSFER,
    ATM,
    CHEQUE,
    CASH,
    WALLET,
    PPI,
    BILLPAY,
    PAYMENT_GATEWAY,
    FASTAG,
    PROVIDENT_FUND,
    UNKNOWN
}

enum class TransactionType {
    EXPENSE,
    INCOME,
    REFUND,
    TRANSFER,
    INVESTMENT,
    SUBSCRIPTION,
    REWARD,
    UNKNOWN
}

enum class CategorySource {
    USER_RULE,
    KNOWN_MERCHANT_RULE,
    KEYWORD_RULE,
    PAYMENT_MODE_RULE,
    DIRECTION_RULE,
    FALLBACK,
    NONE
}

data class ParsedTransaction(
    val isTransaction: Boolean,
    val status: TransactionStatus,
    val amount: Double?,
    val currency: String?,
    val direction: Direction,
    val merchantRaw: String?,
    val merchantNormalized: String?,
    val miscCategory: String?,
    val departmentCategory: String?,
    val paymentMode: PaymentMode,
    val accountHint: String?,
    val transactionDate: String?,
    val transactionTime: String?,
    val transactionType: TransactionType,
    val categorySource: CategorySource,
    val confidence: Double,
    val ignoreReason: String?
)

data class ParserFacts(
    val amount: Double?,
    val currency: String?,
    val direction: Direction,
    val merchantRaw: String?,
    val paymentMode: PaymentMode,
    val accountHint: String?,
    val transactionDate: String?,
    val transactionTime: String?
)

data class CategoryResult(
    val merchantNormalized: String?,
    val miscCategory: String?,
    val departmentCategory: String?,
    val transactionType: TransactionType,
    val categorySource: CategorySource,
    val confidence: Double
)
