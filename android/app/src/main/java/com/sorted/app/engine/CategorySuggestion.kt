package com.sorted.app.engine

/** Small, local-only input contract for category suggestions. This is never transaction evidence. */
data class CategorySuggestionInput(
    val merchant: String?,
    val descriptionHint: String? = null,
    val direction: Direction = Direction.UNKNOWN,
    val paymentMode: PaymentMode = PaymentMode.UNKNOWN
)

data class SuggestedCategory(
    val miscCategory: String,
    val departmentCategory: String,
    val transactionType: TransactionType
) {
    init {
        require(miscCategory.isNotBlank()) { "A suggestion needs a user-facing category" }
        require(departmentCategory.isNotBlank()) { "A suggestion needs a category group" }
        require(transactionType != TransactionType.UNKNOWN) { "Unknown is not a category suggestion" }
    }
}

enum class CategorySuggestionSource {
    DETERMINISTIC_BASELINE,
    ON_DEVICE_MODEL,
    LOCAL_CORRECTION_EXAMPLES
}

enum class CategorySuggestionAbstention {
    NO_USABLE_TEXT,
    NO_CONFIDENT_CATEGORY,
    UNSUPPORTED_INPUT
}

sealed class CategorySuggestionResult {
    data class Suggested(
        val category: SuggestedCategory,
        val confidence: Double,
        val source: CategorySuggestionSource
    ) : CategorySuggestionResult() {
        init {
            require(confidence.isFinite() && confidence in 0.0..1.0) {
                "Suggestion confidence must be finite and between 0 and 1"
            }
        }
    }

    data class Abstained(val reason: CategorySuggestionAbstention) : CategorySuggestionResult()
}

/**
 * Suggestion providers only return a proposal. Callers must keep imports, corrections, and totals
 * authoritative and must require explicit user acceptance before applying a proposal.
 */
fun interface CategorySuggestionEngine {
    fun suggest(input: CategorySuggestionInput): CategorySuggestionResult
}

/** Wraps the current deterministic categorizer as the measurable baseline for future models. */
object RuleBasedCategorySuggestionEngine : CategorySuggestionEngine {
    override fun suggest(input: CategorySuggestionInput): CategorySuggestionResult {
        val text = listOf(input.merchant, input.descriptionHint)
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .joinToString(" ")
        if (text.isBlank() && input.paymentMode == PaymentMode.UNKNOWN) {
            return CategorySuggestionResult.Abstained(CategorySuggestionAbstention.NO_USABLE_TEXT)
        }

        val result = Categorizer.categorize(
            ParserFacts(
                amount = null,
                currency = null,
                direction = input.direction,
                merchantRaw = text.ifBlank { null },
                paymentMode = input.paymentMode,
                accountHint = null,
                transactionDate = null,
                transactionTime = null
            )
        )
        if (
            result.categorySource == CategorySource.FALLBACK ||
            result.categorySource == CategorySource.NONE ||
            result.miscCategory.isNullOrBlank() ||
            result.miscCategory.equals("Uncategorized", ignoreCase = true) ||
            result.departmentCategory.isNullOrBlank() ||
            result.transactionType == TransactionType.UNKNOWN
        ) {
            return CategorySuggestionResult.Abstained(CategorySuggestionAbstention.NO_CONFIDENT_CATEGORY)
        }

        return CategorySuggestionResult.Suggested(
            category = SuggestedCategory(
                miscCategory = result.miscCategory.orEmpty(),
                departmentCategory = result.departmentCategory.orEmpty(),
                transactionType = result.transactionType
            ),
            confidence = result.confidence,
            source = CategorySuggestionSource.DETERMINISTIC_BASELINE
        )
    }
}

/** One explicit category correction stored locally for the current app profile. */
data class LocalCategoryCorrectionExample(
    val merchant: String,
    val category: SuggestedCategory
)

/**
 * Learns exact normalized-merchant preferences from this profile's accepted corrections only.
 * It keeps no global state and abstains if a merchant has contradictory local examples.
 */
class LocalCorrectionCategorySuggestionEngine(
    examples: List<LocalCategoryCorrectionExample>
) : CategorySuggestionEngine {
    private val localExamples = examples.toList()

    override fun suggest(input: CategorySuggestionInput): CategorySuggestionResult {
        val merchantKey = MerchantNormalizer.key(input.merchant)
        if (merchantKey.isBlank()) {
            return CategorySuggestionResult.Abstained(CategorySuggestionAbstention.NO_USABLE_TEXT)
        }

        val matches = localExamples.filter { MerchantNormalizer.key(it.merchant) == merchantKey }
        if (matches.isEmpty()) {
            return CategorySuggestionResult.Abstained(CategorySuggestionAbstention.NO_CONFIDENT_CATEGORY)
        }

        val categories = matches.map { it.category }.distinct()
        if (categories.size != 1) {
            return CategorySuggestionResult.Abstained(CategorySuggestionAbstention.NO_CONFIDENT_CATEGORY)
        }

        return CategorySuggestionResult.Suggested(
            category = categories.single(),
            confidence = 0.98,
            source = CategorySuggestionSource.LOCAL_CORRECTION_EXAMPLES
        )
    }
}
