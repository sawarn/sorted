package com.sorted.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers generic parser improvements landed as a group.
 *
 * Message bodies are synthetic reproductions of the templates each defect was traced to;
 * amounts, dates, account numbers and merchant names are placeholders. Reproductions keep
 * the exact word order and surrounding punctuation that the fix depends on.
 */
class GenericParseFixesTest {
    // ---------- promotional offers that used to post as income ----------

    @Test fun `Enjoy up to Rs cashback is a promo, not income`() {
        val p = parse(
            "Hello, go super with your savings. Enjoy up to Rs.100 Cashback on debit card spends with Super Savings A/c. Apply now https://example T&C."
        )
        assertFalse(p.isTransaction)
        assertEquals("promotional_offer", p.ignoreReason)
    }

    @Test fun `Flexipay EMI conversion offer is a promo`() {
        val p = parse(
            "Enjoy Zero Processing Fee on Flexipay EMI! Simply convert your Credit Card Trxn. of Rs. 100 dated 01JAN into EMIs for 24 months"
        )
        assertFalse(p.isTransaction)
        assertEquals("promotional_offer", p.ignoreReason)
    }

    @Test fun `off credited to your cart is a promo`() {
        val p = parse(
            "Rs.100 off credited to your cart, use code SAMPLE at checkout. Offer valid only for today."
        )
        assertFalse(p.isTransaction)
    }

    @Test fun `credited with INR followed by Shop and promo code is a promo`() {
        val p = parse(
            "Congratulations! A/C: xxxxxxx0000 Credited with INR 100 | Shop Brand @ Extra 5% Prepaid OFF | Click here: https://example"
        )
        assertFalse(p.isTransaction)
    }

    @Test fun `credited to your account with 'to avail' and 'use code' is a promo`() {
        val p = parse(
            "INR 100 credited to your account. To avail use code: SAMPLE | min purchase of Rs 500 | Online exclusive"
        )
        assertFalse(p.isTransaction)
    }

    @Test fun `Spend milestone congratulatory message is not a credit`() {
        val p = parse(
            "Congratulations! You have received 100 Reward Points on reaching Spend Milestone of Rs. 100 with your Credit Card ending 0000."
        )
        assertFalse(p.isTransaction)
    }

    @Test fun `Job scam request for Salary is not a credit`() {
        val p = parse(
            "You have received request for Salary Rs100 with following details, Work at home on api.example.com/send?phone=0000 - EXAMPLE"
        )
        assertFalse(p.isTransaction)
    }

    // ---------- bill notices that used to post as debits ----------

    @Test fun `Last day to pay bill with Ignore if paid is bill_due`() {
        val p = parse(
            "Last day to pay utility bill of Rs 100.0 for 0000000000. To pay visit example.com . Ignore if paid-Bank."
        )
        assertFalse(p.isTransaction)
        assertEquals("bill_due", p.ignoreReason)
    }

    // ---------- credit card bill payments: transfer, not income ----------

    @Test fun `payment towards your Credit Card is a card_payment_ack`() {
        val p = parse(
            "Dear Customer, payment of INR 100.00 towards your Bank Credit Card XX0000 has been received through Click to Pay on 01-JAN-26."
        )
        assertFalse(p.isTransaction)
        assertEquals("card_payment_ack", p.ignoreReason)
    }

    @Test fun `UPI payment credited to your Credit Card is a card_payment_ack`() {
        val p = parse(
            "We have received payment of Rs.100.00 via UPI & the same has been credited to your Bank Credit Card. Your available limit is Rs.100.00."
        )
        assertFalse(p.isTransaction)
        assertEquals("card_payment_ack", p.ignoreReason)
    }

    @Test fun `BBPS payment received on your Credit Card is a card_payment_ack`() {
        val p = parse(
            "Payment of Rs 100.00 has been received on your Bank Credit Card XX0000 through Bharat Bill Payment System on 01-JAN-26."
        )
        assertFalse(p.isTransaction)
        assertEquals("card_payment_ack", p.ignoreReason)
    }

    // ---------- direction detection when both debited AND credited appear ----------

    @Test fun `IMPS transfer out of my account is a DEBIT, not CREDIT`() {
        // Both "debited" and "credited" appear; "your a/c X" is adjacent to "debited".
        val p = parse(
            "Dear Customer, Your a/c no. XXXXXXXX0000 is debited for Rs.100.00 on 01-01-26 and a/c XXXXXXX0000 credited (IMPS Ref no 000000000000). -Bank"
        )
        assertTrue(p.isTransaction)
        assertEquals(Direction.DEBIT, p.direction)
        assertEquals(100.0, p.amount ?: 0.0, 0.01)
    }

    @Test fun `against reversal of txn stays as CREDIT`() {
        val p = parse(
            "Dear UPI User, ur A/cX0000 credited with Rs100.00 on 01Jan26 against reversal of txn (Ref no 000000000000)"
        )
        assertTrue(p.isTransaction)
        assertEquals(Direction.CREDIT, p.direction)
    }

    // ---------- bare-amount parsing: no currency prefix ----------

    @Test fun `UPI 'debited by N on date' with no currency prefix still parses`() {
        val p = parse(
            "Dear UPI user A/C X0000 debited by 100.00 on date 01Jan26 trf to EXAMPLE PAYEE Refno 000000000000 If not u? call-0000000000"
        )
        assertTrue(p.isTransaction)
        assertEquals(Direction.DEBIT, p.direction)
        assertEquals(100.0, p.amount ?: 0.0, 0.01)
    }

    // ---------- merchant extraction: generic 'to' now rejects action phrases ----------

    @Test fun `To dispute call is not treated as a merchant pointer`() {
        val p = parse(
            "Bank Credit Card XX0000 debited for INR 100.00 on 01-Jan-26 for UPI-000000000000-EXAMPLE. To dispute call 0000000"
        )
        assertTrue(p.isTransaction)
        assertEquals(Direction.DEBIT, p.direction)
        assertEquals(100.0, p.amount ?: 0.0, 0.01)
        assertNotEquals("Dispute", p.merchantNormalized)
    }

    @Test fun `sent to Merchant Name extracts the merchant`() {
        val p = parse(
            "Rs. 100 sent from a/c xx0000 on 01-Jan-26 to Example Merchant (UPI Ref: 000000000000). Not you? Call 0000000"
        )
        assertTrue(p.isTransaction)
        assertEquals(Direction.DEBIT, p.direction)
        assertNotNull(p.merchantNormalized)
        assertTrue(p.merchantNormalized!!.contains("Example", ignoreCase = true))
    }

    @Test fun `trf to Payee strips Refno trail from merchant`() {
        val p = parse(
            "Dear UPI user A/C X0000 debited by 100.00 on date 01Jan26 trf to EXAMPLE PAYEE Refno 000000000000 If not u? call-0000000"
        )
        assertNotNull(p.merchantNormalized)
        assertFalse(p.merchantNormalized!!.contains("Refno", ignoreCase = true))
    }

    // ---------- Categorizer: 'Income' is a category, not a merchant ----------

    @Test fun `blank-merchant income keeps merchant null, not literal 'Income'`() {
        val p = parse(
            "Dear UPI User, ur A/cX0000 credited with Rs100.00 on 01Jan26 against reversal of txn (Ref no 000000000000)"
        )
        assertTrue(p.isTransaction)
        assertEquals(Direction.CREDIT, p.direction)
        assertNull(p.merchantNormalized)
        assertEquals("Income", p.departmentCategory)
    }

    private fun parse(message: String) = SmsParser.parse(message, sourceAddress = null)
}
