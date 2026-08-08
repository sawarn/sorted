package com.sorted.app.engine

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

        keywordRule(key, facts)?.let {
            return it.copy(confidence = 0.72)
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
            key.contains("SWIGGYUPI") || key.contains("UPISWIGGY") -> rule("Swiggy", "Food Delivery", "Food", TransactionType.EXPENSE)
            key.contains("SWIGGYINSTAMART") -> rule("Swiggy Instamart", "Grocery Delivery", "Groceries", TransactionType.EXPENSE)
            key.contains("SWIGGY LTD") -> rule("Swiggy", "Food Delivery", "Food", TransactionType.EXPENSE)
            key.contains("SWIGGY LIMITED") -> rule("Swiggy", "Food Delivery", "Food", TransactionType.EXPENSE)
            key.contains("SWIGGY") -> rule("Swiggy", "Food Delivery", "Food", TransactionType.EXPENSE)
            key == "BLINKIT" -> rule("Blinkit", "Grocery Delivery", "Groceries", TransactionType.EXPENSE)
            key.contains("BLINKIT") -> rule("Blinkit", "Grocery Delivery", "Groceries", TransactionType.EXPENSE)
            key.contains("GROFERS") -> rule("Blinkit", "Grocery Delivery", "Groceries", TransactionType.EXPENSE)
            key.contains("ZEPTO") -> rule("Zepto", "Grocery Delivery", "Groceries", TransactionType.EXPENSE)
            key.contains("FRESHCO") -> rule("Freshco Hyper Bazaar", "Groceries", "Groceries", TransactionType.EXPENSE)
            key.contains("ESTAA SWEETS") -> rule("Estaa Sweets", "Sweets", "Food", TransactionType.EXPENSE)
            key.contains("GUTMATTER") -> rule("Gutmatter", "Health", "Health", TransactionType.EXPENSE)
            key.contains("ZOMATO") -> rule("Zomato", "Food Delivery", "Food", TransactionType.EXPENSE)
            key.contains("ETERNAL LIMITED") -> rule("Zomato", "Food Delivery", "Food", TransactionType.EXPENSE)
            key.contains("PVR") -> rule("PVR", "Movies", "Entertainment", TransactionType.EXPENSE)
            key.contains("BOOKMYSHOW") -> rule("BookMyShow", "Movies/Events", "Entertainment", TransactionType.EXPENSE)
            key.contains("INDIAN COFFEE") -> rule("Indian Coffee House", "Restaurants", "Food", TransactionType.EXPENSE)
            key.contains("EMPIRE") -> rule("Empire Restaurant", "Restaurants", "Food", TransactionType.EXPENSE)
            key.contains("SUBKO") -> rule("Subko Coffee", "Restaurants", "Food", TransactionType.EXPENSE)
            key.contains("TIM HORTONS") -> rule("Tim Hortons", "Restaurants", "Food", TransactionType.EXPENSE)
            key.contains("NEXUS DAIRY") -> rule("Nexus Dairy", "Dairy", "Groceries", TransactionType.EXPENSE)
            key.contains("NATURES BASKET") -> rule("Natures Basket", "Groceries", "Groceries", TransactionType.EXPENSE)
            key.contains("SAI FILLING") -> rule("Sai Filling Station", "Fuel", "Fuel", TransactionType.EXPENSE)
            key.contains("OPENAI") || key.contains("CHATGPT") -> rule("OpenAI ChatGPT", "Software Subscription", "Subscriptions", TransactionType.SUBSCRIPTION)
            key.contains("UBER") -> rule("Uber", "Cab", "Transport", TransactionType.EXPENSE)
            key.contains("AIRTEL") -> rule("Airtel", "Mobile/Internet Bill", "Utilities", TransactionType.EXPENSE)
            key.contains("URBANCOMPANY") || key.contains("URBAN COMPANY") -> rule("Urban Company", "Home Services", "Utilities", TransactionType.EXPENSE)
            key == "NETFLIX" -> rule("Netflix", "OTT Subscription", "Subscriptions", TransactionType.SUBSCRIPTION)
            key.contains("NETFLIX") -> rule("Netflix", "OTT Subscription", "Subscriptions", TransactionType.SUBSCRIPTION)
            key.contains("SPOTIFY") -> rule("Spotify", "Music Subscription", "Subscriptions", TransactionType.SUBSCRIPTION)
            key.contains("PRIME VIDEO") -> rule("Prime Video", "OTT Subscription", "Subscriptions", TransactionType.SUBSCRIPTION)
            key.contains("YOUTUBE") -> rule("YouTube", "Video Subscription", "Subscriptions", TransactionType.SUBSCRIPTION)
            key.contains("GOOGLE IRELAND") -> rule("Google", "Digital Subscription", "Subscriptions", TransactionType.SUBSCRIPTION)
            key == "AMAZON PAY BALANCE" -> rule("Amazon Pay Balance", "Wallet Load", "Transfer", TransactionType.TRANSFER)
            key.contains("AMAZON PAY WALL") -> rule("Amazon Pay Wallet", "Wallet Load", "Transfer", TransactionType.TRANSFER)
            key.contains("AMAZON INDIA") -> rule("Amazon", "Online Shopping", "Shopping", TransactionType.EXPENSE)
            key.contains("AMAZON PAY IN") -> rule("Amazon", "Online Shopping", "Shopping", TransactionType.EXPENSE)
            key.startsWith("AMAZON@") -> rule("Amazon", "Online Shopping", "Shopping", TransactionType.EXPENSE)
            key.contains("AMAZONUPI") -> rule("Amazon", "Online Shopping", "Shopping", TransactionType.EXPENSE)
            key.contains("AMAZON") -> rule("Amazon", "Online Shopping", "Shopping", TransactionType.EXPENSE)
            key == "AMAZON PAY" -> rule("Amazon Pay", "Online Shopping", "Shopping", TransactionType.EXPENSE)
            key.contains("MYNTRA") -> rule("Myntra", "Fashion", "Shopping", TransactionType.EXPENSE)
            key.contains("RENTOMOJO") -> rule("Rentomojo", "Rental", "Utilities", TransactionType.EXPENSE)
            key == "MUTUAL FUNDS NCL" -> rule("Mutual Funds NCL", "Mutual Fund", "Investment", TransactionType.INVESTMENT)
            key.contains("INDIAN CLEARING CORPORATION") -> rule("Indian Clearing Corporation", "Mutual Fund", "Investment", TransactionType.INVESTMENT)
            key.contains("BSE STAR") -> rule("BSE Star MF", "Mutual Fund", "Investment", TransactionType.INVESTMENT)
            key.contains("NSE CLEARING") -> rule("NSE Clearing", "Mutual Fund", "Investment", TransactionType.INVESTMENT)
            key.contains("CAMS") -> rule("CAMS", "Mutual Fund", "Investment", TransactionType.INVESTMENT)
            key.contains("KFIN") -> rule("KFintech", "Mutual Fund", "Investment", TransactionType.INVESTMENT)
            key.contains("GROWW") -> rule("Groww", "Investment", "Investment", TransactionType.INVESTMENT)
            key.contains("ZERODHA") -> rule("Zerodha", "Investment", "Investment", TransactionType.INVESTMENT)
            key.contains("VESTED") -> rule("Vested", "US Investment", "Investment", TransactionType.INVESTMENT)
            key.contains("GLOBAL INVESTING") -> rule("Global Investing", "US Investment", "Investment", TransactionType.INVESTMENT)
            key.contains("UPSTOX") -> rule("Upstox", "Investment", "Investment", TransactionType.INVESTMENT)
            key.contains("ANGEL ONE") || key.contains("ANGELONE") -> rule("Angel One", "Investment", "Investment", TransactionType.INVESTMENT)
            key == "INCOME TAX REFUND" -> rule("Income Tax Refund", "Tax Refund", "Refund", TransactionType.REFUND)
            key.contains("POPCLUBPAYOUTS") -> rule("Popclub Payouts", "Cashback/Reward", "Reward", TransactionType.REWARD)
            key.contains("MERCHANTPAYOUTS") -> rule("Merchant Payouts", "Cashback/Reward", "Reward", TransactionType.REWARD)
            key.contains("CRED.TELECOM") -> rule("CRED Telecom", "Utility Bill", "Utilities", TransactionType.EXPENSE)
            key.contains("CRED") -> rule("CRED", "Credit Card Bill", "Utilities", TransactionType.EXPENSE)
            key.contains("BBPS") || key.contains("BHARAT BILL") -> rule("Bharat BillPay", "Bill Payment", "Utilities", TransactionType.EXPENSE)
            key.contains("BILLDESK") -> rule("BillDesk", "Payment Gateway", "Other", TransactionType.EXPENSE)
            key.contains("RAZORPAY") || key.contains("RZP") -> rule("Razorpay", "Payment Gateway", "Other", TransactionType.EXPENSE)
            key.contains("PAYU") -> rule("PayU", "Payment Gateway", "Other", TransactionType.EXPENSE)
            key.contains("CASHFREE") -> rule("Cashfree", "Payment Gateway", "Other", TransactionType.EXPENSE)
            key.contains("JUSPAY") -> rule("Juspay", "Payment Gateway", "Other", TransactionType.EXPENSE)
            key.contains("CCAVENUE") -> rule("CCAvenue", "Payment Gateway", "Other", TransactionType.EXPENSE)
            key.contains("PMJJBY") -> rule("PMJJBY", "Insurance Premium", "Health", TransactionType.EXPENSE)
            else -> null
        }
    }

    private fun keywordRule(key: String, facts: ParserFacts): CategoryResult? {
        if (key.isBlank()) {
            return when {
                facts.direction == Direction.CREDIT -> generic("Income", "Income", "Income", TransactionType.INCOME)
                facts.paymentMode == PaymentMode.ATM -> generic("ATM Withdrawal", "Cash Withdrawal", "Cash", TransactionType.EXPENSE)
                facts.paymentMode == PaymentMode.NACH || facts.paymentMode == PaymentMode.ECS -> generic("Auto Debit", "Auto Debit", "Other", TransactionType.EXPENSE)
                else -> null
            }
        }

        return when {
            containsAny(key, "MUTUAL FUND", "MF ", "SIP", "NACH MANDATE", "BROKING", "DEMAT", "TRADING", "NPS") ->
                generic(titleCasePreservingNcl(key), "Investment", "Investment", TransactionType.INVESTMENT)

            containsAny(key, "INSURANCE", "POLICY", "PREMIUM", "LIC ", "HDFC LIFE", "ICICI PRU", "MAX LIFE", "TATA AIA") ->
                generic(normalizeUnknownMerchant(key), "Insurance Premium", "Insurance", TransactionType.EXPENSE)

            containsAny(key, "ELECTRICITY", "BESCOM", "ADANI ELECTRICITY", "TATA POWER", "MSEB", "MAHAVITARAN", "WATER", "GAS", "IGL", "MAHANAGAR GAS") ->
                generic(normalizeUnknownMerchant(key), "Utility Bill", "Utilities", TransactionType.EXPENSE)

            containsAny(key, "AIRTEL", "JIO", "VI ", "VODAFONE", "BSNL", "BROADBAND", "FIBER", "DTH", "TATA PLAY", "SUN DIRECT") ->
                generic(normalizeUnknownMerchant(key), "Telecom/DTH", "Utilities", TransactionType.EXPENSE)

            containsAny(key, "NETFLIX", "SPOTIFY", "HOTSTAR", "DISNEY", "YOUTUBE", "PRIME VIDEO", "APPLE.COM", "GOOGLE", "MICROSOFT", "OPENAI", "CHATGPT", "SAAS", "SUBSCRIPTION") ->
                generic(normalizeUnknownMerchant(key), "Subscription", "Subscriptions", TransactionType.SUBSCRIPTION)

            containsAny(key, "SWIGGY", "ZOMATO", "RESTAURANT", "CAFE", "COFFEE", "FOOD", "HOTEL", "KITCHEN", "BAKERY", "SWEETS") ->
                generic(normalizeUnknownMerchant(key), "Food", "Food", TransactionType.EXPENSE)

            containsAny(key, "GROCERY", "GROCERIES", "MART", "SUPERMARKET", "HYPER", "FRESH", "MILK", "DAIRY", "BASKET", "BIGBASKET", "DMART") ->
                generic(normalizeUnknownMerchant(key), "Groceries", "Groceries", TransactionType.EXPENSE)

            containsAny(key, "UBER", "OLA", "RAPIDO", "METRO", "IRCTC", "RAILWAY", "AIRLINES", "FLIGHT", "MAKEMYTRIP", "GOIBIBO", "CLEARTRIP", "RED BUS", "REDBUS") ->
                generic(normalizeUnknownMerchant(key), "Travel/Transport", "Transport", TransactionType.EXPENSE)

            containsAny(key, "PETROL", "DIESEL", "FUEL", "FILLING STATION", "IOCL", "BPCL", "HPCL") ->
                generic(normalizeUnknownMerchant(key), "Fuel", "Fuel", TransactionType.EXPENSE)

            containsAny(key, "APOLLO", "PHARMACY", "MEDICAL", "HOSPITAL", "CLINIC", "DIAGNOSTIC", "LABS", "HEALTH") ->
                generic(normalizeUnknownMerchant(key), "Health", "Health", TransactionType.EXPENSE)

            containsAny(key, "PVR", "INOX", "CINEMA", "MOVIE", "BOOKMYSHOW", "GAMES", "ENTERTAINMENT") ->
                generic(normalizeUnknownMerchant(key), "Entertainment", "Entertainment", TransactionType.EXPENSE)

            containsAny(key, "AMAZON", "FLIPKART", "MYNTRA", "AJIO", "NYKAA", "SHOP", "STORE", "RETAIL", "FASHION") ->
                generic(normalizeUnknownMerchant(key), "Shopping", "Shopping", TransactionType.EXPENSE)

            containsAny(key, "RENT", "RENTOMOJO", "MAINTENANCE", "SOCIETY", "HOUSING") ->
                generic(normalizeUnknownMerchant(key), "Housing", "Rent/Home", TransactionType.EXPENSE)

            containsAny(key, "SCHOOL", "COLLEGE", "UNIVERSITY", "TUITION", "EDUCATION", "COURSE") ->
                generic(normalizeUnknownMerchant(key), "Education", "Education", TransactionType.EXPENSE)

            containsAny(key, "LOAN", "EMI", "LENDING", "NBFC", "FINANCE") ->
                generic(normalizeUnknownMerchant(key), "Loan/EMI", "Loans", TransactionType.EXPENSE)

            containsAny(key, "TAX", "GST", "INCOME TAX", "MUNICIPAL") ->
                generic(normalizeUnknownMerchant(key), "Tax", "Taxes", TransactionType.EXPENSE)

            containsAny(key, "CHARGE", "CHARGES", "FEE", "FEES", "PENALTY") ->
                generic(normalizeUnknownMerchant(key), "Bank Charges", "Fees", TransactionType.EXPENSE)

            else -> null
        }
    }

    private fun generic(
        merchant: String?,
        misc: String,
        department: String,
        type: TransactionType
    ): CategoryResult {
        return CategoryResult(
            merchantNormalized = merchant,
            miscCategory = misc,
            departmentCategory = department,
            transactionType = type,
            categorySource = CategorySource.KEYWORD_RULE,
            confidence = 0.72
        )
    }

    private fun containsAny(value: String, vararg needles: String): Boolean {
        return needles.any { it in value }
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
        val merchantWords = listOf(
            "BANK",
            "BAZAAR",
            "DAIRY",
            "FUND",
            "HYPER",
            "INDIA",
            "INVEST",
            "MARKET",
            "MART",
            "PAY",
            "PAYOUT",
            "PETROL",
            "PRIVATE",
            "REFUND",
            "RESTAURANT",
            "STORE",
            "SWEETS",
            "TECH"
        )
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
