package com.sorted.app.data

import com.sorted.app.engine.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryRuleMatcherTest {
    @Test
    fun `exact user correction takes precedence over a broader contains rule`() {
        val broad = rule(id = 1, pattern = "Cafe", matchType = "contains", priority = 100)
        val exact = rule(id = 2, pattern = "Green Leaf Cafe", matchType = "exact", priority = 1)

        val match = CategoryRuleMatcher.bestMatch(
            rules = listOf(broad, exact),
            merchantNormalized = "Green-Leaf Cafe",
            merchantRaw = "UPI/Green-Leaf_Cafe@okicici"
        )

        assertEquals(exact.id, match?.id)
    }

    @Test
    fun `disabled rules are ignored and priority orders same match type`() {
        val disabled = rule(id = 1, pattern = "Green Leaf", matchType = "contains", priority = 500, enabled = false)
        val lowerPriority = rule(id = 2, pattern = "Leaf", matchType = "contains", priority = 10)
        val higherPriority = rule(id = 3, pattern = "Green", matchType = "contains", priority = 20)

        val match = CategoryRuleMatcher.bestMatch(
            rules = listOf(disabled, lowerPriority, higherPriority),
            merchantNormalized = "Green Leaf Cafe",
            merchantRaw = null
        )

        assertEquals(higherPriority.id, match?.id)
    }

    @Test
    fun `similar but nonmatching merchants do not receive a rule`() {
        val savedRule = rule(id = 1, pattern = "Green Leaf Cafe", matchType = "exact", priority = 100)

        val match = CategoryRuleMatcher.bestMatch(
            rules = listOf(savedRule),
            merchantNormalized = "Green Leaf Grocery",
            merchantRaw = null
        )

        assertNull(match)
    }

    private fun rule(
        id: Long,
        pattern: String,
        matchType: String,
        priority: Int,
        enabled: Boolean = true
    ) = CategoryRuleEntity(
        id = id,
        pattern = pattern,
        matchType = matchType,
        merchantNormalized = null,
        miscCategory = "Food",
        departmentCategory = "Food",
        transactionType = TransactionType.EXPENSE,
        priority = priority,
        source = "user",
        enabled = enabled,
        createdAt = 1,
        updatedAt = 1
    )
}
