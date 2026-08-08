# Sorted Build Plan

This document captures the recommended path for building Sorted.

## Recommendation

Do not start with the Android UI immediately.

The hardest part of Sorted is reliable transaction parsing and categorization. The app should be built around a stable transaction data shape, not the other way around.

## Phase 1: Product Skeleton

Define the first version clearly.

The first version should include:

- SMS import
- Transaction detection
- Amount/date/merchant extraction
- Merchant normalization
- Category assignment
- Manual transaction add
- Edit/correct category
- Local learning from corrections
- Transaction feed

Output of this phase:

- Feature breakdown
- Basic data model
- Initial category list

## Phase 2: Parser Prototype

Create a small local prototype before building the full Android app.

Goal:

> Feed sample SMS messages into the parser and check whether the output is correct.

Example input:

```text
Rs.450.00 debited from your HDFC Bank account via UPI to SWIGGY on 08-Aug-26.
```

Expected output:

```json
{
  "amount": 450,
  "direction": "debit",
  "merchant": "Swiggy",
  "department": "Food",
  "paymentMode": "UPI"
}
```

This makes it easier to test many SMS formats quickly.

## Phase 3: Android App

Once the parser output is stable, build the Android app.

Suggested stack:

- Kotlin
- Jetpack Compose
- Room database
- WorkManager
- Local-only categorization logic

Core screens:

- Onboarding and SMS permission
- Transaction feed
- Transaction detail/edit
- Manual add transaction
- Category correction
- Privacy/settings

## Phase 4: Polish

After the core flow works, improve the app feel.

Focus areas:

- Smooth animations
- Fast feed interactions
- Dark mode
- Empty states
- Haptics
- Category correction bottom sheet
- Privacy-first onboarding copy
- Play Store compliance notes

## Build Principle

The first useful version should do one thing extremely well:

> Convert messy transaction SMS alerts into a clean, categorized, private transaction feed.

