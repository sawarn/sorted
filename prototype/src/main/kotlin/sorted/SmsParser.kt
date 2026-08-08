package sorted

object SmsParser {
    fun parse(rawMessage: String): ParsedTransaction {
        val cleaned = clean(rawMessage)

        ignoreReason(cleaned)?.let { reason ->
            return ignored(reason)
        }

        val facts = when {
            cleaned.contains("spent using ICICI Bank Card", ignoreCase = true) -> parseIciciCardSpend(cleaned)
            cleaned.contains("UPI Mandate:", ignoreCase = true) -> parseHdfcUpiMandate(cleaned)
            cleaned.startsWith("Sent Rs.", ignoreCase = true) -> parseHdfcUpiDebit(cleaned)
            cleaned.contains(" debited INR ", ignoreCase = true) && cleaned.contains(" thru UPI", ignoreCase = true) -> parsePnbUpiDebit(cleaned)
            cleaned.contains("IT Refund amount", ignoreCase = true) -> parseSbiTaxRefund(cleaned)
            cleaned.contains("Credit Alert!", ignoreCase = true) && cleaned.contains(" credited to HDFC Bank", ignoreCase = true) -> parseHdfcUpiCredit(cleaned)
            else -> null
        } ?: return ignored("unsupported")

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

    private fun ignoreReason(message: String): String? {
        val lower = message.lowercase()
        return when {
            "consent requested" in lower || "authenticate via otp" in lower -> "consent_request"
            "one-time password" in lower || Regex("""\botp\b""").containsMatchIn(lower) -> "otp"
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
        return value.replace(",", "").toDouble()
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
        return months.getValue(monthName.lowercase())
    }
}
