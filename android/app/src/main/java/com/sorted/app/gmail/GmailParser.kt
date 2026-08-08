package com.sorted.app.gmail

import com.sorted.app.engine.Categorizer
import com.sorted.app.engine.CategorySource
import com.sorted.app.engine.Direction
import com.sorted.app.engine.ParsedTransaction
import com.sorted.app.engine.ParserFacts
import com.sorted.app.engine.PaymentMode
import com.sorted.app.engine.TransactionStatus
import com.sorted.app.engine.TransactionType

object GmailParser {
    private data class MoneyAmount(
        val amount: Double,
        val currency: String
    )

    fun parse(message: GmailRawMessage): ParsedTransaction {
        val text = clean(
            listOfNotNull(message.subject, message.snippet, message.bodyText)
                .joinToString("\n")
        )

        ignoreReason(message, text)?.let { return ignored(it) }

        val money = parseMoney(text) ?: return ignored("amount_missing")
        val direction = parseDirection(text)
        if (direction == Direction.UNKNOWN) return ignored("direction_missing")

        val merchant = parseMerchant(text)
            ?: merchantFromKnownSender(message.from)
            ?: merchantFromKnownSubject(message.subject)

        val facts = ParserFacts(
            amount = money.amount,
            currency = money.currency,
            direction = direction,
            merchantRaw = merchant,
            paymentMode = parsePaymentMode(text),
            accountHint = parseAccountHint(text),
            transactionDate = parseDateFromText(text) ?: message.receivedDate,
            transactionTime = null
        )
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
            confidence = gmailConfidence(category.categorySource, facts.merchantRaw, category.confidence),
            ignoreReason = null
        )
    }

    private fun parseMoney(text: String): MoneyAmount? {
        firstAmount(
            text,
            listOf(
                Regex("""(?is)\b(?:debited|spent|deducted|paid|payment|purchase|charged|sent|transaction)[^.]{0,120}\b(?:INR|Rs\.?|\x{20B9})\s*([\d,]+(?:\.\d+)?)"""),
                Regex("""(?is)\b(?:credited|refund|reversal|cashback|received)[^.]{0,120}\b(?:INR|Rs\.?|\x{20B9})\s*([\d,]+(?:\.\d+)?)"""),
                Regex("""(?is)\b(?:INR|Rs\.?|\x{20B9})\s*([\d,]+(?:\.\d+)?)[^.]{0,120}\b(?:debited|spent|deducted|paid|payment|purchase|charged|sent|transaction)"""),
                Regex("""(?is)\b(?:INR|Rs\.?|\x{20B9})\s*([\d,]+(?:\.\d+)?)[^.]{0,120}\b(?:credited|refund|reversal|cashback|received)"""),
                Regex("""(?i)(?:\bINR\b|\bRs\.?|\x{20B9})\s*([\d,]+(?:\.\d+)?)""")
            )
        )?.let { return MoneyAmount(it, "INR") }

        firstAmount(
            text,
            listOf(
                Regex("""(?is)\bcurrency\s*:\s*USD.{0,120}\bamount\s*:\s*([\d,]+(?:[.,]\d+)?)"""),
                Regex("""(?is)\b(?:remittance|lrs|transferred|transfer|transaction|approved)[^.]{0,120}\b(?:USD|US\$)\s*([\d,]+(?:[.,]\d+)?)"""),
                Regex("""(?is)\b(?:remittance|lrs|transferred|transfer|transaction|approved)[^.]{0,120}(?<![A-Za-z])\$\s*([\d,]+(?:\.\d+)?)"""),
                Regex("""(?is)\b(?:USD|US\$)\s*([\d,]+(?:[.,]\d+)?)[^.]{0,120}\b(?:remittance|lrs|transferred|transfer|transaction|approved)"""),
                Regex("""(?i)\b(?:USD|US\$)\s*([\d,]+(?:[.,]\d+)?)"""),
                Regex("""(?i)(?<![A-Za-z])\$\s*([\d,]+(?:\.\d+)?)""")
            )
        )?.let { return MoneyAmount(it, "USD") }

        return null
    }

    private fun firstAmount(text: String, patterns: List<Regex>): Double? {
        return patterns
            .firstNotNullOfOrNull { pattern -> pattern.find(text)?.groupValues?.get(1) }
            ?.parseFlexibleAmount()
    }

    private fun parseDirection(text: String): Direction {
        val lower = text.lowercase()
        val creditWords = listOf("credited", "refund", "reversal", "cashback", "received")
        val debitWords = listOf("debited", "spent", "deducted", "paid", "payment", "purchase", "charged", "sent")
        val remittanceDebitWords = listOf(
            "lrs remittance",
            "cross-border remittance",
            "remittance request",
            "funds will be transferred to your vested account"
        )

        return when {
            creditWords.any { it in lower } -> Direction.CREDIT
            remittanceDebitWords.any { it in lower } -> Direction.DEBIT
            debitWords.any { it in lower } -> Direction.DEBIT
            else -> Direction.UNKNOWN
        }
    }

    private fun parseMerchant(text: String): String? {
        val lower = text.lowercase()
        if ("vested" in lower && ("remittance" in lower || "hdfc transaction on vested" in lower)) {
            return "Vested"
        }
        if ("team global investing" in lower || "global investing account" in lower) {
            return "Global Investing"
        }

        val patterns = listOf(
            Regex("""(?is)\btowards\s+VPA\s+\S+\s*\(([^)]+)\)"""),
            Regex("""(?is)\btowards\s+(.+?)(?:\s+on\s+\d{1,2}[-/]\d{1,2}[-/]\d{2,4}|\s+with\s+ref|\s+with\s+reference|[.;,]|$)"""),
            Regex("""(?is)\b(?:merchant|biller|paid to|payment to|spent at|at)\s*:?\s*([A-Za-z0-9][A-Za-z0-9 .&@/_-]{1,90})"""),
            Regex("""(?is)\bfrom\s+([A-Za-z0-9][A-Za-z0-9 .&@/_-]{1,90})""")
        )

        return patterns
            .firstNotNullOfOrNull { pattern -> pattern.find(text)?.groupValues?.get(1)?.cleanMerchant() }
    }

    private fun parsePaymentMode(text: String): PaymentMode {
        val lower = text.lowercase()
        return when {
            "lrs" in lower || "swift" in lower || "remittance" in lower || "cross-border" in lower -> PaymentMode.BANK_TRANSFER
            "upi mandate" in lower -> PaymentMode.UPI_MANDATE
            "upi" in lower || "vpa" in lower -> PaymentMode.UPI
            "card" in lower -> PaymentMode.CARD
            "wallet" in lower -> PaymentMode.WALLET
            "account" in lower || "a/c" in lower || "bank" in lower -> PaymentMode.BANK_TRANSFER
            else -> PaymentMode.UNKNOWN
        }
    }

    private fun parseAccountHint(text: String): String? {
        return Regex("""(?i)\b(?:a/c|account|card)(?:\s+no)?\s*(?:ending\s*)?([Xx*]*\d{3,6})""")
            .find(text)
            ?.groupValues
            ?.get(1)
    }

    private fun parseDateFromText(text: String): String? {
        Regex("""\b(\d{4})[-/](\d{1,2})[-/](\d{1,2})\b""")
            .find(text)
            ?.let { return "${it.groupValues[1]}-${it.groupValues[2].pad2()}-${it.groupValues[3].pad2()}" }

        Regex("""\b(\d{1,2})[-/](\d{1,2})[-/](\d{2,4})\b""")
            .find(text)
            ?.let {
                val year = it.groupValues[3].normalizeYear()
                return "$year-${it.groupValues[2].pad2()}-${it.groupValues[1].pad2()}"
            }

        Regex("""(?i)\b(\d{1,2})\s+([A-Za-z]{3,9})\s+(\d{2,4})\b""")
            .find(text)
            ?.let {
                val month = monthNumber(it.groupValues[2]) ?: return@let
                return "${it.groupValues[3].normalizeYear()}-$month-${it.groupValues[1].pad2()}"
            }

        return null
    }

    private fun ignoreReason(message: GmailRawMessage, text: String): String? {
        val lower = text.lowercase()
        val subject = message.subject.orEmpty().lowercase()
        val from = message.from.orEmpty().lowercase()
        val financialAction = listOf(
            "debited",
            "credited",
            "spent",
            "deducted",
            "refund",
            "payment",
            "purchase",
            "charged",
            "remittance",
            "lrs"
        ).any { it in lower }

        return when {
            "one-time password" in lower || Regex("""\botp\b""").containsMatchIn(lower) -> "otp"
            "failed" in lower || "declined" in lower || "unsuccessful" in lower -> "failed_transaction"
            ("swift copy" in subject || "swift copy" in lower) && "cross-border remittance" in lower -> "remittance_document"
            "buy order is complete" in subject || "sell order is complete" in subject -> "broker_order"
            "payment reminder" in subject || "bill payment is due" in lower || "payment is due" in lower -> "payment_reminder"
            "due date alert" in lower || "payment due today" in lower || "pay without penalty" in subject -> "payment_reminder"
            "smart statement" in subject || "bill summary" in lower || "total amount due" in lower || "minimum due" in lower -> "statement"
            "lock in your life insurance premium" in subject || ("product brochure" in lower && "term plan" in lower) -> "marketing"
            "groww digest" in subject || "all you need to know about the day" in lower -> "newsletter"
            "newsletter" in lower && !financialAction -> "newsletter"
            "annual general meeting" in lower || "integrated annual report" in lower -> "company_notice"
            "notice of the" in lower && "meeting" in lower && !financialAction -> "company_notice"
            "will be held" in lower && "meeting" in lower && !financialAction -> "company_notice"
            "recipe contest" in lower || ("contest" in subject && !financialAction) -> "marketing"
            "awake & hungry" in subject -> "marketing"
            "periodic funds settlement" in subject || ("periodic funds settlement" in lower && "groww" in from) -> "broker_notice"
            "statement" in lower && !financialAction -> "statement"
            !looksFinancial(lower) -> "unsupported"
            else -> null
        }
    }

    private fun looksFinancial(lower: String): Boolean {
        val markers = listOf(
            "inr",
            "rs.",
            "debited",
            "credited",
            "spent",
            "deducted",
            "refund",
            "payment",
            "transaction",
            "invoice",
            "receipt",
            "upi",
            "card",
            "usd",
            "$",
            "remittance",
            "lrs",
            "vested"
        )
        return markers.any { it in lower }
    }

    private fun merchantFromKnownSender(from: String?): String? {
        val lower = from?.lowercase().orEmpty()
        val bankSenders = listOf("hdfc", "icici", "sbi", "pnb", "axis", "kotak", "bank")
        if (bankSenders.any { it in lower }) return null
        return knownMerchantFromText(lower)
    }

    private fun merchantFromKnownSubject(subject: String?): String? {
        return knownMerchantFromText(subject?.lowercase().orEmpty())
    }

    private fun knownMerchantFromText(lower: String): String? {
        return when {
            "swiggy" in lower -> "Swiggy"
            "blinkit" in lower || "grofers" in lower -> "Blinkit"
            "zepto" in lower -> "Zepto"
            "zomato" in lower -> "Zomato"
            "amazon" in lower -> "Amazon"
            "myntra" in lower -> "Myntra"
            "netflix" in lower -> "Netflix"
            "spotify" in lower -> "Spotify"
            "uber" in lower -> "Uber"
            "groww" in lower -> "Groww"
            "zerodha" in lower -> "Zerodha"
            "vested" in lower -> "Vested"
            "global investing" in lower -> "Global Investing"
            "google" in lower || "youtube" in lower -> "Google"
            "openai" in lower || "chatgpt" in lower -> "OpenAI ChatGPT"
            else -> null
        }
    }

    private fun gmailConfidence(
        categorySource: CategorySource,
        merchantRaw: String?,
        categoryConfidence: Double
    ): Double {
        return when {
            categorySource == CategorySource.KNOWN_MERCHANT_RULE -> maxOf(categoryConfidence, 0.86)
            merchantRaw.isNullOrBlank() -> minOf(categoryConfidence, 0.45)
            else -> categoryConfidence
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

    private fun clean(value: String): String {
        return value
            .replace("\u202f", " ")
            .replace("\u00a0", " ")
            .replace(Regex("""[ \t\r\n]+"""), " ")
            .trim()
    }

    private fun String.cleanMerchant(): String? {
        val candidate = replace(Regex("""(?i)^VPA\s+"""), "")
            .replace(Regex("""(?i)\s+(?:on|date|using|via|through|thru|for|with|ref|reference|transaction|card|a/c|account)\b.*"""), "")
            .replace(Regex("""[.,;:]+$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(70)

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
            "help you quickly",
            "help you quickly check",
            "a recent upi",
            "transaction details",
            "attorney or tax professional",
            "torney or tax professional",
            "regarding your specific financial"
        )
        return candidate.takeIf { it.length >= 2 && blocked.none { blockedValue -> lower == blockedValue || lower.startsWith("$blockedValue ") } }
    }

    private fun String.parseFlexibleAmount(): Double? {
        val trimmed = trim()
        val normalized = if ("," in trimmed && "." !in trimmed) {
            val parts = trimmed.split(",")
            if (parts.size == 2 && parts.last().length == 2 && parts.first().length <= 3) {
                parts.first() + "." + parts.last()
            } else {
                trimmed.replace(",", "")
            }
        } else {
            trimmed.replace(",", "")
        }
        return normalized.toDoubleOrNull()
    }

    private fun String.pad2(): String = padStart(2, '0')

    private fun String.normalizeYear(): String {
        return if (length == 2) "20$this" else this
    }

    private fun monthNumber(value: String): String? {
        return when (value.take(3).lowercase()) {
            "jan" -> "01"
            "feb" -> "02"
            "mar" -> "03"
            "apr" -> "04"
            "may" -> "05"
            "jun" -> "06"
            "jul" -> "07"
            "aug" -> "08"
            "sep" -> "09"
            "oct" -> "10"
            "nov" -> "11"
            "dec" -> "12"
            else -> null
        }
    }
}
