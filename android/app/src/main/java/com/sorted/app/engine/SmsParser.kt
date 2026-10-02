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

        val ignoredReason = ignoreReason(cleaned, sourceAddress)
        val isPending = ignoredReason == "pending"
        if (ignoredReason != null && !isPending) return ignored(ignoredReason)

        val template = parseKnownTemplate(cleaned)
        val knownFacts = (template as? TemplateMatch.Parsed)?.facts
        val facts = when (template) {
            is TemplateMatch.Parsed -> template.facts
            // The message was positively identified as this template, so a failed extraction
            // is not the same as an unknown message. Falling through to the generic parser
            // here lets it harvest an unrelated figure such as an available-balance line.
            is TemplateMatch.ExtractionFailed -> return ignored("template_extraction_failed")
            TemplateMatch.NotRecognised -> parseGenericFinancialAlert(cleaned, sourceAddress)
                ?: return ignored("unsupported")
        }

        val category = Categorizer.categorize(facts)
        return ParsedTransaction(
            isTransaction = true,
            status = if (isPending) TransactionStatus.PENDING else TransactionStatus.COMPLETED,
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
            ignoreReason = null,
            evidenceConfidence = when {
                isPending -> 0.86
                knownFacts != null -> 0.98
                // An inferred amount is only as trustworthy as the text around it. Without
                // this the generic path reported a flat 0.86 regardless of how weak its
                // evidence was, so ImportDecisionPolicy could never act on a poor score.
                else -> genericEvidenceConfidence(facts.amountScore)
            }
        )
    }

    /** Distinguishes "no template claims this message" from "a template claimed it and failed". */
    private sealed interface TemplateMatch {
        data class Parsed(val facts: ParserFacts) : TemplateMatch
        data class ExtractionFailed(val template: String) : TemplateMatch
        data object NotRecognised : TemplateMatch
    }

    private fun parseKnownTemplate(cleaned: String): TemplateMatch {
        val template: String
        val facts: ParserFacts?
        when {
            cleaned.contains("spent using ICICI Bank Card", ignoreCase = true) -> {
                template = "icici_card_spend"; facts = parseIciciCardSpend(cleaned)
            }
            cleaned.startsWith("Txn Rs.", ignoreCase = true) && cleaned.contains("HDFC Bank Card", ignoreCase = true) -> {
                template = "hdfc_card_txn"; facts = parseHdfcCardTxn(cleaned)
            }
            cleaned.contains("UPI Mandate:", ignoreCase = true) -> {
                template = "hdfc_upi_mandate"; facts = parseHdfcUpiMandate(cleaned)
            }
            cleaned.startsWith("Sent Rs.", ignoreCase = true) -> {
                template = "hdfc_upi_debit"; facts = parseHdfcUpiDebit(cleaned)
            }
            cleaned.contains(" debited INR ", ignoreCase = true) && cleaned.contains(" thru UPI", ignoreCase = true) -> {
                template = "pnb_upi_debit"; facts = parsePnbUpiDebit(cleaned)
            }
            cleaned.contains("IT Refund amount", ignoreCase = true) -> {
                template = "sbi_tax_refund"; facts = parseSbiTaxRefund(cleaned)
            }
            cleaned.contains("Credit Alert!", ignoreCase = true) && cleaned.contains(" credited to HDFC Bank", ignoreCase = true) -> {
                template = "hdfc_upi_credit"; facts = parseHdfcUpiCredit(cleaned)
            }
            cleaned.contains("PAYMENT ALERT!", ignoreCase = true) && cleaned.contains(" deducted from ", ignoreCase = true) -> {
                template = "payment_alert_deduction"; facts = parsePaymentAlertDeduction(cleaned)
            }
            cleaned.contains("deducted towards PMJJBY", ignoreCase = true) -> {
                template = "pmjjby_debit"; facts = parsePmjjbyDebit(cleaned)
            }
            else -> return TemplateMatch.NotRecognised
        }
        return facts?.let { TemplateMatch.Parsed(it) } ?: TemplateMatch.ExtractionFailed(template)
    }

    private fun parseIciciCardSpend(message: String): ParserFacts? {
        val regex = Regex(
            """([A-Z]{3})\s+((?:[\d,]+(?:\.\d+)?|\.\d+))\s+spent using ICICI Bank Card\s+(\S+)\s+on\s+(\d{2}-[A-Za-z]{3}-\d{2})\s+on\s+(.+?)(?:\.|$)""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(message) ?: return null
        return ParserFacts(
            amount = parseAmount(match.groupValues[2]),
            currency = match.groupValues[1].uppercase(),
            direction = Direction.DEBIT,
            merchantRaw = match.groupValues[5].trim(),
            paymentMode = PaymentMode.CARD,
            accountHint = match.groupValues[3],
            transactionDate = parseDate(match.groupValues[4]),
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
        if (!hasNearbyTransactionSignal(message, direction, amount.startIndex)) return null
        val paymentMode = parseGenericPaymentMode(message)
        val merchant = parseGenericMerchant(message, direction)
            ?: parseMerchantFromSourceAddress(sourceAddress)
        val accountHint = parseGenericAccountHint(message)
        val date = parseGenericDate(message)
        val time = parseGenericTime(message)

        return ParserFacts(
            amount = amount.amount,
            currency = "INR",
            direction = direction,
            merchantRaw = merchant,
            paymentMode = paymentMode,
            accountHint = accountHint,
            transactionDate = date,
            transactionTime = time,
            amountScore = amount.score
        )
    }

    /**
     * Words saying money actually moved, by direction. Single source of truth for both
     * [amountContextScore] and [hasNearbyTransactionSignal]; these previously held separate,
     * drifting copies, so a word like "transferred" counted as a signal but scored nothing.
     */
    private val MovementWords = mapOf(
        Direction.DEBIT to listOf(
            "debited", "debit", "spent", "deducted", "paid", "sent",
            "withdrawn", "withdrawal", "purchase", "charged", "transferred", "transfer"
        ),
        Direction.CREDIT to listOf(
            "credited", "credit", "received", "deposited", "refund", "refunded",
            "reversal", "reversed", "cashback", "reward", "interest"
        ),
        Direction.UNKNOWN to emptyList()
    )

    /**
     * Phrases marking a figure as something other than the transaction amount.
     *
     * Matched on word boundaries: as bare substrings "limit" hits "Limited", which appears in a
     * great many Indian merchant names, and wrongly penalised genuine payments.
     */
    private val NoisePatterns = listOf(
        "bal", "balance", "avl", "available", "limit", "outstanding",
        "minimum", "min due", "total due", "due date", "cashback offer", "reward points"
    ).map { it.toWordRegex() }

    private val MovementPatterns: Map<Direction, List<Regex>> =
        MovementWords.mapValues { (_, words) -> words.map { it.toWordRegex() } }

    private fun String.toWordRegex(): Regex =
        Regex("""\b${Regex.escape(this)}\b""", RegexOption.IGNORE_CASE)

    private fun hasNearbyTransactionSignal(message: String, direction: Direction, amountStartIndex: Int): Boolean {
        // Checked against the chosen amount, not against any amount in the message, so one
        // figure's evidence can never validate a different figure. Shares MovementRadius with
        // amountContextScore; the two previously disagreed (56 vs 105), letting the wider one
        // validate a figure the narrower one had already scored as balance noise.
        val context = message.windowAround(amountStartIndex, radius = MovementRadius)
        return MovementPatterns[direction].orEmpty().any { it.containsMatchIn(context) }
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
            "credited", "credit alert", "received", "deposited",
            "refund", "refunded", "reversal", "reversed", "cashback", "reward", "interest"
        )
        val debitWords = listOf(
            "debited", "debit alert", "spent", "deducted", "paid", "sent",
            "withdrawn", "purchase", "charged", "transferred", "dr "
        )

        val hasCredit = creditWords.any { it in lower }
        val hasDebit = debitWords.any { it in lower }
        if (!hasCredit && !hasDebit) return Direction.UNKNOWN
        if (hasCredit && !hasDebit) return Direction.CREDIT
        if (hasDebit && !hasCredit) return Direction.DEBIT

        // Both appear. Common in two-sided alerts: "your a/c X debited for Rs N on D and
        // a/c Y credited" — from the sender's POV this is a debit. Decide by which verb
        // sits closest to the possessive "your" phrase that names the user's account.
        //
        // Also handles reversal credits: "credited ... against reversal of txn" is still
        // a credit back to the user.
        if (Regex("""(?i)against (?:reversal|refund) of""").containsMatchIn(message)) return Direction.CREDIT

        val yourAcIdx = Regex("""(?i)\byour\s+(?:a/c|ac|account)""").find(message)?.range?.first
        if (yourAcIdx != null) {
            val creditIdx = Regex("""(?i)\bcredited\b""").findAll(message).map { it.range.first }.toList()
            val debitIdx = Regex("""(?i)\bdebited\b""").findAll(message).map { it.range.first }.toList()
            val nearestCredit = creditIdx.minByOrNull { kotlin.math.abs(it - yourAcIdx) } ?: Int.MAX_VALUE
            val nearestDebit = debitIdx.minByOrNull { kotlin.math.abs(it - yourAcIdx) } ?: Int.MAX_VALUE
            if (nearestDebit < nearestCredit) return Direction.DEBIT
            if (nearestCredit < nearestDebit) return Direction.CREDIT
        }

        // Fallback: debit wins when ambiguous. In practice ambiguous alerts are mostly
        // "credited to your credit card" style outbound bill payments.
        return Direction.DEBIT
    }

    private data class AmountCandidate(
        val amount: Double,
        val startIndex: Int,
        val score: Int
    )

    /**
     * Lowest context score an amount may have and still be treated as the transaction amount.
     * A figure sitting in a balance, limit or due-amount phrase scores below this, so a message
     * whose only number is such a figure yields no amount rather than the wrong one.
     */
    private const val MinimumAmountScore = 0

    private fun parseBestAmount(message: String, direction: Direction): AmountCandidate? {
        // Prefix-amount candidates ("INR 1,234.56", "Rs.100"). Strongest form of amount.
        val prefixed = Regex("""(?i)(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d+)?)""")
            .findAll(message)
            .mapNotNull { match ->
                val amount = parseAmountOrNull(match.groupValues.getOrNull(1).orEmpty())
                    ?: return@mapNotNull null
                val score = amountContextScore(message, match.range.first, direction)
                AmountCandidate(amount = amount, startIndex = match.range.first, score = score)
            }

        // Bare-amount candidates with a decimal. Some senders print "debited by 2500.00"
        // without any currency prefix. Require a decimal part so arbitrary order numbers
        // ("Refno 601216164509") cannot be mistaken for amounts. Also require a strong
        // movement word adjacent (score >= 12), so plain numbers in balance/limit windows
        // are rejected by the floor.
        val bare = Regex("""(?<![\d.])(\d{1,3}(?:,\d{2,3})*(?:\.\d{1,2})|\d{1,10}\.\d{1,2})(?!\d)""")
            .findAll(message)
            .mapNotNull { match ->
                val raw = match.groupValues[1]
                val amount = parseAmountOrNull(raw) ?: return@mapNotNull null
                // Guard against ref numbers: amounts rarely exceed 1e8 and never go below 0.
                if (amount <= 0.0 || amount >= 1e8) return@mapNotNull null
                val score = amountContextScore(message, match.range.first, direction)
                AmountCandidate(amount = amount, startIndex = match.range.first, score = score)
            }

        val candidates = (prefixed + bare).toList()
        if (candidates.isEmpty()) return null
        val best = candidates.maxWith(compareBy<AmountCandidate> { it.score }.thenBy { -it.startIndex })
        // maxWith only ranks candidates against each other. Without a floor, a lone
        // balance/limit figure wins by default however badly it scores.
        return best.takeIf { it.score >= MinimumAmountScore }
    }

    /**
     * Maps an amount's context score onto the evidence scale [ImportDecisionPolicy] gates on.
     * A strongly supported amount keeps the previous 0.86; weaker support drops below
     * [ImportDecisionPolicy.MinimumEvidenceConfidence] so the record is not imported silently.
     */
    private fun genericEvidenceConfidence(amountScore: Int?): Double = when {
        amountScore == null -> 0.86
        amountScore >= 12 -> 0.86
        amountScore >= 6 -> 0.72
        else -> 0.60
    }

    /**
     * Scores how well the text around an amount supports it being the transaction amount.
     *
     * Scoring is asymmetric because these alerts label amounts positionally: the label sits
     * immediately before its number ("Debited; INR 531.00", "Bal INR 5,925.37"). A symmetric
     * window puts both numbers inside each other's context and scores them identically, so a
     * trailing balance drags down the real payment. Noise is therefore read only from the text
     * leading up to the amount, while movement words count on either side because some formats
     * put the verb after ("INR 1,001.00 is debited from...").
     */
    private fun amountContextScore(message: String, amountStartIndex: Int, direction: Direction): Int {
        val label = message.labelWindow(amountStartIndex)
        val movementContext = message.windowAround(amountStartIndex, radius = MovementRadius)

        val movement = MovementPatterns[direction].orEmpty()
        var score = 0
        // A verb right next to the figure identifies it; one merely somewhere nearby only says
        // the message describes a payment. Without this distinction two figures in one message
        // score alike and the tie-break picks whichever comes first — which is how
        // "Rs. 825.0 is the total fare ... You paid Rs. 752.0" chose the fare over the payment.
        score += when {
            movement.any { it.containsMatchIn(label) } -> 12
            movement.any { it.containsMatchIn(movementContext) } -> 8
            else -> 0
        }
        if ("transaction" in movementContext.lowercase() || "txn" in movementContext.lowercase()) score += 4
        if (NoisePatterns.any { it.containsMatchIn(label) }) score -= 18
        return score
    }

    /**
     * The text immediately before an amount, which is where these alerts put the word naming
     * what the figure is ("Debited; INR 531.00", "Avl Bal INR 5,925.37"). Kept short and
     * preceding-only: a balance printed after the payment must not describe the payment.
     */
    private fun String.labelWindow(amountStartIndex: Int): String =
        substring((amountStartIndex - LabelRadius).coerceAtLeast(0), amountStartIndex)

    /** How far back to read the label naming an amount. */
    private const val LabelRadius = 34

    /**
     * How far to look for a word saying money moved. Wider than [LabelRadius] and symmetric,
     * because the verb is often far from the figure and may follow it
     * ("INR 230.00 (Incl. TCS as applicable) is debited from...").
     */
    private const val MovementRadius = 105

    private fun parseGenericMerchant(message: String, direction: Direction): String? {
        val searchableMessage = message.withoutSecurityTail()
        // Terms that end a merchant phrase. A merchant can run until an acting word
        // ("on 12-Jan-26", "via UPI", "ref 123") or punctuation.
        val merchantEnd =
            """(?:\s+(?:on|via|using|through|thru|ref|refno|rrn|utr|upi|txn|transaction|a/c|account|if not|not you|bal|avl|limit|dispute|block|report|dt)\b|[.,\n]|${'$'})"""

        // "To" that starts an action phrase ("To dispute call ...", "To block ...",
        // "To pay visit ...", "To avail use code ...") is not a merchant pointer.
        // Checked by negative lookahead so bare "to <merchant>" still works.
        val toNotAction = """to(?!\s+(?:dispute|block|report|call|register|pay|avail|redeem|activate|know|view|opt|change|get)\b)"""

        val patterns = when (direction) {
            Direction.CREDIT -> listOf(
                Regex("""(?is)\bfrom\s+(?:VPA\s+)?(.+?)$merchantEnd"""),
                Regex("""(?is)\bby\s+(.+?)$merchantEnd""")
            )
            Direction.DEBIT -> listOf(
                Regex("""(?is)\b(?:paid to|payment to|sent to|transferred to|trf to|towards|at|merchant|biller|beneficiary|$toNotAction)\s+(?:VPA\s+)?(.+?)$merchantEnd"""),
                Regex("""(?is)\bon\s+([A-Za-z][A-Za-z0-9 .&@/_-]{1,80})(?:\s+(?:via|using|through|thru|ref|rrn|upi|txn|transaction|if not|not you|bal|avl)\b|[.,\n]|${'$'})""")
            )
            Direction.UNKNOWN -> emptyList()
        }

        return patterns
            .firstNotNullOfOrNull { pattern -> pattern.find(searchableMessage)?.groupValues?.get(1)?.cleanMerchant() }
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
            "shop now" in lower &&
                Regex("""(?i)(?:https?://|www\.)""").containsMatchIn(lower) &&
                !Regex("""(?i)\b(?:debited|credited|spent using|deducted from|sent rs|transaction successful|payment successful)\b""").containsMatchIn(lower) -> "promotional_link"
            // An offer of money the user could receive is not money that moved. Guarded on the
            // absence of settled wording, so a real alert can never match this rule.
            listOf("pre-qualified", "prequalified", "pre-approved", "preapproved", "you are eligible for")
                .any { it in lower } &&
                !Regex("""(?i)\b(?:debited|credited|spent|deducted|withdrawn|transferred)\b""").containsMatchIn(lower) -> "promotional_offer"

            // A cashback offer printed in future-tense or capped language ("enjoy up to Rs.N
            // cashback", "get flat Rs.N cashback", "earn N cashback") is bait, not a payment.
            // The language that distinguishes a real cashback credit — "has been credited to",
            // "is credited to", "credited to your", "credited to a/c" — is explicitly excluded.
            Regex("""(?i)(?:enjoy|get|earn|avail|unlock|up to|flat)\s+(?:up to\s+)?rs\.?\s*[\d,]+(?:\.\d+)?\*?\s+cashback""").containsMatchIn(lower) &&
                !Regex("""(?i)(?:has been|is)\s+credited\s+(?:to|in|with)\s+(?:your|the|a/c)""").containsMatchIn(lower) -> "promotional_offer"

            // A "shop ..." / "use code ..." / "coupon ..." construct with a currency figure
            // but no settled wording is a promo. The literal word "credited" can appear in
            // promos like "Rs.1000 off credited to your cart" or "credited with Rs.1500 off" —
            // caught by the "off" suffix check.
            (("use code" in lower || "coupon" in lower || "shop now" in lower || "shop at" in lower ||
                "click here" in lower || "offer valid" in lower || "limited period" in lower) &&
                !Regex("""(?i)(?:debited|spent|deducted|withdrawn|transferred|has been credited|is credited by)""").containsMatchIn(lower)) -> "promotional_offer"
            Regex("""(?i)credited (?:to your cart|with (?:rs\.?\s*)?[\d,]+(?:\.\d+)?\*?\s*off)""").containsMatchIn(lower) -> "promotional_offer"
            Regex("""(?i)rs\.?\s*[\d,]+(?:\.\d+)?\s+off credited""").containsMatchIn(lower) -> "promotional_offer"

            // SBI "Enjoy Zero Processing Fee on Flexipay EMI! Simply convert your ... Trxn.
            // of Rs. N dated DDMMM into EMIs ..." is an EMI-conversion offer referencing a
            // prior transaction, not a new one.
            Regex("""(?i)flexipay emi.*?convert your.*?trxn.*?into emi""").containsMatchIn(lower) -> "promotional_offer"

            // "spend milestone of Rs. N" — a milestone target referenced to congratulate the
            // card user, not a payment. The figure is a lifetime spend goal.
            Regex("""(?i)\bspend milestone of rs""").containsMatchIn(lower) -> "promotional_offer"

            // Milestone / threshold rewards framed as "you have received N reward points"
            // typically also quote a Rs. threshold that is not a payment.
            Regex("""(?i)reward points on reaching\b""").containsMatchIn(lower) -> "promotional_offer"

            // Job offer scams: "you have received request for Salary Rs.N ... work at home".
            // Guarded on absence of settled transfer wording.
            (Regex("""(?i)received request for\s+(?:salary|job|work|data entry)""").containsMatchIn(lower) &&
                !Regex("""(?i)(?:transferred to|credited to your a/c|from a/c)""").containsMatchIn(lower)) -> "promotional_offer"

            // Credit card bill payments. Money going OUT of the user's bank account to pay
            // their own card: the card issuer records a credit, but for the user's cash flow
            // this is a transfer between their own accounts, not income or new spend.
            // Three common phrasings:
            //   - "payment of INR N towards your <bank> Credit Card"
            //   - "we have received payment of Rs N via UPI & the same has been credited to
            //      your <bank> Credit Card"
            //   - "payment of Rs N has been received on your <bank> Credit Card"
            Regex("""(?i)payment of (?:INR|Rs)\.?\s*[\d,.]+\s+towards your (?:ICICI|HDFC|SBI|Axis|Kotak|American Express|Amex|Bank)""").containsMatchIn(lower) -> "card_payment_ack"
            Regex("""(?i)we have received payment of rs\.?\s*[\d,.]+\s+via upi\s*&\s*the same has been credited to your (?:\w+ )?credit card""").containsMatchIn(lower) -> "card_payment_ack"
            Regex("""(?i)payment of (?:INR|Rs)\.?\s*[\d,.]+\s+has been received on your (?:ICICI|HDFC|SBI|Axis|Kotak|American Express|Amex|Bank)[^.]*\bcredit card""").containsMatchIn(lower) -> "card_payment_ack"
            "consent requested" in lower || "authenticate via otp" in lower -> "consent_request"
            "mandate request" in lower && "debited" !in lower && "deducted" !in lower -> "consent_request"
            listOf("transaction cancelled", "transaction canceled", "payment cancelled", "payment canceled", "transaction voided", "payment voided")
                .any { it in lower } -> "cancelled_transaction"
            "not completed" in lower || "payment failure" in lower || "failed" in lower || "declined" in lower || "unsuccessful" in lower -> "failed_transaction"
            "will be deducted" in lower || "will be debited" in lower || "will be credited" in lower || "upcoming mandate" in lower || "pre-debit" in lower -> "pending"
            "settlement worth" in lower || "settlement has been processed" in lower -> "pending"
            "sip installment" in lower || "sip instalment" in lower || "units are allotted" in lower -> "investment_allotment"
            "one-time password" in lower || Regex("""\botp\b""").containsMatchIn(lower) -> "otp"
            "statement" in lower && listOf("debited", "credited", "spent", "deducted").none { it in lower } -> "statement"
            "total amount due" in lower || "minimum amount due" in lower || "min amount due" in lower -> "bill_due"
            "due for payment" in lower || ("pay by" in lower && "ignore if paid" in lower) -> "bill_due"
            // "Last day to pay BESCOM bill of Rs 1667 ... Ignore if paid" — bill reminder with
            // a "to pay" preamble and an "Ignore if paid" footer. Guarded by "Ignore if paid"
            // so a genuine debit alert never matches.
            Regex("""(?i)\b(?:last day )?to pay\s+(?:your\s+)?[A-Za-z ]*bill\s+of\b""").containsMatchIn(lower) &&
                "ignore if paid" in lower -> "bill_due"
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
        val candidate = replace(Regex("""(?i)\s+(?:on|using|via|through|thru|for|with|ref|refno|reference|transaction|txn|upi|rrn|utr|card|a/c|account|if not|not you|bal|avl|limit|dispute|block|call|sms)\b.*"""), "")
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
        val isSecurityInstruction = "to block" in lower ||
            "sms block" in lower ||
            "fwd this sms" in lower ||
            ("block upi" in lower && Regex("""\d{6,}""").containsMatchIn(candidate))
        return candidate.takeIf {
            it.length >= 2 &&
                !isSecurityInstruction &&
                blocked.none { blockedValue -> lower == blockedValue || lower.startsWith("$blockedValue ") }
        }
    }

    private fun String.withoutSecurityTail(): String {
        return replace(
            Regex(
                """(?is)\b(?:if\s+not\s+you|if\s+not\s+u|not\s+you\??|not\s+u\??|fwd\s+this\s+sms|forward\s+this\s+sms|sms\s+block|call\s+\d{6,}).*$"""
            ),
            ""
        ).trim()
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
