package com.sorted.app.engine

object SmsParser {
    fun parse(rawMessage: String, sourceAddress: String? = null): ParsedTransaction {
        return runCatching {
            parseInternal(rawMessage, sourceAddress)
        }.getOrElse {
            ignored("parser_error")
        }
    }

    private fun parseInternal(rawMessage: String, sourceAddress: String?): ParsedTransaction {
        val cleaned = clean(rawMessage)

        ignoreReason(cleaned, sourceAddress)?.let { reason ->
            return ignored(reason)
        }

        val facts = parseKnownTemplate(cleaned)
            ?: parseGenericFinancialAlert(cleaned, sourceAddress)
            ?: return ignored("unsupported")

        val category = Categorizer.categorize(facts)
        return ParsedTransaction(
            isTransaction = true,
            status = TransactionStatus.COMPLETED,
            amount = facts.amount,
            currency = facts.currency,
            direction = facts.direction,
            merchantRaw = facts.merchantRaw,
            merchantNormalized = category.merchantNormalized,
            miscCategory = category.miscCategory,
            departmentCategory = category.departmentCategory,
            paymentMode = facts.paymentMode,
            accountHint = facts.accountHint,
            transactionDate = facts.transactionDate,
            transactionTime = facts.transactionTime,
            transactionType = category.transactionType,
            categorySource = category.categorySource,
            confidence = category.confidence,
            ignoreReason = null
        )
    }

    private fun parseKnownTemplate(cleaned: String): ParserFacts? {
        return when {
            cleaned.contains("spent using ICICI Bank Card", ignoreCase = true) -> parseIciciCardSpend(cleaned)
            cleaned.startsWith("Txn Rs.", ignoreCase = true) && cleaned.contains("HDFC Bank Card", ignoreCase = true) -> parseHdfcCardTxn(cleaned)
            cleaned.contains("UPI Mandate:", ignoreCase = true) -> parseHdfcUpiMandate(cleaned)
            cleaned.startsWith("Sent Rs.", ignoreCase = true) -> parseHdfcUpiDebit(cleaned)
            cleaned.contains(" debited INR ", ignoreCase = true) && cleaned.contains(" thru UPI", ignoreCase = true) -> parsePnbUpiDebit(cleaned)
            cleaned.contains("IT Refund amount", ignoreCase = true) -> parseSbiTaxRefund(cleaned)
            cleaned.contains("Credit Alert!", ignoreCase = true) && cleaned.contains(" credited to HDFC Bank", ignoreCase = true) -> parseHdfcUpiCredit(cleaned)
            cleaned.contains("PAYMENT ALERT!", ignoreCase = true) && cleaned.contains(" deducted from ", ignoreCase = true) -> parsePaymentAlertDeduction(cleaned)
            cleaned.contains("deducted towards PMJJBY", ignoreCase = true) -> parsePmjjbyDebit(cleaned)
            else -> null
        }
    }

    private fun parseIciciCardSpend(message: String): ParserFacts? {
        val regex = Regex(
            """INR\s+([\d,]+(?:\.\d+)?)\s+spent using ICICI Bank Card\s+(\S+)\s+on\s+(\d{2}-[A-Za-z]{3}-\d{2})\s+on\s+(.+?)(?:\.|$)""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(message) ?: return null
        return ParserFacts(
            amount = parseAmount(match.groupValues[1]),
            currency = "INR",
            direction = Direction.DEBIT,
            merchantRaw = match.groupValues[4].trim(),
            paymentMode = PaymentMode.CARD,
            accountHint = match.groupValues[2],
            transactionDate = parseDate(match.groupValues[3]),
            transactionTime = null
        )
    }

    private fun parseHdfcUpiDebit(message: String): ParserFacts? {
        val amount = Regex("""Sent\s+Rs\.([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.let(::parseAmount)
        val account = Regex("""From\s+HDFC Bank A/C\s+(\S+)""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)
        val merchant = Regex("""(?m)^To\s+(.+)$""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.trim()
        val date = Regex("""(?m)^On\s+(\d{2}/\d{2}/\d{2})$""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.let(::parseDate)

        if (amount == null || merchant == null) return null
        return ParserFacts(
            amount = amount,
            currency = "INR",
            direction = Direction.DEBIT,
            merchantRaw = merchant,
            paymentMode = PaymentMode.UPI,
            accountHint = account,
            transactionDate = date,
            transactionTime = null
        )
    }

    private fun parseHdfcCardTxn(message: String): ParserFacts? {
        val amount = Regex("""Txn\s+Rs\.([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.let(::parseAmount)
        val cardHint = Regex("""On\s+HDFC Bank Card\s+(\S+)""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)
        val merchant = Regex("""(?m)^At\s+(.+?)\s*$""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.trim()

        if (amount == null || merchant == null) return null
        return ParserFacts(
            amount = amount,
            currency = "INR",
            direction = Direction.DEBIT,
            merchantRaw = merchant,
            paymentMode = PaymentMode.CARD,
            accountHint = cardHint,
            transactionDate = null,
            transactionTime = null
        )
    }

    private fun parseHdfcUpiMandate(message: String): ParserFacts? {
        val amount = Regex("""Sent\s+Rs\.([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.let(::parseAmount)
        val account = Regex("""from\s+HDFC Bank A/c\s+(\S+)""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)
        val merchant = Regex("""(?m)^To\s+(.+)$""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.trim()
        val date = Regex("""(?m)^(\d{2}/\d{2}/\d{2})$""")
            .find(message)?.groupValues?.get(1)?.let(::parseDate)

        if (amount == null || merchant == null) return null
        return ParserFacts(
            amount = amount,
            currency = "INR",
            direction = Direction.DEBIT,
            merchantRaw = merchant,
            paymentMode = PaymentMode.UPI_MANDATE,
            accountHint = account,
            transactionDate = date,
            transactionTime = null
        )
    }

    private fun parsePnbUpiDebit(message: String): ParserFacts? {
        val regex = Regex(
            """A/c\s+(\S+)\s+debited\s+INR\s+([\d,]+(?:\.\d+)?)\s+Dt\s+(\d{2}-\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2})\s+to\s+(.+?)\s+thru\s+UPI""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(message) ?: return null
        return ParserFacts(
            amount = parseAmount(match.groupValues[2]),
            currency = "INR",
            direction = Direction.DEBIT,
            merchantRaw = match.groupValues[5].trim(),
            paymentMode = PaymentMode.UPI,
            accountHint = match.groupValues[1],
            transactionDate = parseDate(match.groupValues[3]),
            transactionTime = match.groupValues[4]
        )
    }

    private fun parseSbiTaxRefund(message: String): ParserFacts? {
        val amount = Regex("""IT Refund amount of Rs\s+([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.let(::parseAmount)
        val account = Regex("""account\s+([Xx\d*]+)(?:\s+on|\s)""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.takeLast(6)?.replace(Regex("^X+"), "XX")
        val date = Regex("""on\s+(\d{4}-\d{2}-\d{2})""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)

        if (amount == null) return null
        return ParserFacts(
            amount = amount,
            currency = "INR",
            direction = Direction.CREDIT,
            merchantRaw = "Income Tax Refund",
            paymentMode = PaymentMode.BANK_TRANSFER,
            accountHint = account,
            transactionDate = date,
            transactionTime = null
        )
    }

    private fun parseHdfcUpiCredit(message: String): ParserFacts? {
        val regex = Regex(
            """Rs\.([\d,]+(?:\.\d+)?)\s+credited to HDFC Bank A/c\s+(\S+)\s+on\s+(\d{2}-\d{2}-\d{2})\s+from VPA\s+(.+?)\s+\(UPI""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(message) ?: return null
        return ParserFacts(
            amount = parseAmount(match.groupValues[1]),
            currency = "INR",
            direction = Direction.CREDIT,
            merchantRaw = match.groupValues[4].trim(),
            paymentMode = PaymentMode.UPI,
            accountHint = match.groupValues[2],
            transactionDate = parseDate(match.groupValues[3]),
            transactionTime = null
        )
    }

    private fun parsePaymentAlertDeduction(message: String): ParserFacts? {
        val regex = Regex(
            """INR\s+([\d,]+(?:\.\d+)?)\s+deducted from\s+(.+?)\s+A/C No\s+(\S+)\s+towards\s+(.+?)(?:\s+UMRN:|$)""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(message) ?: return null
        return ParserFacts(
            amount = parseAmount(match.groupValues[1]),
            currency = "INR",
            direction = Direction.DEBIT,
            merchantRaw = match.groupValues[4].trim(),
            paymentMode = parseGenericPaymentMode(message),
            accountHint = match.groupValues[3].takeLast(4),
            transactionDate = null,
            transactionTime = null
        )
    }

    private fun parsePmjjbyDebit(message: String): ParserFacts? {
        val amount = Regex("""Premium of Rs\.?\s*([\d,]+(?:\.\d+)?)\s+deducted towards\s+(PMJJBY)""", RegexOption.IGNORE_CASE)
            .find(message)
        val account = Regex("""from A/c\s+([Xx\d*]+)""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.takeLast(6)?.replace(Regex("^X+"), "XX")
        val date = Regex("""dt\s+(\d{2}-\d{2}-\d{4})""", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.get(1)?.let(::parseDate)

        if (amount == null) return null
        return ParserFacts(
            amount = parseAmount(amount.groupValues[1]),
            currency = "INR",
            direction = Direction.DEBIT,
            merchantRaw = amount.groupValues[2].uppercase(),
            paymentMode = parseGenericPaymentMode(message),
            accountHint = account,
            transactionDate = date,
            transactionTime = null
        )
    }

    private fun parseGenericFinancialAlert(message: String, sourceAddress: String?): ParserFacts? {
        if (!looksLikeFinancialAlert(message, sourceAddress)) return null

        val direction = parseGenericDirection(message)
        if (direction == Direction.UNKNOWN) return null

        val amount = parseBestAmount(message, direction) ?: return null
        val paymentMode = parseGenericPaymentMode(message)
        val merchant = parseGenericMerchant(message, direction)
            ?: parseMerchantFromSourceAddress(sourceAddress)
        val accountHint = parseGenericAccountHint(message)
        val date = parseGenericDate(message)
        val time = parseGenericTime(message)

        return ParserFacts(
            amount = amount,
            currency = "INR",
            direction = direction,
            merchantRaw = merchant,
            paymentMode = paymentMode,
            accountHint = accountHint,
            transactionDate = date,
            transactionTime = time
        )
    }

    private fun looksLikeFinancialAlert(message: String, sourceAddress: String?): Boolean {
        val lower = message.lowercase()
        val sender = sourceAddress.orEmpty().lowercase()
        val bodySignals = listOf(
            "inr",
            "rs.",
            "₹",
            "debited",
            "credited",
            "spent",
            "deducted",
            "paid",
            "sent",
            "received",
            "withdrawn",
            "deposited",
            "refund",
            "reversal",
            "upi",
            "imps",
            "neft",
            "rtgs",
            "nach",
            "ecs",
            "card",
            "wallet",
            "a/c",
            "account"
        )
        val senderSignals = listOf(
            "bank",
            "bnk",
            "hdfc",
            "icici",
            "sbi",
            "axis",
            "kotak",
            "pnb",
            "canara",
            "yesbnk",
            "idfc",
            "indus",
            "federal",
            "rbl",
            "paytm",
            "phonepe",
            "gpay",
            "cred"
        )
        return bodySignals.any { it in lower } || senderSignals.any { it in sender }
    }

    private fun parseGenericDirection(message: String): Direction {
        val lower = message.lowercase()
        val creditWords = listOf(
            "credited",
            "credit alert",
            "received",
            "deposited",
            "refund",
            "refunded",
            "reversal",
            "reversed",
            "cashback",
            "reward",
            "interest"
        )
        val debitWords = listOf(
            "debited",
            "debit alert",
            "spent",
            "deducted",
            "paid",
            "sent",
            "withdrawn",
            "purchase",
            "charged",
            "transferred",
            "dr "
        )

        return when {
            creditWords.any { it in lower } -> Direction.CREDIT
            debitWords.any { it in lower } -> Direction.DEBIT
            else -> Direction.UNKNOWN
        }
    }

    private data class AmountCandidate(
        val amount: Double,
        val startIndex: Int,
        val score: Int
    )

    private fun parseBestAmount(message: String, direction: Direction): Double? {
        val candidates = Regex("""(?i)(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d+)?)""")
            .findAll(message)
            .mapNotNull { match ->
                val amount = parseAmountOrNull(match.groupValues.getOrNull(1).orEmpty())
                    ?: return@mapNotNull null
                val context = message.windowAround(match.range.first, radius = 56).lowercase()
                val score = amountContextScore(context, direction)
                AmountCandidate(amount = amount, startIndex = match.range.first, score = score)
            }
            .toList()

        if (candidates.isEmpty()) return null
        return candidates.maxWith(compareBy<AmountCandidate> { it.score }.thenBy { -it.startIndex }).amount
    }

    private fun amountContextScore(context: String, direction: Direction): Int {
        val amountWords = when (direction) {
            Direction.CREDIT -> listOf("credited", "received", "deposited", "refund", "reversal", "cashback", "interest")
            Direction.DEBIT -> listOf("debited", "spent", "deducted", "paid", "sent", "withdrawn", "purchase", "charged")
            Direction.UNKNOWN -> emptyList()
        }
        val noiseWords = listOf(
            "bal",
            "balance",
            "avl",
            "available",
            "limit",
            "outstanding",
            "minimum",
            "min due",
            "total due",
            "due date",
            "cashback offer",
            "reward points"
        )

        var score = 0
        if (amountWords.any { it in context }) score += 12
        if ("transaction" in context || "txn" in context) score += 4
        if ("on " in context || " at " in context || " to " in context || " from " in context) score += 2
        if (noiseWords.any { it in context }) score -= 18
        return score
    }

    private fun parseGenericMerchant(message: String, direction: Direction): String? {
        val patterns = when (direction) {
            Direction.CREDIT -> listOf(
                Regex("""(?is)\bfrom\s+(?:VPA\s+)?(.+?)(?:\s+(?:on|via|using|through|thru|ref|rrn|upi|txn|transaction|a/c|account)\b|[.,\n]|$)"""),
                Regex("""(?is)\bby\s+(.+?)(?:\s+(?:on|via|using|through|thru|ref|rrn|upi|txn|transaction)\b|[.,\n]|$)""")
            )
            Direction.DEBIT -> listOf(
                Regex("""(?is)\b(?:paid to|payment to|sent to|transferred to|to|towards|at|merchant|biller|beneficiary)\s+(?:VPA\s+)?(.+?)(?:\s+(?:on|via|using|through|thru|ref|rrn|upi|txn|transaction|a/c|account|if not|not you|bal|avl)\b|[.,\n]|$)"""),
                Regex("""(?is)\bon\s+([A-Za-z][A-Za-z0-9 .&@/_-]{1,80})(?:\s+(?:via|using|through|thru|ref|rrn|upi|txn|transaction|if not|not you|bal|avl)\b|[.,\n]|$)""")
            )
            Direction.UNKNOWN -> emptyList()
        }

        return patterns
            .firstNotNullOfOrNull { pattern -> pattern.find(message)?.groupValues?.get(1)?.cleanMerchant() }
    }

    private fun parseMerchantFromSourceAddress(sourceAddress: String?): String? {
        val cleaned = sourceAddress
            ?.replace(Regex("""^[A-Z]{2}-"""), "")
            ?.replace(Regex("""-[PSTG]$"""), "")
            ?.replace(Regex("""[^A-Za-z0-9]"""), " ")
            ?.trim()
            .orEmpty()
        val lower = cleaned.lowercase()
        val banks = listOf("hdfc", "icici", "sbi", "pnb", "axis", "kotak", "bank", "bnk")
        return cleaned.takeIf { it.length >= 3 && banks.none { bank -> bank in lower } }
    }

    private fun parseGenericPaymentMode(message: String): PaymentMode {
        val lower = message.lowercase()
        return when {
            "upi mandate" in lower || ("upi" in lower && "mandate" in lower) -> PaymentMode.UPI_MANDATE
            "upi" in lower || "vpa" in lower || Regex("""@[a-z]{2,}""").containsMatchIn(lower) -> PaymentMode.UPI
            "fastag" in lower || "netc" in lower -> PaymentMode.FASTAG
            "nach" in lower || "ach" in lower || "umrn" in lower -> PaymentMode.NACH
            "ecs" in lower -> PaymentMode.ECS
            "imps" in lower -> PaymentMode.IMPS
            "neft" in lower -> PaymentMode.NEFT
            "rtgs" in lower -> PaymentMode.RTGS
            "net banking" in lower || "netbanking" in lower || "internet banking" in lower || " inb " in " $lower " -> PaymentMode.NET_BANKING
            "atm" in lower || "cash withdrawal" in lower || "withdrawn" in lower -> PaymentMode.ATM
            "cheque" in lower || " chq " in " $lower " -> PaymentMode.CHEQUE
            "cash deposit" in lower || "cash deposited" in lower -> PaymentMode.CASH
            "wallet" in lower || "ppi" in lower || "pay balance" in lower -> PaymentMode.WALLET
            "billpay" in lower || "bbps" in lower || "bharat bill" in lower -> PaymentMode.BILLPAY
            listOf("razorpay", "rzp", "payu", "cashfree", "billdesk", "juspay", "ccavenue", "paytm").any { it in lower } -> PaymentMode.PAYMENT_GATEWAY
            "card" in lower || Regex("""\b(?:cc|dc)\s*[x*]?\d{3,4}\b""", RegexOption.IGNORE_CASE).containsMatchIn(message) -> PaymentMode.CARD
            "bank" in lower || "a/c" in lower || "account" in lower -> PaymentMode.BANK_TRANSFER
            else -> PaymentMode.UNKNOWN
        }
    }

    private fun parseGenericAccountHint(message: String): String? {
        val patterns = listOf(
            Regex("""(?i)\b(?:a/c|account)(?:\s+no)?\s*([Xx*]*\d{3,6})"""),
            Regex("""(?i)\b(?:card|cc|dc)\s*([Xx*]*\d{3,6})"""),
            Regex("""(?i)\b(?:ending|xx|\*)\s*(\d{3,6})""")
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(message)?.groupValues?.get(1)
        }
    }

    private fun parseGenericDate(message: String): String? {
        Regex("""\b(\d{4})[-/](\d{1,2})[-/](\d{1,2})\b""")
            .find(message)
            ?.let { return "${it.groupValues[1]}-${it.groupValues[2].pad2()}-${it.groupValues[3].pad2()}" }

        Regex("""\b(\d{1,2})[-/](\d{1,2})[-/](\d{2,4})\b""")
            .find(message)
            ?.let {
                val year = it.groupValues[3].normalizeYear()
                return "$year-${it.groupValues[2].pad2()}-${it.groupValues[1].pad2()}"
            }

        Regex("""(?i)\b(\d{1,2})[- ]([A-Za-z]{3,9})[- ](\d{2,4})\b""")
            .find(message)
            ?.let {
                val month = monthNumberOrNull(it.groupValues[2]) ?: return@let
                return "${it.groupValues[3].normalizeYear()}-$month-${it.groupValues[1].pad2()}"
            }

        return null
    }

    private fun parseGenericTime(message: String): String? {
        return Regex("""\b(\d{2}:\d{2}(?::\d{2})?)\b""")
            .find(message)
            ?.groupValues
            ?.get(1)
    }

    private fun ignoreReason(message: String, sourceAddress: String?): String? {
        val lower = message.lowercase()
        val sender = sourceAddress.orEmpty().lowercase()
        val senderLooksLikeBankOrPaymentApp = listOf(
            "bank",
            "bnk",
            "hdfc",
            "icici",
            "sbi",
            "axis",
            "kotak",
            "pnb",
            "canara",
            "yesbnk",
            "idfc",
            "indus",
            "federal",
            "rbl",
            "paytm",
            "phonepe",
            "gpay",
            "cred"
        ).any { it in sender }

        return when {
            "consent requested" in lower || "authenticate via otp" in lower -> "consent_request"
            "mandate request" in lower && "debited" !in lower && "deducted" !in lower -> "consent_request"
            "not completed" in lower || "payment failure" in lower || "failed" in lower || "declined" in lower || "unsuccessful" in lower -> "failed_transaction"
            "will be deducted" in lower || "will be debited" in lower || "will be credited" in lower || "upcoming mandate" in lower || "pre-debit" in lower -> "pending"
            "settlement worth" in lower || "settlement has been processed" in lower -> "pending"
            "sip installment" in lower || "sip instalment" in lower || "units are allotted" in lower -> "investment_allotment"
            "one-time password" in lower || Regex("""\botp\b""").containsMatchIn(lower) -> "otp"
            "statement" in lower && listOf("debited", "credited", "spent", "deducted").none { it in lower } -> "statement"
            "total amount due" in lower || "minimum amount due" in lower || "min amount due" in lower -> "bill_due"
            "due for payment" in lower || ("pay by" in lower && "ignore if paid" in lower) -> "bill_due"
            "invoice" in lower && ("generated" in lower || "total due" in lower || "pay by" in lower) -> "bill_due"
            "credited to your card" in lower || "received towards your credit card" in lower -> "card_payment_ack"
            "we have received a payment" in lower && !senderLooksLikeBankOrPaymentApp -> "merchant_receipt"
            lower.startsWith("pf interest") && "epfo" in lower -> "unsupported"
            else -> null
        }
    }

    private fun ignored(reason: String): ParsedTransaction {
        return ParsedTransaction(
            isTransaction = false,
            status = TransactionStatus.IGNORED,
            amount = null,
            currency = null,
            direction = Direction.UNKNOWN,
            merchantRaw = null,
            merchantNormalized = null,
            miscCategory = null,
            departmentCategory = null,
            paymentMode = PaymentMode.UNKNOWN,
            accountHint = null,
            transactionDate = null,
            transactionTime = null,
            transactionType = TransactionType.UNKNOWN,
            categorySource = CategorySource.NONE,
            confidence = 0.0,
            ignoreReason = reason
        )
    }

    private fun clean(rawMessage: String): String {
        val withoutChatPrefix = rawMessage.replace(
            Regex("""^\[\d{2}/\d{2}/\d{2},\s+.+?]\s+.+?:\s*"""),
            ""
        )
        return withoutChatPrefix
            .replace("\u202f", " ")
            .replace("\u00a0", " ")
            .trim()
    }

    private fun parseAmount(value: String): Double {
        return parseAmountOrNull(value) ?: throw IllegalArgumentException("Invalid amount: $value")
    }

    private fun parseAmountOrNull(value: String): Double? {
        return value
            .replace(",", "")
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.toDoubleOrNull()
    }

    private fun String.windowAround(index: Int, radius: Int): String {
        val start = (index - radius).coerceAtLeast(0)
        val end = (index + radius).coerceAtMost(length)
        return substring(start, end)
    }

    private fun String.cleanMerchant(): String? {
        val candidate = replace(Regex("""(?i)\s+(?:on|using|via|through|thru|for|with|ref|reference|transaction|txn|upi|rrn|card|a/c|account|if not|not you|bal|avl|limit)\b.*"""), "")
            .replace(Regex("""(?i)\b(?:no|number)\s+\d+.*"""), "")
            .replace(Regex("""[.,;:]+$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(80)

        val lower = candidate.lowercase()
        val blocked = listOf(
            "your account",
            "my account",
            "account",
            "card",
            "credit card",
            "debit card",
            "hdfc bank",
            "icici bank",
            "sbi",
            "pnb",
            "bank",
            "a c",
            "vpa"
        )
        return candidate.takeIf {
            it.length >= 2 &&
                blocked.none { blockedValue -> lower == blockedValue || lower.startsWith("$blockedValue ") }
        }
    }

    private fun String.pad2(): String = padStart(2, '0')

    private fun String.normalizeYear(): String {
        return if (length == 2) "20$this" else this
    }

    private fun parseDate(value: String): String {
        return when {
            Regex("""\d{4}-\d{2}-\d{2}""").matches(value) -> value
            Regex("""\d{2}/\d{2}/\d{2}""").matches(value) -> {
                val (day, month, year) = value.split("/")
                "20$year-$month-$day"
            }
            Regex("""\d{2}-\d{2}-\d{2}""").matches(value) -> {
                val (day, month, year) = value.split("-")
                "20$year-$month-$day"
            }
            Regex("""\d{2}-[A-Za-z]{3}-\d{2}""").matches(value) -> {
                val (day, monthName, year) = value.split("-")
                val month = monthNumber(monthName)
                "20$year-$month-$day"
            }
            else -> value
        }
    }

    private fun monthNumber(monthName: String): String {
        return monthNumberOrNull(monthName) ?: "01"
    }

    private fun monthNumberOrNull(monthName: String): String? {
        val months = mapOf(
            "jan" to "01",
            "feb" to "02",
            "mar" to "03",
            "apr" to "04",
            "may" to "05",
            "jun" to "06",
            "jul" to "07",
            "aug" to "08",
            "sep" to "09",
            "oct" to "10",
            "nov" to "11",
            "dec" to "12"
        )
        return months[monthName.take(3).lowercase()]
    }
}
