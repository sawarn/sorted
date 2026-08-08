# Categorization Engine

The Categorization Engine is the second core component of Sorted.

The SMS Parser extracts what happened. The Categorization Engine decides what kind of transaction it is.

## Goal

Turn a parsed transaction into a clean, useful category result.

Example:

```json
{
  "merchantRaw": "SWIGGY",
  "amount": 379.0,
  "direction": "debit",
  "paymentMode": "upi"
}
```

Expected categorization:

```json
{
  "merchantNormalized": "Swiggy",
  "miscCategory": "Food Delivery",
  "departmentCategory": "Food",
  "transactionType": "expense"
}
```

## Category Levels

Sorted should support three levels of categorization.

### 1. Merchant Level

The cleaned merchant, person, or source name.

Examples:

- Swiggy
- Blinkit
- Netflix
- Amazon Pay Balance
- Mutual Funds NCL
- Income Tax Refund

### 2. Misc Category

A more specific semantic category.

Examples:

- Food Delivery
- Grocery Delivery
- OTT Subscription
- Wallet Load
- Mutual Fund
- Person Transfer
- Tax Refund
- Cashback/Reward

### 3. Department Category

The broad category shown in the main app experience.

Examples:

- Food
- Groceries
- Subscriptions
- Transfer
- Investment
- Refund
- Reward

## Why Categorization Is Separate From Parsing

The parser should extract facts from the message.

The categorizer should apply product intelligence.

This matters because future transactions may come from:

- SMS
- Manual entry
- CSV import
- Receipt scan
- Future bank integration

All sources should use the same categorization engine.

## Input

The categorizer receives a parsed transaction.

Example:

```json
{
  "amount": 350.0,
  "currency": "INR",
  "direction": "debit",
  "merchantRaw": "Swiggy",
  "paymentMode": "upi",
  "accountHint": "*1234",
  "transactionDate": "2026-08-07"
}
```

## Output

The categorizer returns category fields.

Example:

```json
{
  "merchantNormalized": "Swiggy",
  "miscCategory": "Food Delivery",
  "departmentCategory": "Food",
  "transactionType": "expense",
  "categorySource": "known_merchant_rule",
  "confidence": 0.95
}
```

## Rule Priority

Rules should run in this order:

1. User correction rules
2. Known merchant rules
3. Keyword rules
4. Payment mode rules
5. Direction rules
6. Fallback category

### 1. User Correction Rules

User corrections have the highest priority.

Example:

If the user changes `Amazon Pay Balance` to `Shopping`, future similar transactions should follow that user rule.

### 2. Known Merchant Rules

Built-in merchant mappings.

Examples:

- Swiggy -> Food Delivery -> Food
- Blinkit -> Grocery Delivery -> Groceries
- Netflix -> OTT Subscription -> Subscriptions

### 3. Keyword Rules

Used when exact merchant match is not available.

Examples:

- Contains `MUTUAL FUND` -> Mutual Fund -> Investment
- Contains `PAY BALANCE` -> Wallet Load -> Transfer
- Contains `REFUND` -> Tax Refund/Refund -> Refund

### 4. Payment Mode Rules

Used when merchant does not clearly identify the category.

Examples:

- UPI to person -> Person Transfer -> Transfer
- UPI mandate -> Subscription, if merchant looks recurring
- ATM -> Cash Withdrawal -> Transfer/Cash

### 5. Direction Rules

Used for basic transaction type.

Examples:

- Debit -> expense by default
- Credit -> income by default
- Credit + refund keyword -> refund

### 6. Fallback Category

If no rules match:

- Misc category: `Uncategorized`
- Department category: `Other`
- Transaction type: `unknown`

## Initial Department Categories

Start with a small list.

- Food
- Groceries
- Shopping
- Transport
- Fuel
- Utilities
- Subscriptions
- Travel
- Health
- Education
- Entertainment
- Investment
- Transfer
- Income
- Refund
- Reward
- Fees
- Other

## Initial Misc Categories

Early misc categories:

- Food Delivery
- Restaurants
- Grocery Delivery
- Online Shopping
- Wallet Load
- Person Transfer
- Mutual Fund
- OTT Subscription
- Tax Refund
- Cashback/Reward
- Bank Charges
- Salary
- Utility Bill
- Fuel
- Travel Booking
- Cab
- Uncategorized

This list can grow slowly as we see more real SMS examples.

## Initial Merchant Rules

| Merchant Pattern | Normalized Merchant | Misc Category | Department Category | Transaction Type |
| --- | --- | --- | --- | --- |
| `SWIGGY` | Swiggy | Food Delivery | Food | expense |
| `Swiggy` | Swiggy | Food Delivery | Food | expense |
| `BLINKIT` | Blinkit | Grocery Delivery | Groceries | expense |
| `GROFERS` | Blinkit | Grocery Delivery | Groceries | expense |
| `NETFLIX` | Netflix | OTT Subscription | Subscriptions | subscription |
| `AMAZON PAY BALANCE` | Amazon Pay Balance | Wallet Load | Transfer | transfer |
| `MUTUAL FUNDS NCL` | Mutual Funds NCL | Mutual Fund | Investment | investment |
| `IT REFUND` | Income Tax Refund | Tax Refund | Refund | refund |
| `INCOME TAX REFUND` | Income Tax Refund | Tax Refund | Refund | refund |
| `POPPCLUBPAYOUTS` | Popclub Payouts | Cashback/Reward | Reward | reward |
| `POPCLUBPAYOUTS` | Popclub Payouts | Cashback/Reward | Reward | reward |

## User Correction Learning

When a user edits a transaction category, Sorted should create a local rule.

Example correction:

```text
Merchant: Amazon Pay Balance
Old department: Transfer
New department: Shopping
```

Local rule:

```json
{
  "pattern": "AMAZON PAY BALANCE",
  "merchantNormalized": "Amazon Pay Balance",
  "miscCategory": "Online Shopping",
  "departmentCategory": "Shopping",
  "transactionType": "expense",
  "source": "user",
  "priority": 1
}
```

This rule should apply to future similar transactions.

## Category Source

The categorizer should return where the category came from.

Allowed values:

- `user_rule`
- `known_merchant_rule`
- `keyword_rule`
- `payment_mode_rule`
- `direction_rule`
- `fallback`

This helps debug wrong categories later.

## Confidence

Suggested confidence values:

- User rule: `1.00`
- Known merchant rule: `0.90 - 0.99`
- Keyword rule: `0.75 - 0.89`
- Payment mode rule: `0.60 - 0.74`
- Direction rule: `0.50 - 0.59`
- Fallback: `0.20 - 0.40`

## Examples From Current Samples

### Swiggy UPI

Input merchant:

```text
SWIGGY
```

Output:

- Merchant: Swiggy
- Misc: Food Delivery
- Department: Food
- Type: expense

### Blinkit Card Spend

Input merchant:

```text
Blinkit
```

Output:

- Merchant: Blinkit
- Misc: Grocery Delivery
- Department: Groceries
- Type: expense

### Mutual Funds NCL

Input merchant:

```text
MUTUAL FUNDS NCL
```

Output:

- Merchant: Mutual Funds NCL
- Misc: Mutual Fund
- Department: Investment
- Type: investment

### Netflix UPI Mandate

Input merchant:

```text
NETFLIX
```

Output:

- Merchant: Netflix
- Misc: OTT Subscription
- Department: Subscriptions
- Type: subscription

### Person UPI Transfer

Input merchant:

```text
PERSON NAME
```

Output:

- Merchant: Person Name
- Misc: Person Transfer
- Department: Transfer
- Type: transfer

## MVP Behavior

For the first version, the categorizer should be:

- Local only
- Rule based
- Explainable
- Easy to correct
- Fast enough to run instantly on device

No cloud model should be needed for MVP.

