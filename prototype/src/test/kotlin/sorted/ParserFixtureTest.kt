package sorted

data class ExpectedTransaction(
    val isTransaction: Boolean,
    val status: TransactionStatus,
    val amount: Double?,
    val currency: String?,
    val direction: Direction,
    val merchantRaw: String?,
    val merchantNormalized: String?,
    val departmentCategory: String?,
    val paymentMode: PaymentMode,
    val accountHint: String?,
    val transactionDate: String?,
    val transactionTime: String?,
    val transactionType: TransactionType,
    val ignoreReason: String?
)

data class Fixture(
    val id: String,
    val rawMessage: String,
    val expected: ExpectedTransaction
)

fun main() {
    val failures = fixtures.mapNotNull { fixture ->
        val actual = SmsParser.parse(fixture.rawMessage)
        compare(fixture, actual)
    }

    if (failures.isNotEmpty()) {
        failures.forEach { println(it) }
        error("${failures.size} fixture(s) failed")
    }

    println("All ${fixtures.size} parser fixtures passed.")
}

private fun compare(fixture: Fixture, actual: ParsedTransaction): String? {
    val expected = fixture.expected
    val mismatches = buildList {
        check("isTransaction", expected.isTransaction, actual.isTransaction)
        check("status", expected.status, actual.status)
        check("amount", expected.amount, actual.amount)
        check("currency", expected.currency, actual.currency)
        check("direction", expected.direction, actual.direction)
        check("merchantRaw", expected.merchantRaw, actual.merchantRaw)
        check("merchantNormalized", expected.merchantNormalized, actual.merchantNormalized)
        check("departmentCategory", expected.departmentCategory, actual.departmentCategory)
        check("paymentMode", expected.paymentMode, actual.paymentMode)
        check("accountHint", expected.accountHint, actual.accountHint)
        check("transactionDate", expected.transactionDate, actual.transactionDate)
        check("transactionTime", expected.transactionTime, actual.transactionTime)
        check("transactionType", expected.transactionType, actual.transactionType)
        check("ignoreReason", expected.ignoreReason, actual.ignoreReason)
    }

    if (mismatches.isEmpty()) return null
    return "Fixture '${fixture.id}' failed:\n" + mismatches.joinToString("\n") { "  - $it" } +
        "\nActual:\n${actual.toPrettyString()}"
}

private fun <T> MutableList<String>.check(field: String, expected: T, actual: T) {
    if (expected != actual) add("$field expected <$expected> but was <$actual>")
}

private val fixtures = listOf(
    Fixture(
        id = "icici-card-spend-blinkit",
        rawMessage = "INR 606.00 spent using ICICI Bank Card XX1234 on 07-Aug-26 on Blinkit. Avl Limit: INR XX,XXX.XX. If not you, call <support>/SMS BLOCK 1234 to <support>.",
        expected = ExpectedTransaction(true, TransactionStatus.COMPLETED, 606.0, "INR", Direction.DEBIT, "Blinkit", "Blinkit", "Groceries", PaymentMode.CARD, "XX1234", "2026-08-07", null, TransactionType.EXPENSE, null)
    ),
    Fixture(
        id = "hdfc-upi-debit-swiggy-titlecase",
        rawMessage = "Sent Rs.350.00\nFrom HDFC Bank A/C *1234\nTo Swiggy\nOn 07/08/26\nRef <ref>\nNot You?\nCall <support>/SMS BLOCK UPI to <support>",
        expected = ExpectedTransaction(true, TransactionStatus.COMPLETED, 350.0, "INR", Direction.DEBIT, "Swiggy", "Swiggy", "Food", PaymentMode.UPI, "*1234", "2026-08-07", null, TransactionType.EXPENSE, null)
    ),
    Fixture(
        id = "hdfc-upi-debit-amazon-pay-balance",
        rawMessage = "Sent Rs.942.77\nFrom HDFC Bank A/C *1234\nTo Amazon Pay Balance\nOn 18/06/26\nRef <ref>\nNot You?\nCall <support>/SMS BLOCK UPI to <support>",
        expected = ExpectedTransaction(true, TransactionStatus.COMPLETED, 942.77, "INR", Direction.DEBIT, "Amazon Pay Balance", "Amazon Pay Balance", "Transfer", PaymentMode.UPI, "*1234", "2026-06-18", null, TransactionType.TRANSFER, null)
    ),
    Fixture(
        id = "hdfc-upi-debit-swiggy-uppercase",
        rawMessage = "Sent Rs.379.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 07/08/26\nRef <ref>\nNot You?\nCall <support>/SMS BLOCK UPI to <support>",
        expected = ExpectedTransaction(true, TransactionStatus.COMPLETED, 379.0, "INR", Direction.DEBIT, "SWIGGY", "Swiggy", "Food", PaymentMode.UPI, "*1234", "2026-08-07", null, TransactionType.EXPENSE, null)
    ),
    Fixture(
        id = "hdfc-upi-debit-mutual-funds",
        rawMessage = "Sent Rs.25000.00\nFrom HDFC Bank A/C *1234\nTo MUTUAL FUNDS NCL\nOn 07/08/26\nRef <ref>\nNot You?\nCall <support>/SMS BLOCK UPI to <support>",
        expected = ExpectedTransaction(true, TransactionStatus.COMPLETED, 25000.0, "INR", Direction.DEBIT, "MUTUAL FUNDS NCL", "Mutual Funds NCL", "Investment", PaymentMode.UPI, "*1234", "2026-08-07", null, TransactionType.INVESTMENT, null)
    ),
    Fixture(
        id = "pnb-upi-debit-person-transfer",
        rawMessage = "A/c X4321 debited INR 1.00 Dt 01-07-26 13:41:26 to PERSON NAME thru UPI:<ref>.Bal INR 4853.77 Not u?Fwd this SMS to <support> to block UPI.-PNB",
        expected = ExpectedTransaction(true, TransactionStatus.COMPLETED, 1.0, "INR", Direction.DEBIT, "PERSON NAME", "Person Name", "Transfer", PaymentMode.UPI, "X4321", "2026-07-01", "13:41:26", TransactionType.TRANSFER, null)
    ),
    Fixture(
        id = "icici-card-otp-ignore",
        rawMessage = "854055 is One-Time Password for INR 606.00 transaction towards GROFERS IND using ICICI Bank Credit Card XX1234. OTPs are SECRET. DO NOT disclose",
        expected = ExpectedTransaction(false, TransactionStatus.IGNORED, null, null, Direction.UNKNOWN, null, null, null, PaymentMode.UNKNOWN, null, null, null, TransactionType.UNKNOWN, "otp")
    ),
    Fixture(
        id = "sbi-income-tax-refund",
        rawMessage = "Dear Customer, For PAN XXXXXX000X, An IT Refund amount of Rs 1980 for AY-2026-27 has been credited to your account XXXXXXXXXX1234 on 2026-08-07. -SBI",
        expected = ExpectedTransaction(true, TransactionStatus.COMPLETED, 1980.0, "INR", Direction.CREDIT, "Income Tax Refund", "Income Tax Refund", "Refund", PaymentMode.BANK_TRANSFER, "XX1234", "2026-08-07", null, TransactionType.REFUND, null)
    ),
    Fixture(
        id = "hdfc-upi-credit-vpa",
        rawMessage = "Credit Alert!\nRs.37.50 credited to HDFC Bank A/c XX1234 on 02-08-26 from VPA merchantpayouts@example (UPI <ref>)",
        expected = ExpectedTransaction(true, TransactionStatus.COMPLETED, 37.5, "INR", Direction.CREDIT, "merchantpayouts@example", "Merchant Payouts", "Reward", PaymentMode.UPI, "XX1234", "2026-08-02", null, TransactionType.REWARD, null)
    ),
    Fixture(
        id = "hdfc-upi-mandate-netflix",
        rawMessage = "UPI Mandate:\nSent Rs.199.00\nfrom HDFC Bank A/c 1234\nTo NETFLIX\n22/07/26\nRef <ref>\nNot You? Call <support>/SMS BLOCK UPI to <support>",
        expected = ExpectedTransaction(true, TransactionStatus.COMPLETED, 199.0, "INR", Direction.DEBIT, "NETFLIX", "Netflix", "Subscriptions", PaymentMode.UPI_MANDATE, "1234", "2026-07-22", null, TransactionType.SUBSCRIPTION, null)
    ),
    Fixture(
        id = "hdfc-autopay-consent-ignore",
        rawMessage = "AutoPay (E-mandate) Consent Requested!\nFor:Example SaaS\nAmt Due: INR2285.63\nCurrent Limit: INR23.60\nDate:16/07/2026\nVia: HDFC Bank CC 5678\nReason:Txn value above the limit or exceeds RBI approved limit of 15K\nSI HUB ID:<mandate-id>\nClick sihub.in/managesi/hdfcbank to authenticate via OTP before due date.\nTnC",
        expected = ExpectedTransaction(false, TransactionStatus.IGNORED, null, null, Direction.UNKNOWN, null, null, null, PaymentMode.UNKNOWN, null, null, null, TransactionType.UNKNOWN, "consent_request")
    ),
    Fixture(
        id = "epfo-pf-interest-ignore-for-mvp",
        rawMessage = "PF interest of 2654 for 2025-26 credited to your UAN <masked> (<masked>) The CB on 31MAR2026 is 50107 - EPFO",
        expected = ExpectedTransaction(false, TransactionStatus.IGNORED, null, null, Direction.UNKNOWN, null, null, null, PaymentMode.UNKNOWN, null, null, null, TransactionType.UNKNOWN, "unsupported")
    )
)
