# Manual Entry

Manual Entry is the fifth core component of Sorted.

It lets users add transactions that were not detected from SMS.

## Goal

Allow users to quickly add a transaction manually without making Sorted feel like a heavy expense tracker.

Manual entry should be fast, minimal, and optional.

## Why It Matters

SMS import will not catch everything.

Examples:

- Cash expenses
- Transactions without SMS alerts
- Split payments
- Borrowed or lent money
- Small personal entries
- Transactions from accounts that do not send SMS alerts

## Principle

Manual transactions should use the same transaction model as SMS transactions.

That means they should still support:

- Merchant name
- Amount
- Direction
- Payment mode
- Misc category
- Department category
- Transaction type
- User correction learning
- Local storage

## Required Fields

For MVP, manual entry should require only:

- Amount
- Merchant or note
- Department category
- Date

Default values:

- Direction: `debit`
- Currency: `INR`
- Date: today
- Source: `manual`
- Payment mode: `unknown`

## Optional Fields

Optional fields:

- Misc category
- Transaction type
- Payment mode
- Time
- Account hint
- Note

Do not make these mandatory in MVP.

## Manual Transaction Shape

Example:

```json
{
  "source": "manual",
  "sourceMessageId": null,
  "sourceMessageHash": null,
  "amount": 250.0,
  "currency": "INR",
  "direction": "debit",
  "merchantRaw": "Coffee",
  "merchantNormalized": "Coffee",
  "miscCategory": "Restaurants",
  "departmentCategory": "Food",
  "transactionType": "expense",
  "paymentMode": "unknown",
  "accountHint": null,
  "transactionDate": "2026-08-08",
  "transactionTime": null,
  "status": "completed",
  "categorySource": "user_rule",
  "confidence": 1.0,
  "userEdited": true
}
```

## Categorization Behavior

When a user enters a merchant name, Sorted should run the Categorization Engine.

Example:

User types:

```text
Swiggy
```

Sorted suggests:

- Merchant: Swiggy
- Misc: Food Delivery
- Department: Food

If the user chooses a different category, Correction Learning should create a local rule.

## UI Flow

Manual entry should be quick.

Suggested MVP flow:

1. Tap add button.
2. Enter amount.
3. Enter merchant or note.
4. Pick category if not auto-suggested.
5. Save.

Target:

> A common manual transaction should be addable in under 10 seconds.

## Add Button

The app should always make manual entry available, but not dominant.

Possible placement:

- Floating action button on transaction feed
- Small `+` button in top bar
- Quick action from bottom navigation later

## Direction

Most manual entries will be debit expenses.

For MVP:

- Default to `debit`
- Allow switching to `credit`

Credit examples:

- Cash received
- Refund not detected by SMS
- Personal repayment
- Manual income entry

## Payment Mode

Payment mode can be optional.

Allowed values:

- Cash
- UPI
- Card
- Bank Transfer
- Wallet
- Other

Internally these can map to:

- `cash`
- `upi`
- `card`
- `bank_transfer`
- `wallet`
- `unknown`

## Notes

Notes are optional.

Examples:

- `split with friends`
- `office lunch`
- `cash payment`

Notes should remain local.

## Duplicate Handling

Manual transactions should not be deduplicated against SMS transactions automatically in MVP.

Reason:

- A user may intentionally add a cash split related to an SMS transaction
- Automatic duplicate detection may hide valid entries

Later, Sorted can warn:

```text
Similar transaction found. Save anyway?
```

## Editing Manual Transactions

Manual transactions should be editable.

Users should be able to change:

- Amount
- Merchant/note
- Category
- Date
- Direction
- Payment mode

## Privacy

Manual entries are private and local.

Do not upload:

- Manual transactions
- Notes
- Categories
- Merchant names

## MVP Behavior

For the first version:

- User can add a manual transaction
- Required fields stay minimal
- Categorization suggestions run locally
- Saved transaction appears in the same feed as SMS transactions
- Manual entries use `source = manual`
- Manual entries can be edited later

