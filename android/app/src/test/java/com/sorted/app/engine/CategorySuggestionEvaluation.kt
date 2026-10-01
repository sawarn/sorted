package com.sorted.app.engine

data class CategorySuggestionExample(
    val id: String,
    val input: CategorySuggestionInput,
    val expected: SuggestedCategory
)

data class CategoryClassScore(
    val total: Int,
    val covered: Int,
    val correct: Int
) {
    val coverage: Double get() = covered.toDouble() / total.coerceAtLeast(1)
    val precision: Double? get() = if (covered == 0) null else correct.toDouble() / covered
}

data class CategorySuggestionEvaluation(
    val total: Int,
    val covered: Int,
    val correct: Int,
    val incorrect: Int,
    val abstained: Int,
    val perExpectedDepartment: Map<String, CategoryClassScore>,
    val perPredictedDepartment: Map<String, CategoryClassScore>
) {
    val coverage: Double get() = covered.toDouble() / total.coerceAtLeast(1)
    val precisionWhenSuggested: Double? get() = if (covered == 0) null else correct.toDouble() / covered
}

/** Test-only evaluator: keeps fixture examples and model scores out of user transaction storage. */
object CategorySuggestionEvaluator {
    fun evaluate(
        examples: List<CategorySuggestionExample>,
        engine: CategorySuggestionEngine
    ): CategorySuggestionEvaluation {
        val evaluated = examples.map { example ->
            val result = engine.suggest(example.input)
            val proposal = (result as? CategorySuggestionResult.Suggested)?.category
            EvaluatedSuggestion(
                expected = example.expected,
                proposed = proposal,
                correct = proposal?.matches(example.expected) == true
            )
        }
        val covered = evaluated.filter { it.proposed != null }
        return CategorySuggestionEvaluation(
            total = evaluated.size,
            covered = covered.size,
            correct = covered.count { it.correct },
            incorrect = covered.count { !it.correct },
            abstained = evaluated.count { it.proposed == null },
            perExpectedDepartment = evaluated.groupBy { it.expected.departmentCategory }
                .mapValues { (_, group) -> group.toClassScore() },
            perPredictedDepartment = covered.groupBy { it.proposed!!.departmentCategory }
                .mapValues { (_, group) -> group.toClassScore() }
        )
    }

    private fun List<EvaluatedSuggestion>.toClassScore() = CategoryClassScore(
        total = size,
        covered = count { it.proposed != null },
        correct = count { it.proposed != null && it.correct }
    )

    private fun SuggestedCategory.matches(expected: SuggestedCategory): Boolean =
        miscCategory.equals(expected.miscCategory, ignoreCase = true) &&
            departmentCategory.equals(expected.departmentCategory, ignoreCase = true) &&
            transactionType == expected.transactionType

    private data class EvaluatedSuggestion(
        val expected: SuggestedCategory,
        val proposed: SuggestedCategory?,
        val correct: Boolean
    )
}

/** Synthetic merchant/context examples only; contains no transaction history or personal data. */
object CategorySuggestionFixtures {
    val examples = listOf(
        example("food-cafe", "Green Leaf Cafe", "", "Food", "Food", TransactionType.EXPENSE),
        example("food-delivery", "Swiggy", "", "Food Delivery", "Food", TransactionType.EXPENSE),
        example("groceries", "Blinkit", "", "Grocery Delivery", "Groceries", TransactionType.EXPENSE),
        example("utilities", "Sample Electricity Board", "", "Utility Bill", "Utilities", TransactionType.EXPENSE),
        example("transport", "Uber", "", "Cab", "Transport", TransactionType.EXPENSE),
        example("subscription", "Netflix", "", "OTT Subscription", "Subscriptions", TransactionType.SUBSCRIPTION),
        example("investment-groww", "Groww", "", "Investment", "Investment", TransactionType.INVESTMENT),
        example("investment-nse", "NSE Clearing", "", "Mutual Fund", "Investment", TransactionType.INVESTMENT),
        example("transfer-self", "UPI transfer to own account", "", "Self Transfer", "Self Transfer", TransactionType.TRANSFER),
        example("transfer-card-bill", "Credit Card Bill Payment", "", "Credit Card Bill", "Transfer", TransactionType.TRANSFER),
        example("shopping", "Amazon India", "", "Online Shopping", "Shopping", TransactionType.EXPENSE),
        example("same-merchant-food", "Merchant Echo", "restaurant payment", "Food", "Food", TransactionType.EXPENSE),
        example("same-merchant-health", "Merchant Echo", "pharmacy purchase", "Health", "Health", TransactionType.EXPENSE),
        example("unknown", "Unfamiliar Merchant 482", "", "Other", "Other", TransactionType.EXPENSE)
    )

    private fun example(
        id: String,
        merchant: String,
        context: String,
        misc: String,
        department: String,
        type: TransactionType
    ) = CategorySuggestionExample(
        id = id,
        input = CategorySuggestionInput(
            merchant = merchant,
            descriptionHint = context.takeIf(String::isNotBlank),
            direction = Direction.DEBIT
        ),
        expected = SuggestedCategory(misc, department, type)
    )
}
