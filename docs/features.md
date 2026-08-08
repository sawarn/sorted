# Sorted Feature Breakdown

This document breaks the first version of Sorted into simple feature components.

## Product Direction

Sorted should start as a private Android transaction organizer.

The first version should focus on one job:

> Read transaction messages, extract transactions, and categorize them well.

It should not try to become a full budgeting app immediately.

## Core Components

### 1. SMS Reader

Reads SMS messages from the user's phone after permission is granted.

Purpose:

- Find transaction-related SMS alerts
- Scan old messages during setup
- Detect new transaction messages later

### 2. Transaction Detector

Decides whether an SMS is a real financial transaction.

Examples of messages to detect:

- Debit alerts
- Credit alerts
- UPI payments
- Card spends
- ATM withdrawals
- Refunds
- Bank charges

Examples of messages to ignore:

- OTPs
- Promotional messages
- Balance-only messages
- Loan ads
- Generic bank notices

### 3. Transaction Parser

Extracts useful details from a transaction SMS.

Target fields:

- Amount
- Debit or credit
- Merchant/payee
- Date and time
- Payment mode
- Account/card hint
- Raw SMS reference

### 4. Merchant Normalizer

Cleans messy merchant names into readable names.

Examples:

- `SWIGGYUPI` -> `Swiggy`
- `AMZN MKTP` -> `Amazon`
- `ZOMATO LTD` -> `Zomato`
- `UBERINDIA` -> `Uber`

### 5. Categorization Engine

Assigns categories to each transaction.

Three levels:

- Merchant level: Swiggy, Amazon, Uber
- Misc level: Food delivery, Grocery, Cab, Subscription
- Department level: Food, Shopping, Transport, Utilities

### 6. Local Database

Stores all app data on the user's device.

Should store:

- Transactions
- Merchant rules
- Category rules
- User corrections
- Manual transactions

### 7. Manual Entry

Allows users to add a transaction manually.

This is needed for:

- Cash spends
- Missing SMS transactions
- Split payments
- Personal notes

### 8. Correction Learning

When the user corrects a merchant or category, Sorted should remember it locally.

Example:

If the user changes `Amazon` from `Shopping` to `Groceries`, similar future transactions should follow that correction.

### 9. Transaction Feed

The main screen of the app.

Should show:

- Merchant name
- Amount
- Debit/credit indicator
- Category
- Date
- Source: SMS or manual

The feed should be fast, clean, and easy to scan.

### 10. Settings and Privacy

Explains clearly that:

- Data stays on the device
- SMS is used only for transaction detection
- No bank login is required
- No cloud account is required

## First MVP Scope

Include:

- SMS permission flow
- SMS scan
- Transaction detection
- Basic parser
- Merchant normalization
- Category assignment
- Local storage
- Transaction feed
- Manual add
- Edit/correct category
- Local correction memory

Do not include yet:

- Login
- Cloud sync
- Bank linking
- Budgets
- Investments
- Receipt scanning
- AI chat
- Multi-device support

