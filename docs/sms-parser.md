# SMS Parser

The SMS Parser is the first core component of Sorted.

Its job is to read transaction SMS messages and convert them into clean transaction records.

## Goal

Take messy bank, UPI, card, and wallet messages and extract:

- Amount
- Debit or credit
- Merchant or payee
- Date and time
- Payment mode
- Account or card hint
- Transaction type
- Category

## Input

The parser input is a raw SMS message.

Example:

```text
Rs.450.00 debited from A/c XX1234 via UPI to SWIGGY on 08-Aug-26.
```

## Output

The parser should return a structured transaction.

Example:

```json
{
  "amount": 450,
  "direction": "debit",
  "merchantRaw": "SWIGGY",
  "merchantNormalized": "Swiggy",
  "paymentMode": "UPI",
  "accountHint": "XX1234",
  "transactionType": "expense",
  "departmentCategory": "Food",
  "source": "sms"
}
```

## Message Samples Needed

To build the parser well, we need real examples of different transaction SMS formats.

Useful sample types:

- UPI debit
- UPI credit
- Card spend
- ATM withdrawal
- Refund
- Salary or income credit
- Bank charge
- Wallet payment
- Bill payment
- Failed transaction
- Reversal
- Balance-only message
- OTP message
- Promotional bank message

The parser should detect transaction messages and ignore messages that are not actual transactions.

## How To Share Samples

Share copied SMS text, but remove or mask sensitive details.

Mask:

- Full account numbers
- Full card numbers
- UPI IDs if personal
- Reference numbers
- Phone numbers
- Names
- Exact balances, if uncomfortable

Safe example:

```text
Rs.1,250.00 debited from A/c XX1234 through UPI to ZOMATO on 08-Aug-26. Ref no XXXXX.
```

Avoid sharing:

```text
Rs.1,250.00 debited from A/c <full-account-number> through UPI to <personal-upi-id> on 08-Aug-26. Ref no <reference-number>.
```

## Sample Format For Documentation

For each sample, we should capture:

```text
Raw SMS:

Expected Output:
- Is transaction: yes/no
- Amount:
- Direction:
- Merchant/payee:
- Payment mode:
- Department category:
- Notes:
```

## Parser Responsibilities

The parser should:

- Identify whether the SMS is a transaction
- Ignore OTPs and promotional messages
- Extract the transaction amount
- Detect debit or credit
- Find the merchant or payee
- Detect payment mode
- Extract account/card hint if available
- Normalize merchant names
- Assign a first-pass category
- Keep the raw SMS reference locally for debugging and correction

## Early Approach

Start with rules and patterns.

This is better for the first version because:

- SMS formats are repetitive
- Rules are explainable
- Everything can run locally
- Corrections are easier to apply

Later, we can consider a lightweight on-device model if rules are not enough.

## Initial Sample Messages

These are early anonymized examples to guide parser behavior.

### Sample 1: ICICI Card Spend

Raw SMS:

```text
INR 606.00 spent using ICICI Bank Card XX1234 on 07-Aug-26 on Blinkit. Avl Limit: INR XX,XXX.XX. If not you, call XXXXX/SMS BLOCK XXXX to XXXXX.
```

Expected Output:

- Is transaction: yes
- Amount: 606.00
- Direction: debit
- Merchant/payee: Blinkit
- Payment mode: card
- Account/card hint: XX1234
- Department category: Groceries
- Notes: Ignore available limit and block instructions.

### Sample 2: HDFC UPI Debit To Swiggy

Raw SMS:

```text
Sent Rs.350.00
From HDFC Bank A/C *1234
To Swiggy
On 07/08/26
Ref XXXXX
Not You?
Call XXXXX/SMS BLOCK UPI to XXXXX
```

Expected Output:

- Is transaction: yes
- Amount: 350.00
- Direction: debit
- Merchant/payee: Swiggy
- Payment mode: UPI
- Account/card hint: *1234
- Department category: Food
- Notes: HDFC "Sent" format should be treated as debit.

### Sample 3: HDFC UPI Debit To Amazon Pay Balance

Raw SMS:

```text
Sent Rs.942.77
From HDFC Bank A/C *1234
To Amazon Pay Balance
On 18/06/26
Ref XXXXX
Not You?
Call XXXXX/SMS BLOCK UPI to XXXXX
```

Expected Output:

- Is transaction: yes
- Amount: 942.77
- Direction: debit
- Merchant/payee: Amazon Pay Balance
- Payment mode: UPI
- Account/card hint: *1234
- Department category: Wallet/Transfer
- Notes: This may be a wallet load, not direct shopping. Category may need user correction.

### Sample 4: HDFC UPI Debit To SWIGGY

Raw SMS:

```text
Sent Rs.379.00
From HDFC Bank A/C *1234
To SWIGGY
On 07/08/26
Ref XXXXX
Not You?
Call XXXXX/SMS BLOCK UPI to XXXXX
```

Expected Output:

- Is transaction: yes
- Amount: 379.00
- Direction: debit
- Merchant/payee: Swiggy
- Payment mode: UPI
- Account/card hint: *1234
- Department category: Food
- Notes: Merchant normalizer should convert uppercase `SWIGGY` to `Swiggy`.

### Sample 5: HDFC UPI Debit To Mutual Funds NCL

Raw SMS:

```text
Sent Rs.25000.00
From HDFC Bank A/C *1234
To MUTUAL FUNDS NCL
On 07/08/26
Ref XXXXX
Not You?
Call XXXXX/SMS BLOCK UPI to XXXXX
```

Expected Output:

- Is transaction: yes
- Amount: 25000.00
- Direction: debit
- Merchant/payee: Mutual Funds NCL
- Payment mode: UPI
- Account/card hint: *1234
- Department category: Investment
- Notes: Should not be categorized as normal expense.

### Sample 6: PNB UPI Debit To Person

Raw SMS:

```text
A/c X4321 debited INR 1.00 Dt 01-07-26 13:41:26 to PERSON NAME thru UPI:XXXXXXXXXXXX.Bal INR XXXX.XX Not u?Fwd this SMS to XXXXX to block UPI.-PNB
```

Expected Output:

- Is transaction: yes
- Amount: 1.00
- Direction: debit
- Merchant/payee: Person Name
- Payment mode: UPI
- Account/card hint: X4321
- Department category: Transfer
- Notes: Person-to-person UPI should default to Transfer unless a user rule says otherwise. Ignore balance and block instructions.

## Early Pattern Notes

Important patterns found so far:

- ICICI card spends use: `INR <amount> spent using ICICI Bank Card <card> ... on <merchant>`
- HDFC UPI debits use multiline format with `Sent Rs.<amount>`, `From`, `To`, and `On`
- PNB UPI debits use: `debited INR <amount> ... to <payee> thru UPI`
- Available limits and balances are not transaction amounts
- Ref numbers and block instructions should be ignored
- Person-to-person UPI should usually be `Transfer`
- Mutual fund payments should be treated separately from normal expenses

### Sample 7: ICICI OTP For Card Transaction

Raw SMS:

```text
XXXXXX is One-Time Password for INR 606.00 transaction towards GROFERS IND using ICICI Bank Credit Card XX1234. OTPs are SECRET. DO NOT disclose
```

Expected Output:

- Is transaction: no
- Amount: 606.00
- Direction: unknown
- Merchant/payee: Grofers/Blinkit
- Payment mode: card
- Account/card hint: XX1234
- Department category: none
- Notes: This is an OTP message, not a completed transaction alert. Ignore it to avoid duplicate or false transactions.

### Sample 8: Income Tax Refund Credit

Raw SMS:

```text
Dear Customer, For PAN XXXXXX000X, An IT Refund amount of Rs 1980 for AY-2026-27 has been credited to your account XXXXXXXXXX1234 on 2026-08-07. -SBI
```

Expected Output:

- Is transaction: yes
- Amount: 1980.00
- Direction: credit
- Merchant/payee: Income Tax Refund
- Payment mode: bank transfer
- Account/card hint: XX1234
- Department category: Income/Refund
- Notes: PAN and full account number must never be stored in readable form. Use only masked account hint if needed.

### Sample 9: HDFC UPI Credit From VPA

Raw SMS:

```text
Credit Alert!
Rs.37.50 credited to HDFC Bank A/c XX1234 on 02-08-26 from VPA merchantpayouts@example (UPI XXXXXXXXXXXX)
```

Expected Output:

- Is transaction: yes
- Amount: 37.50
- Direction: credit
- Merchant/payee: merchantpayouts@example
- Payment mode: UPI
- Account/card hint: XX1234
- Department category: Income/Reward
- Notes: VPA credits may be refunds, rewards, cashback, payouts, or transfers. Initial categorization may need user correction.

### Sample 10: HDFC UPI Mandate Debit

Raw SMS:

```text
UPI Mandate:
Sent Rs.199.00
from HDFC Bank A/c 1234
To NETFLIX
22/07/26
Ref XXXXX
Not You? Call XXXXX/SMS BLOCK UPI to XXXXX
```

Expected Output:

- Is transaction: yes
- Amount: 199.00
- Direction: debit
- Merchant/payee: Netflix
- Payment mode: UPI mandate
- Account/card hint: 1234
- Department category: Subscriptions
- Notes: UPI mandate messages can represent recurring payments and should be categorized as subscription when merchant matches.

### Sample 11: AutoPay Consent Request

Raw SMS:

```text
AutoPay (E-mandate) Consent Requested!
For:Example SaaS
Amt Due: INR2285.63
Current Limit: INRXX.XX
Date:16/07/2026
Via: HDFC Bank CC XXXX
Reason:Txn value above the limit or exceeds RBI approved limit of 15K
SI HUB ID:XXXX
Click example.com to authenticate via OTP before due date.
TnC
```

Expected Output:

- Is transaction: no
- Amount: 2285.63
- Direction: pending
- Merchant/payee: Example SaaS
- Payment mode: card mandate
- Account/card hint: CC XXXX
- Department category: none
- Notes: This is a consent/authentication request, not a completed debit. Ignore for MVP or store later as a pending mandate reminder.

### Sample 12: EPFO PF Interest Credit

Raw SMS:

```text
PF interest of 2654 for 2025-26 credited to your UAN XXXXXXXXXXXX (XXXXXXXXXXXXXXXXXXXX) The CB on 31MAR2026 is XXXXX - EPFO
```

Expected Output:

- Is transaction: optional
- Amount: 2654.00
- Direction: credit
- Merchant/payee: EPFO
- Payment mode: provident fund
- Account/card hint: none
- Department category: Investment/Retirement
- Notes: This is not a bank account transaction. For MVP, we may ignore it unless Sorted later supports investment/retirement entries.

## Ignore Rules Found So Far

Ignore messages when they contain:

- `One-Time Password`
- `OTP`
- `Consent Requested`
- Requests to authenticate before payment
- Promotional or advisory language without completed debit/credit

Exceptions:

- Some completed transaction messages include safety text like `Not You?` or `If not you`. These should not be ignored.

## Credit Handling Notes

Credits need separate treatment from expenses.

Early credit types:

- Refund
- Cashback/reward
- Salary/income
- Personal transfer
- Interest
- Investment/retirement credit

For MVP, credits can appear in the feed but should not be mixed into expense totals unless the user chooses that behavior.

### Sample 13: HDFC Payment Alert Mutual Fund Deduction

Raw SMS:

```text
PAYMENT ALERT!
INR 2500.00 deducted from HDFC Bank A/C No 1234 towards Indian Clearing Corporation Lt UMRN: XXXXX
```

Expected Output:

- Is transaction: yes
- Amount: 2500.00
- Direction: debit
- Merchant/payee: Indian Clearing Corporation Lt
- Payment mode: bank transfer/mandate
- Account/card hint: 1234
- Department category: Investment
- Notes: Message body has no transaction date, so Sorted should use the SMS received date. UMRN should not be stored.
