# Local Data Storage

Local Data Storage is the third core component of Sorted.

Sorted's privacy promise depends on storing transaction data only on the user's device.

## Goal

Store parsed and categorized transactions locally in a way that is:

- Private
- Fast
- Easy to query
- Easy to correct
- Safe for sensitive SMS data

## Storage Direction

For Android, Sorted should use:

- Room database
- Local files only if needed
- No cloud database for MVP
- No login requirement

Prototype note:

The current Android prototype uses Android's built-in SQLite directly. This keeps the schema easy to change while SMS and Gmail imports are still being stabilized. Once the model settles, it can be migrated to Room.

## Important Privacy Decision

Sorted should not store full raw SMS bodies by default.

Raw SMS messages may contain:

- Full or partial account numbers
- Card numbers
- PAN
- UAN
- Reference numbers
- Phone numbers
- Balances
- Available limits
- Personal names

Instead, Sorted should store:

- Parsed transaction fields
- Android SMS message ID, if available
- Message hash/fingerprint for duplicate detection
- Masked account/card hint

Raw SMS storage can be considered later only for:

- Debug builds
- User-enabled troubleshooting mode
- Temporary parser diagnostics

## Core Tables

### 1. transactions

Stores each transaction shown in the app.

Fields:

```text
id
source
sourceMessageId
sourceMessageHash
amount
currency
direction
merchantRaw
merchantNormalized
miscCategory
departmentCategory
transactionType
paymentMode
accountHint
transactionDate
transactionTime
status
categorySource
confidence
userEdited
createdAt
updatedAt
```

### 2. category_rules

Stores built-in and user-created categorization rules.

Fields:

```text
id
pattern
matchType
merchantNormalized
miscCategory
departmentCategory
transactionType
priority
source
enabled
createdAt
updatedAt
```

Example:

```json
{
  "pattern": "SWIGGY",
  "matchType": "contains",
  "merchantNormalized": "Swiggy",
  "miscCategory": "Food Delivery",
  "departmentCategory": "Food",
  "transactionType": "expense",
  "priority": 10,
  "source": "built_in",
  "enabled": true
}
```

### 3. merchant_aliases

Stores alternate names for the same merchant.

Fields:

```text
id
alias
merchantNormalized
source
createdAt
updatedAt
```

Examples:

```text
SWIGGY -> Swiggy
GROFERS IND -> Blinkit
AMZN MKTP -> Amazon
```

### 4. user_corrections

Stores user edits made to transactions.

Fields:

```text
id
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

This table helps Sorted understand how the user corrected the app.

### 5. scan_state

Stores SMS scanning progress.

Fields:

```text
id
lastScannedMessageId
lastScannedAt
initialScanCompleted
createdAt
updatedAt
```

This prevents repeated full scans.

## Source Values

Allowed transaction sources:

- `sms`
- `manual`
- `import`

For MVP:

- `sms`
- `manual`

## Direction Values

Allowed values:

- `debit`
- `credit`
- `unknown`

## Status Values

Allowed values:

- `completed`
- `pending`
- `failed`
- `ignored`
- `unknown`

For stored transactions, MVP should mostly use:

- `completed`

Ignored messages should usually not be stored in the transactions table.

## Duplicate Detection

Sorted should avoid importing the same SMS transaction twice.

Possible duplicate keys:

- Android SMS message ID
- Source message hash
- Amount + date + account hint + merchant

Recommended MVP approach:

1. Use Android SMS message ID when available.
2. Also calculate a local hash of cleaned SMS text.
3. If either already exists, skip import.

## Manual Transactions

Manual transactions should use the same transaction table.

Differences:

- `source = manual`
- `sourceMessageId = null`
- `sourceMessageHash = null`
- `userEdited = true`

Manual transactions should still use the Categorization Engine.

## User Edits

When a user edits a transaction:

1. Update the transaction row.
2. Set `userEdited = true`.
3. Add a row in `user_corrections`.
4. Optionally create or update a `category_rules` row.

User-created category rules should have higher priority than built-in rules.

## Query Needs

The app should efficiently query:

- Recent transaction feed
- Transactions by month
- Transactions by department category
- Transactions by merchant
- Transactions by source
- Transactions needing review

Useful indexes:

```text
transactionDate
departmentCategory
merchantNormalized
sourceMessageId
sourceMessageHash
source
```

## What Not To Store

Do not store:

- OTPs
- Full SMS body by default
- Full account numbers
- Full card numbers
- PAN
- UAN
- Full transaction reference numbers
- Phone numbers from bank safety instructions
- Available balances
- Available limits

## MVP Storage Behavior

For the first version:

- Store parsed transactions locally
- Store category rules locally
- Store user corrections locally
- Store manual transactions locally
- Do not upload anything
- Do not require account creation
- Do not store raw SMS bodies by default
