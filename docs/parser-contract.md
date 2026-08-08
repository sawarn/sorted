# Parser Contract

This document defines what the SMS Parser should return for every message.

The goal is to make the parser predictable before we start implementation.

## Parser Input

The parser receives one raw SMS message as text.

Input may include:

- Plain SMS body
- Multiline SMS body
- Copied chat wrapper text
- Extra spaces
- Bank safety instructions
- Reference numbers
- Balance or limit information

The parser should clean the message before processing.

## Parser Output

The parser should always return one structured result.

For completed transactions:

```json
{
  "isTransaction": true,
  "status": "completed",
  "amount": 606.0,
  "currency": "INR",
  "direction": "debit",
  "merchantRaw": "Blinkit",
  "merchantNormalized": "Blinkit",
  "paymentMode": "card",
  "accountHint": "XX1234",
  "transactionDate": "2026-08-07",
  "transactionTime": null,
  "transactionType": "expense",
  "departmentCategory": "Groceries",
  "confidence": 0.92,
  "ignoreReason": null
}
```

For ignored messages:

```json
{
  "isTransaction": false,
  "status": "ignored",
  "amount": null,
  "currency": null,
  "direction": "unknown",
  "merchantRaw": null,
  "merchantNormalized": null,
  "paymentMode": "unknown",
  "accountHint": null,
  "transactionDate": null,
  "transactionTime": null,
  "transactionType": "unknown",
  "departmentCategory": null,
  "confidence": 0.0,
  "ignoreReason": "otp"
}
```

## Field Definitions

### isTransaction

Whether this SMS should become a transaction in Sorted.

Allowed values:

- `true`
- `false`

### status

The state of the parsed message.

Allowed values:

- `completed`
- `ignored`
- `pending`
- `failed`
- `unknown`

For MVP, only `completed` and `ignored` are required.

### amount

The transaction amount.

Rules:

- Use the actual transaction amount
- Do not use available balance
- Do not use available limit
- Do not use current limit
- Do not use outstanding balance unless the message is clearly a payment transaction

### currency

Allowed values for MVP:

- `INR`
- `unknown`

### direction

Allowed values:

- `debit`
- `credit`
- `unknown`

Examples:

- `spent`, `sent`, `debited` -> `debit`
- `credited`, `refund`, `payout` -> `credit`

### merchantRaw

The merchant or payee exactly as found after cleanup.

Examples:

- `SWIGGY`
- `Amazon Pay Balance`
- `MUTUAL FUNDS NCL`
- `Blinkit`

### merchantNormalized

The cleaned display name.

Examples:

- `SWIGGY` -> `Swiggy`
- `AMZN MKTP` -> `Amazon`
- `GROFERS IND` -> `Blinkit`
- `MUTUAL FUNDS NCL` -> `Mutual Funds NCL`

### paymentMode

Allowed values:

- `upi`
- `upi_mandate`
- `card`
- `bank_transfer`
- `atm`
- `wallet`
- `provident_fund`
- `unknown`

### accountHint

Only a masked account or card hint.

Allowed examples:

- `XX1234`
- `*1234`
- `XX1234`
- `X4321`

Do not store full account numbers, full card numbers, PAN, UAN, reference numbers, or phone numbers.

### transactionDate

Date of the transaction in ISO format.

Example:

```text
2026-08-07
```

### transactionTime

Time of the transaction if available.

Example:

```text
13:41:26
```

If not available, use `null`.

### transactionType

Allowed values:

- `expense`
- `income`
- `refund`
- `transfer`
- `investment`
- `subscription`
- `reward`
- `unknown`

### departmentCategory

Initial department categories:

- `Food`
- `Groceries`
- `Shopping`
- `Transport`
- `Fuel`
- `Utilities`
- `Subscriptions`
- `Travel`
- `Health`
- `Education`
- `Entertainment`
- `Investment`
- `Transfer`
- `Income`
- `Refund`
- `Reward`
- `Fees`
- `Other`

### confidence

A parser confidence score between `0.0` and `1.0`.

Suggested meaning:

- `0.90 - 1.00`: Strong template match
- `0.70 - 0.89`: Good match but category or merchant may need correction
- `0.40 - 0.69`: Partial parse, needs review
- `0.00 - 0.39`: Ignore or uncertain

### ignoreReason

Allowed values:

- `otp`
- `consent_request`
- `promotion`
- `balance_only`
- `failed_transaction`
- `not_financial`
- `unsupported`
- `duplicate`
- `none`

Use `null` for completed transactions.

## Parser Pipeline

The parser should run in stages.

1. Clean raw text
2. Detect ignore cases
3. Detect bank/template pattern
4. Extract amount
5. Detect direction
6. Extract merchant/payee
7. Detect payment mode
8. Extract date/time
9. Extract account/card hint
10. Normalize merchant
11. Assign transaction type
12. Assign department category
13. Return confidence score

## First Supported Patterns

### ICICI Card Spend

Pattern:

```text
INR <amount> spent using ICICI Bank Card <cardHint> on <date> on <merchant>.
```

Rules:

- Direction: `debit`
- Payment mode: `card`
- Merchant comes after the second `on`
- Ignore available limit

### HDFC UPI Debit

Pattern:

```text
Sent Rs.<amount>
From HDFC Bank A/C <accountHint>
To <merchant>
On <date>
Ref <ref>
```

Rules:

- Direction: `debit`
- Payment mode: `upi`
- Merchant comes from `To`
- Ignore reference number and block instructions

### HDFC UPI Mandate Debit

Pattern:

```text
UPI Mandate:
Sent Rs.<amount>
from HDFC Bank A/c <accountHint>
To <merchant>
<date>
Ref <ref>
```

Rules:

- Direction: `debit`
- Payment mode: `upi_mandate`
- Likely transaction type: `subscription`

### PNB UPI Debit

Pattern:

```text
A/c <accountHint> debited INR <amount> Dt <date> <time> to <payee> thru UPI:<ref>.Bal INR <balance>
```

Rules:

- Direction: `debit`
- Payment mode: `upi`
- Payee comes after `to` and before `thru UPI`
- Ignore balance

### SBI Income Tax Refund

Pattern:

```text
IT Refund amount of Rs <amount> ... credited to your account <accountHint> on <date>
```

Rules:

- Direction: `credit`
- Payment mode: `bank_transfer`
- Transaction type: `refund`
- Department category: `Refund`

### HDFC UPI Credit

Pattern:

```text
Rs.<amount> credited to HDFC Bank A/c <accountHint> on <date> from VPA <vpa> (UPI <ref>)
```

Rules:

- Direction: `credit`
- Payment mode: `upi`
- Payee/source comes from VPA
- Initial category may need correction

## Ignore Patterns

### OTP

Ignore if message contains:

- `One-Time Password`
- `OTP`
- `OTPs are SECRET`

Even if the message includes an amount and merchant, it is not a completed transaction.

### Consent Request

Ignore for MVP if message contains:

- `Consent Requested`
- `authenticate via OTP`
- `Amt Due`
- `Current Limit`

This may become a pending mandate feature later.

## Privacy Rules

The parser should not store:

- Full account numbers
- Full card numbers
- PAN
- UAN
- Transaction reference numbers
- Phone numbers
- OTPs

Only masked account/card hints may be stored.

