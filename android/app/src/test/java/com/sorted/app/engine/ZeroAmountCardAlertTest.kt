package com.sorted.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ICICI international card alerts whose amount is written `.00`, with the card's available
 * limit printed in the same message.
 *
 * These once produced the available limit as the transaction amount: the template regex
 * required a digit before the decimal point, so extraction failed, the message fell through to
 * the generic parser, and the only INR figure there was the `Avl Limit`.
 */
class ZeroAmountCardAlertTest {
    private data class Alert(val message: String, val limit: Double, val merchant: String)

    private val zeroAmountAlerts = listOf(
        Alert("USD .00 spent using ICICI Bank Card XX0000 on 27-May-26 on ANTHROPIC, PBC. Avl Limit: INR 2,76,854.28. If not you, call 1800 2662/SMS BLOCK 0000 to 9215676766.", 276854.28, "Anthropic"),
        Alert("USD .00 spent using ICICI Bank Card XX0000 on 26-Apr-26 on ANTHROPIC, PBC. Avl Limit: INR 60,586.40. If not you, call 1800 2662/SMS BLOCK 0000 to 9215676766.", 60586.40, "Anthropic"),
        Alert("USD .00 spent using ICICI Bank Card XX0000 on 15-Apr-26 on ANTHROPIC, PBC. Avl Limit: INR 4,274.10. If not you, call 1800 2662/SMS BLOCK 0000 to 9215676766.", 4274.10, "Anthropic"),
        Alert("USD .00 spent using ICICI Bank Card XX0000 on 08-Jan-26 on JETBRAINS. Avl Limit: INR 1,19,786.79. If not you, call 1800 2662/SMS BLOCK 0000 to 9215676766.", 119786.79, "Jetbrains"),
        Alert("USD .00 spent using ICICI Bank Card XX0000 on 12-Dec-25 on OPENAI, LLC. Avl Limit: INR 2,40,355.79. If not you, call 1800 2662/SMS BLOCK 0000 to 9215676766.", 240355.79, "OpenAI"),
        Alert("USD .00 spent using ICICI Bank Card XX0000 on 08-Dec-25 on JETBRAINS. Avl Limit: INR 2,54,965.92. If not you, call 1800 2662/SMS BLOCK 0000 to 9215676766.", 254965.92, "Jetbrains"),
        Alert("USD .00 spent using ICICI Bank Card XX1007 on 04-Dec-25 on METAPAY*PAYMENT. Avl Limit: INR 2,63,392.57. If not you, call 1800 2662/SMS BLOCK 1007 to 9215676766.", 263392.57, "Metapay")
    )

    @Test
    fun `available limit is never used as the transaction amount`() {
        zeroAmountAlerts.forEach { alert ->
            val parsed = SmsParser.parse(alert.message, sourceAddress = "JD-ICICIT-S")
            assertNotEquals(
                "Available limit leaked into the amount for ${alert.merchant}",
                alert.limit,
                parsed.amount ?: 0.0,
                0.001
            )
        }
    }

    @Test
    fun `zero amount card authorisations parse as zero in the charged currency`() {
        zeroAmountAlerts.forEach { alert ->
            val parsed = SmsParser.parse(alert.message, sourceAddress = "JD-ICICIT-S")
            assertEquals("amount for ${alert.merchant}", 0.0, parsed.amount ?: -1.0, 0.001)
            assertEquals("currency for ${alert.merchant}", "USD", parsed.currency)
        }
    }

    @Test
    fun `zero amount authorisations stay out of spend and surface for review`() {
        zeroAmountAlerts.forEach { alert ->
            val parsed = SmsParser.parse(alert.message, sourceAddress = "JD-ICICIT-S")

            assertTrue(
                "${alert.merchant} must not count toward Spent this month",
                !OutflowPolicy.countsTowardSpent(
                    status = parsed.status,
                    direction = parsed.direction,
                    amount = parsed.amount,
                    type = parsed.transactionType
                )
            )

            val assessment = ImportDecisionPolicy.assess(parsed)
            assertEquals(
                "${alert.merchant} should be reviewable, not silently accepted",
                ImportDecision.REVIEW,
                assessment.decision
            )
            assertEquals("amount_unclear", assessment.reason)
        }
    }

    @Test
    fun `a normally formatted card alert still parses`() {
        val parsed = SmsParser.parse(
            "USD 11.80 spent using ICICI Bank Card XX0000 on 16-Sep-26 on JETBRAINS. Avl Limit: INR 2,74,736.95. If not you, call 1800 2662/SMS BLOCK 0000 to 9215676766.",
            sourceAddress = "JD-ICICIT-S"
        )
        assertEquals(11.80, parsed.amount ?: 0.0, 0.001)
        assertEquals("USD", parsed.currency)
        assertEquals(Direction.DEBIT, parsed.direction)
    }

    @Test
    fun `a balance printed after a payment does not become the payment`() {
        // Both figures sit within each other's context window; only the label immediately
        // before each one tells them apart.
        val parsed = SmsParser.parse(
            "IndusInd A/C  Debited; INR 531.00 Ref-To ECS Return 01 Jan 2025.Bal INR 5,925.37.Dispute-Call 18602677777-IndusInd Bank",
            sourceAddress = "AD-INDUSB-S"
        )
        assertEquals(531.00, parsed.amount ?: 0.0, 0.001)
    }

    @Test
    fun `a merchant named Limited is not mistaken for a credit limit`() {
        // "limit" must match on word boundaries: "Private Limited" is a company suffix.
        val parsed = SmsParser.parse(
            "A/c *XX1990 debited by Rs 391.00 towards blusmartmobilityprivatelimited.rzp@icici. RRN: 547080448282. Not You? call 18602677777- IndusInd Bank",
            sourceAddress = "AD-INDUSB-S"
        )
        assertEquals(391.00, parsed.amount ?: 0.0, 0.001)
    }

    @Test
    fun `a template that fails extraction is not retried by the generic parser`() {
        // Recognised as an ICICI card spend, but the trailing fields are missing. The generic
        // parser must not get a chance to pick up the limit instead.
        val parsed = SmsParser.parse(
            "USD spent using ICICI Bank Card XX0000. Avl Limit: INR 99,999.00.",
            sourceAddress = "JD-ICICIT-S"
        )
        assertNotEquals(99999.00, parsed.amount ?: 0.0, 0.001)
    }

    @Test
    fun `an offer of money is not recorded as money received`() {
        val parsed = SmsParser.parse(
            "Hello Valued Airtel Customer, You are pre-qualified for Airtel Axis Bank Credit Card with Credit limit of up to Rs 500000 and annual cashback of Rs 18000. Apply today.",
            sourceAddress = "AD-AIRTEL-P"
        )
        assertEquals(false, parsed.isTransaction)
    }
}
