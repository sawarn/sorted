package com.sorted.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantNormalizerTest {
    @Test
    fun `removes routing prefix upi handle and reference suffix`() {
        assertEquals(
            "Green Leaf Cafe",
            MerchantNormalizer.normalize("UPI/Green-Leaf_Cafe@okicici/Ref/5678912345")
        )
    }

    @Test
    fun `normalizes website merchant text`() {
        assertEquals("Fresh Basket", MerchantNormalizer.normalize("Fresh-Basket.in"))
    }

    @Test
    fun `does not present a url or numeric reference as a merchant`() {
        assertNull(MerchantNormalizer.normalize("https://vil.example/shop"))
        assertNull(MerchantNormalizer.normalize("123456789012"))
        assertNull(MerchantNormalizer.normalize("  "))
    }

    @Test
    fun `known merchant categorization still uses canonical merchant`() {
        val result = Categorizer.categorize(
            ParserFacts(
                amount = 320.0,
                currency = "INR",
                direction = Direction.DEBIT,
                merchantRaw = "VPA SWIGGYUPI@OKAXIS Ref 123456789012",
                paymentMode = PaymentMode.UPI,
                accountHint = null,
                transactionDate = null,
                transactionTime = null
            )
        )

        assertEquals("Swiggy", result.merchantNormalized)
        assertEquals("Food", result.departmentCategory)
    }
}
