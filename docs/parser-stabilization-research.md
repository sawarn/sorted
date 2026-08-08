# Parser Stabilization Research

Date: 2026-08-08

This note captures the generic parsing direction for Sorted after testing against the current phone SMS corpus.

## Core Decision

Sorted should not try to hardcode every bank and merchant first.

The stable approach is:

1. Parse high-confidence known templates first.
2. Fall back to generic Indian financial-message patterns.
3. Categorize using merchant rules, keyword rules, payment-mode rules, and user corrections.
4. Treat merchant receipts, bill reminders, card-payment acknowledgements, and upcoming mandate notices as non-posted or duplicate-risk messages unless we later reconcile them against a missing bank/card alert.

This gives broad bank coverage without importing fake spends.

## Source Of Truth

Authoritative transaction messages:

- Bank account debit alerts.
- Credit card spend alerts.
- Debit card/POS/ATM alerts.
- UPI debit and credit alerts.
- NACH/ECS/ACH deduction alerts.
- Wallet/PPI debit alerts.
- Refund/reversal credits where money is actually credited.

Candidate messages for later reconciliation:

- Merchant receipts: "we have received your payment".
- Payment gateway settlement notices.
- Credit card bill payment acknowledgements.
- Invoices, bills due, reminders, and "pay by" messages.
- Autopay consent/pre-debit/upcoming mandate notices.

Candidate messages should not affect spend totals until we can prove there is no matching bank/card alert.

## Payment Rails To Support

The parser and categorizer should understand these rails/modes:

- UPI: P2P, P2M, QR/intent, VPA, collect, UPI mandate/autopay.
- Cards: credit card, debit card, POS, online card, tokenized/contactless card.
- Bank transfers: NEFT, IMPS, RTGS, internal transfer.
- Direct debits: NACH, ACH, ECS, UMRN-based mandates.
- Cash: ATM withdrawal, cash deposit.
- Cheque: cheque debit/credit, cheque return where applicable.
- Wallet/PPI: wallet load, wallet spend, prepaid card/PPI.
- BBPS/bill pay: utility, telecom, credit-card bill, FASTag and other biller payments.
- FASTag/NETC.
- Payment gateways/aggregators: Razorpay/RZP, PayU, BillDesk, Cashfree, Juspay, CCAvenue.
- Securities/investment flows: mutual fund SIP, broker, demat, NCL/ICC/NSE/BSE/CAMS/KFintech.

## Generic Parser Shape

The parser should extract facts, not categories:

- Amount: choose the amount closest to debit/credit/spent/deducted/paid/received/refund context; avoid balance, limit, total due, minimum due, rewards, and offer amounts.
- Direction: debit for debited/spent/deducted/paid/sent/withdrawn; credit for credited/received/refund/reversal/cashback/interest.
- Merchant/payee: prefer explicit `to`, `towards`, `at`, `paid to`, `merchant`, `biller`, `beneficiary`; for credits prefer `from` or `by`.
- Date/time: support `DD/MM/YY`, `DD-MM-YY`, `YYYY-MM-DD`, `DD-MMM-YY`, and optional time.
- Account hint: capture masked account/card endings only.
- Payment mode: infer from UPI/VPA, card, NACH/ACH/UMRN, ECS, NEFT, IMPS, RTGS, ATM, cheque, wallet/PPI, BBPS, FASTag/NETC, gateway names.

## Ignore Rules

Do not import these as transactions:

- OTPs and authentication messages.
- Failed, declined, unsuccessful, or not-completed transactions.
- Consent requests and mandate setup requests.
- Pre-debit/upcoming autopay notices.
- Bill reminders and invoices: total due, minimum due, due for payment, pay by, ignore if paid.
- Merchant receipts where the sender says they received payment.
- Payment gateway settlement notices that say money will be credited later.
- Credit card payment acknowledgements such as payment credited to card or received towards credit card.
- Statements and summaries without a posted debit/credit.

## Categorization Shape

The categorizer should run after parsing and apply:

1. User correction rules.
2. Known merchant rules.
3. Keyword rules.
4. Payment-mode rules.
5. Direction fallback.

Current merchant grouping should keep these together:

- Swiggy across UPI/card variants.
- Blinkit/Grofers.
- Zepto.
- Amazon/Amazon Pay variants.
- Groww, Mutual Funds NCL, Indian Clearing Corporation, BSE/NSE/CAMS/KFintech.
- Airtel, Rentomojo, Spotify, Netflix.
- Razorpay/RZP and other payment gateway aliases.

## Current Verification

The parser has been tested against a private local SMS corpus from a real Android device.

Public repository rule:

- Do not commit exported SMS feeds.
- Do not commit local SQLite databases.
- Do not commit exact private monthly totals.
- Keep only sanitized examples and high-level parser behavior in source control.

## Research Sources

- RBI customer-protection notification: banks must send SMS alerts and email alerts where registered for electronic banking transactions: https://www.rbi.org.in/commonman/English/scripts/Notification.aspx?Id=2623
- RBI Benchmarking India's Payment Systems: useful taxonomy for cards, ATMs, credit transfers, direct debits, e-money, digital utility payments, and payment aggregators: https://www.rbi.org.in/Scripts/PublicationReportDetails.aspx?ID=1214
- NPCI UPI OSG Q&A: UPI parties, P2P/P2M/P2PM types, UPI Autopay, PPI on UPI, RuPay credit card on UPI, UPI Lite, Tap and Pay: https://www.npcisupport.org.in/portal/en/kb/articles/upi-osg-q-a
- NPCI UPI Autopay: recurring payment use cases including bills, EMI, OTT, insurance, and mutual funds: https://www.npci.org.in/product/autopay
- RBI PPI Master Directions: wallet/PPI terminology and treatment: https://www.rbi.org.in/Scripts/NotificationUser.aspx/NotificationUser.aspx?Id=12156
- BBPS public reference for bill categories: https://www.pib.gov.in/PressReleasePage.aspx?PRID=2088182
- TRAI/UCC sender-header context: useful for understanding SMS headers, but sender headers alone should not be the source of truth: https://www.trai.gov.in/telecom/consumer-initiatives/unsolicited-commercial-communication

## Next Stabilization Work

- Add parser tests for each real template and each ignored false-positive class.
- Add a low-confidence candidate table for merchant receipts and Gmail receipts.
- Add user corrections so merchant/category edits become local rules.
- Add reconciliation so Gmail/merchant receipts can fill gaps only when no matching SMS transaction exists.
- Add a diagnostics screen showing parsed, ignored, candidate, and parser-error counts.
