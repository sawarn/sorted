package sorted

object Categorizer {
    fun categorize(facts: ParserFacts): CategoryResult {
        val raw = facts.merchantRaw?.trim().orEmpty()
        val key = raw.uppercase()

        merchantRule(key)?.let {
            return it.copy(confidence = 0.95)
        }

        if (key.contains("MUTUAL FUND")) {
            return CategoryResult(
                merchantNormalized = titleCasePreservingNcl(raw),
                miscCategory = "Mutual Fund",
                departmentCategory = "Investment",
                transactionType = TransactionType.INVESTMENT,
                categorySource = CategorySource.KEYWORD_RULE,
                confidence = 0.86
            )
        }

        if (key.contains("REFUND")) {
            return CategoryResult(
                merchantNormalized = if (raw.isBlank()) "Refund" else titleCase(raw),
                miscCategory = "Refund",
                departmentCategory = "Refund",
                transactionType = TransactionType.REFUND,
                categorySource = CategorySource.KEYWORD_RULE,
                confidence = 0.84
            )
        }

        if (facts.paymentMode == PaymentMode.UPI && looksLikePersonName(raw)) {
            return CategoryResult(
                merchantNormalized = titleCase(raw),
                miscCategory = "Person Transfer",
                departmentCategory = "Transfer",
                transactionType = TransactionType.TRANSFER,
                categorySource = CategorySource.PAYMENT_MODE_RULE,
                confidence = 0.68
            )
        }

        if (facts.direction == Direction.CREDIT) {
            return CategoryResult(
                merchantNormalized = normalizeUnknownMerchant(raw),
                miscCategory = "Income",
                departmentCategory = "Income",
                transactionType = TransactionType.INCOME,
                categorySource = CategorySource.DIRECTION_RULE,
                confidence = 0.55
            )
        }

        if (facts.direction == Direction.DEBIT) {
            return CategoryResult(
                merchantNormalized = normalizeUnknownMerchant(raw),
                miscCategory = "Uncategorized",
                departmentCategory = "Other",
                transactionType = TransactionType.EXPENSE,
                categorySource = CategorySource.FALLBACK,
                confidence = 0.35
            )
        }

        return CategoryResult(
            merchantNormalized = normalizeUnknownMerchant(raw),
            miscCategory = "Uncategorized",
            departmentCategory = "Other",
            transactionType = TransactionType.UNKNOWN,
            categorySource = CategorySource.FALLBACK,
            confidence = 0.25
        )
    }

    private fun merchantRule(key: String): CategoryResult? {
        return when {
            key == "SWIGGY" -> rule("Swiggy", "Food Delivery", "Food", TransactionType.EXPENSE)
            key == "BLINKIT" -> rule("Blinkit", "Grocery Delivery", "Groceries", TransactionType.EXPENSE)
            key.contains("GROFERS") -> rule("Blinkit", "Grocery Delivery", "Groceries", TransactionType.EXPENSE)
            key == "NETFLIX" -> rule("Netflix", "OTT Subscription", "Subscriptions", TransactionType.SUBSCRIPTION)
            key == "AMAZON PAY BALANCE" -> rule("Amazon Pay Balance", "Wallet Load", "Transfer", TransactionType.TRANSFER)
            key == "MUTUAL FUNDS NCL" -> rule("Mutual Funds NCL", "Mutual Fund", "Investment", TransactionType.INVESTMENT)
            key == "INCOME TAX REFUND" -> rule("Income Tax Refund", "Tax Refund", "Refund", TransactionType.REFUND)
            key.contains("POPCLUBPAYOUTS") -> rule("Popclub Payouts", "Cashback/Reward", "Reward", TransactionType.REWARD)
            key.contains("MERCHANTPAYOUTS") -> rule("Merchant Payouts", "Cashback/Reward", "Reward", TransactionType.REWARD)
            else -> null
        }
    }

    private fun rule(
        merchant: String,
        misc: String,
        department: String,
        type: TransactionType
    ): CategoryResult {
        return CategoryResult(
            merchantNormalized = merchant,
            miscCategory = misc,
            departmentCategory = department,
            transactionType = type,
            categorySource = CategorySource.KNOWN_MERCHANT_RULE,
            confidence = 0.95
        )
    }

    private fun looksLikePersonName(value: String): Boolean {
        if (value.isBlank()) return false
        if (value.contains("@")) return false
        val upper = value.uppercase()
        val merchantWords = listOf("BANK", "PAY", "FUND", "MART", "STORE", "INDIA", "PAYOUT", "REFUND")
        return value.split(Regex("\\s+")).size in 2..4 && merchantWords.none { upper.contains(it) }
    }

    private fun normalizeUnknownMerchant(value: String): String? {
        return value.takeIf { it.isNotBlank() }?.let(::titleCase)
    }

    private fun titleCasePreservingNcl(value: String): String {
        return titleCase(value).replace("Ncl", "NCL")
    }

    private fun titleCase(value: String): String {
        return value
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { char -> char.uppercase() }
            }
    }
}
