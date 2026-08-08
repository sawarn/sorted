# Correction Learning

Correction Learning is the fourth core component of Sorted.

It lets Sorted improve locally when the user corrects a merchant, category, or transaction type.

## Goal

When Sorted gets a transaction wrong, the user should be able to fix it quickly.

Sorted should remember that correction on the device and apply it to future similar transactions.

## Principle

User corrections always win.

Rule priority:

1. User-created rules
2. Built-in merchant rules
3. Keyword rules
4. Payment mode rules
5. Direction rules
6. Fallback

## What Users Can Correct

Users should be able to edit:

- Merchant name
- Misc category
- Department category
- Transaction type
- Whether a transaction should be hidden from spending totals

Later, users may also edit:

- Payment mode
- Account hint
- Transaction date
- Notes

## Correction Flow

Example:

Sorted detects:

```text
Amazon Pay Balance -> Transfer
```

User changes it to:

```text
Amazon Pay Balance -> Shopping
```

Sorted should:

1. Update the current transaction.
2. Save the correction.
3. Create or update a local user rule.
4. Apply that rule to future matching transactions.

## User Rule Shape

Example local rule:

```json
{
  "pattern": "AMAZON PAY BALANCE",
  "matchType": "exact",
  "merchantNormalized": "Amazon Pay Balance",
  "miscCategory": "Online Shopping",
  "departmentCategory": "Shopping",
  "transactionType": "expense",
  "source": "user",
  "priority": 1,
  "enabled": true
}
```

## Match Types

Supported MVP match types:

- `exact`
- `contains`

Possible later match types:

- `starts_with`
- `regex`
- `merchant_and_amount`
- `merchant_and_payment_mode`

## When To Create A Rule

Create a user rule when:

- The user changes merchant name
- The user changes misc category
- The user changes department category
- The user changes transaction type

Do not automatically create a rule when:

- The user edits only the amount
- The user edits only the date
- The user adds a note

## Applying A Correction

When a correction is saved, Sorted should ask internally:

> Should this apply only to this transaction or future similar transactions too?

For MVP, default behavior:

- Apply to this transaction
- Also remember for future transactions with the same normalized merchant

Later, the UI may offer:

- Only this transaction
- This and future transactions
- All matching past and future transactions

## Past Transactions

For MVP, do not automatically rewrite past transactions.

Reason:

- It may surprise users
- It can make history change unexpectedly
- It adds complexity

Later, Sorted can offer an explicit action:

```text
Apply this category to all past Amazon Pay Balance transactions?
```

## Conflict Handling

If multiple user rules match, Sorted should choose:

1. Exact match over contains match
2. Higher priority rule
3. Most recently updated rule

## User Correction Storage

When a correction is made, store:

```text
transactionId
oldMerchantNormalized
newMerchantNormalized
oldMiscCategory
newMiscCategory
oldDepartmentCategory
newDepartmentCategory
oldTransactionType
newTransactionType
createdRuleId
createdAt
```

This allows Sorted to understand what changed and why.

## Undo

The edit flow should eventually support undo.

For MVP:

- Allow editing again
- No complex undo history required

Later:

- Undo last correction
- Disable a learned rule
- Reset merchant to default category

## UI Behavior

Correction should be fast.

Target:

> A wrong category should be fixable in under two taps.

Possible flow:

1. Tap category chip on transaction.
2. Bottom sheet opens with common categories.
3. User selects new category.
4. Transaction updates instantly.
5. Sorted saves the local rule.

## Review State

Transactions with low confidence may be marked for review.

Examples:

- Unknown merchant
- Fallback category
- Conflicting rules
- Partial parser result

Possible field:

```text
needsReview = true
```

For MVP, this can be hidden from the UI and used later.

## Privacy

Correction Learning must remain local.

Do not upload:

- User corrections
- Merchant rules
- Category rules
- Transaction history

## MVP Behavior

For the first version:

- User can edit merchant/category/type
- Sorted updates the transaction
- Sorted stores the correction locally
- Sorted creates a user rule for future transactions
- User rules override built-in rules
- Past transactions are not changed automatically

