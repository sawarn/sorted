package com.sorted.app.data

import com.sorted.app.engine.MerchantNormalizer

/** Matches explicit local user rules against normalized merchant text. */
object CategoryRuleMatcher {
    fun bestMatch(
        rules: List<CategoryRuleEntity>,
        merchantNormalized: String?,
        merchantRaw: String?
    ): CategoryRuleEntity? {
        val merchantKeys = listOf(merchantNormalized, merchantRaw)
            .map(MerchantNormalizer::key)
            .filter(String::isNotBlank)
            .distinct()
        if (merchantKeys.isEmpty()) return null

        return rules.asSequence()
            .filter(CategoryRuleEntity::enabled)
            .filter { rule ->
                val patternKey = MerchantNormalizer.key(rule.pattern)
                if (patternKey.isBlank()) return@filter false
                when (rule.matchType.lowercase()) {
                    MatchExact -> merchantKeys.any { it == patternKey }
                    MatchContains -> merchantKeys.any { it.contains(patternKey) }
                    else -> false
                }
            }
            .sortedWith(
                compareByDescending<CategoryRuleEntity> {
                    if (it.matchType.equals(MatchExact, ignoreCase = true)) 1 else 0
                }
                    .thenByDescending(CategoryRuleEntity::priority)
                    .thenByDescending(CategoryRuleEntity::updatedAt)
            )
            .firstOrNull()
    }

    const val MatchExact = "exact"
    const val MatchContains = "contains"
}
