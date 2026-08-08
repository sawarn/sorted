# Sorted

Sorted is a private Android app for organizing personal transactions from SMS alerts.

The app reads transaction messages on the user's phone, extracts useful transaction details, and categorizes spending by merchant, category, and department. Everything stays on the device.

## Basic Idea

Many people receive transaction alerts through SMS, but those messages are messy and hard to review. Existing finance apps often feel too heavy, require account linking, collect data, or do not categorize transactions well.

Sorted should solve one focused problem:

> Turn transaction messages into a clean, categorized, private spending feed.

## Core Promise

- No cloud account required
- No financial data uploaded
- No bank linking
- Data stored only on the user's device
- Smooth, fast, minimal experience

## First Feature

The first version should focus only on transaction categorization.

Sorted should:

- Read transaction SMS messages
- Detect real transactions
- Extract amount, merchant, date, and transaction type
- Categorize at merchant level
- Categorize miscellaneous merchants
- Assign department categories like Food, Utilities, Shopping, Transport, etc.
- Allow users to manually add transactions
- Allow users to correct categories
- Remember corrections locally

## Repository Structure

```text
android/     Native Android prototype
docs/        Product, parser, categorization, storage, and privacy notes
prototype/   Early Kotlin parser prototype and fixtures
```

## Current Prototype

The Android prototype currently includes:

- Kotlin + Jetpack Compose UI
- SMS permission flow
- Local SMS import
- Rule-based SMS parser
- Merchant and department categorization
- Local SQLite persistence
- Monthly totals and merchant grouping
- Gmail readonly import prototype hooks

## Local Development

Open the Android project:

```text
Sorted/android
```

Build:

```bash
cd android
gradle :app:assembleDebug
```

Install on a connected Android device:

```bash
adb install --user 0 -r app/build/outputs/apk/debug/app-debug.apk
```

Create your own ignored `android/local.properties` file with your Android SDK path:

```properties
sdk.dir=/Users/you/Library/Android/sdk
```

## Design Direction

Sorted should feel:

- Simple
- Fast
- Premium
- Calm
- Lightweight
- Privacy-first

The main screen should probably be a clean transaction feed, not a complex dashboard.

## Early MVP Boundary

Include:

- Android app
- Local storage
- SMS-based transaction import
- Manual transaction entry
- Merchant/category correction
- Basic monthly category view

Avoid for now:

- Login
- Cloud sync
- Bank account linking
- Budgets
- Investment tracking
- Receipt scanning
- AI chat
- Multi-device sync

## Privacy Boundary

The repository intentionally excludes:

- Pulled SQLite databases
- Exported SMS/Gmail debug feeds
- Android SDK local paths
- Build outputs
- Heap dumps
- Signing keys and OAuth/secrets files

Only sanitized examples and docs should be committed.

## Working Tagline

Sorted - your transactions, categorized privately.
