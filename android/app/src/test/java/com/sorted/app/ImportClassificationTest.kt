package com.sorted.app

import com.sorted.app.engine.Direction
import com.sorted.app.engine.ImportDecision
import com.sorted.app.engine.ImportDecisionPolicy
import com.sorted.app.engine.OutflowPolicy
import com.sorted.app.engine.SmsParser
import com.sorted.app.engine.TransactionStatus
import com.sorted.app.gmail.GmailParser
import com.sorted.app.gmail.GmailRawMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportClassificationTest {
    @Test
    fun `sms shop link without completed payment evidence is ignored`() {
        val parsed = SmsParser.parse(
            "Shop now https://vil.example/offer and get Rs 500 off on your next UPI payment",
            sourceAddress = "AD-OFFER"
        )

        assertFalse(parsed.isTransaction)
    }

    @Test
    fun `informational aeps cash withdrawal message is ignored`() {
        val parsed = SmsParser.parse(
            "AEPS cash withdrawal service is available at your nearest banking correspondent today"
        )

        assertFalse(parsed.isTransaction)
    }

    @Test
    fun `completed aeps debit with amount remains a transaction`() {
        val parsed = SmsParser.parse(
            "Rs. 500 debited from your account for AEPS cash withdrawal. Ref No 123456"
        )

        assertTrue(parsed.isTransaction)
        assertEquals(Direction.DEBIT, parsed.direction)
        assertEquals(500.0, parsed.amount!!, 0.0)
    }

    @Test
    fun `gmail shop link with payment offer is not treated as a transaction`() {
        val parsed = GmailParser.parse(
            GmailRawMessage(
                id = "fixture-promo-1",
                threadId = null,
                internalDateMillis = null,
                receivedDate = "2026-10-01",
                from = "offers@hdfcbank.com",
                subject = "Shop now",
                snippet = "Shop now https://vil.example/offer Rs. 500 off on your next payment",
                bodyText = "Shop now https://vil.example/offer Rs. 500 off on your next payment"
            )
        )

        assertFalse(parsed.isTransaction)
        assertEquals("promotional_link", parsed.ignoreReason)
    }

    @Test
    fun `gmail completed debit remains a transaction`() {
        val parsed = GmailParser.parse(
            GmailRawMessage(
                id = "fixture-debit-1",
                threadId = null,
                internalDateMillis = null,
                receivedDate = "2026-10-01",
                from = "alerts@hdfcbank.com",
                subject = "Payment successful",
                snippet = "Rs. 500 debited from your account for Amazon purchase. Ref 123456",
                bodyText = "Rs. 500 debited from your account for Amazon purchase. Ref 123456"
            )
        )

        assertTrue(parsed.isTransaction)
        assertEquals(Direction.DEBIT, parsed.direction)
        assertEquals(500.0, parsed.amount!!, 0.0)
    }

    @Test
    fun `gmail completed debit from unknown merchant is retained for category review`() {
        val parsed = GmailParser.parse(
            GmailRawMessage(
                id = "fixture-unknown-merchant-1",
                threadId = null,
                internalDateMillis = null,
                receivedDate = "2026-10-01",
                from = "alerts@hdfcbank.com",
                subject = "Debit alert",
                snippet = "Rs. 500 debited from your account for Green Leaf Cafe. Ref 123456",
                bodyText = "Rs. 500 debited from your account for Green Leaf Cafe. Ref 123456"
            )
        )

        assertTrue(parsed.isTransaction)
        assertTrue(parsed.evidenceConfidence >= ImportDecisionPolicy.MinimumEvidenceConfidence)
        assertEquals(ImportDecision.REVIEW, ImportDecisionPolicy.assess(parsed).decision)
    }

    @Test
    fun `payment wording without completed movement evidence is ignored`() {
        val parsed = GmailParser.parse(
            GmailRawMessage(
                id = "fixture-weak-payment-1",
                threadId = null,
                internalDateMillis = null,
                receivedDate = "2026-10-01",
                from = "alerts@hdfcbank.com",
                subject = "Your payment options",
                snippet = "Payment of Rs. 500 is available with your new offers",
                bodyText = "Payment of Rs. 500 is available with your new offers"
            )
        )

        assertTrue(parsed.isTransaction)
        assertEquals(ImportDecision.IGNORE, ImportDecisionPolicy.assess(parsed).decision)
    }

    @Test
    fun `future debit notification is not imported as a completed payment`() {
        val parsed = GmailParser.parse(
            GmailRawMessage(
                id = "fixture-pending-1",
                threadId = null,
                internalDateMillis = null,
                receivedDate = "2026-10-01",
                from = "alerts@hdfcbank.com",
                subject = "Upcoming debit",
                snippet = "Your account will be debited INR 500 tomorrow",
                bodyText = "Your account will be debited INR 500 tomorrow"
            )
        )

        assertTrue(parsed.isTransaction)
        assertEquals(TransactionStatus.PENDING, parsed.status)
        assertEquals(ImportDecision.REVIEW, ImportDecisionPolicy.assess(parsed).decision)
        assertFalse(
            OutflowPolicy.countsTowardSpent(
                parsed.status,
                parsed.direction,
                parsed.amount,
                parsed.transactionType
            )
        )
    }

    @Test
    fun `pending sms debit remains reviewable and outside spend total`() {
        val parsed = SmsParser.parse("Your account will be debited INR 500 tomorrow")

        assertTrue(parsed.isTransaction)
        assertEquals(TransactionStatus.PENDING, parsed.status)
        assertEquals(ImportDecision.REVIEW, ImportDecisionPolicy.assess(parsed).decision)
        assertFalse(
            OutflowPolicy.countsTowardSpent(
                parsed.status,
                parsed.direction,
                parsed.amount,
                parsed.transactionType
            )
        )
    }
}
