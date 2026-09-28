# Sorted

Sorted is a private Android app that turns transaction alerts into a simple, local view of monthly spending.

It reads transaction SMS messages on the phone, can import Gmail transaction alerts with readonly access, parses useful money movement, and keeps the resulting record on the device. There is no Sorted account, no bank linking, and no financial data upload.

## What Sorted Is

Most transaction alerts are useful but messy. They arrive as SMS or email fragments, mix spend with transfers and refunds, and make it hard to understand what actually happened in a month.

Sorted's job is focused:

> Print a clean, categorized, private record of money movement from local sources.

The app currently supports:

- SMS transaction import with local parsing
- Gmail readonly import hooks for transaction alerts
- Manual transaction capture
- Merchant, category, and transaction type correction
- Local saved rules for remembered corrections
- Review for uncertain or low-confidence transactions
- Monthly spending with not-counted movement shown separately
- Refund, reward, transfer, investment, and income separation from spend
- Import status and local permission controls
- Auto-sorting rules for learned merchant/category corrections

## Privacy Model

Sorted is designed as local-first personal finance software.

- No cloud account is required.
- No bank account linking is used.
- Transaction data is stored locally on the Android device.
- Corrections and learned rules are stored locally.
- SMS parsing happens on device.
- Debug feeds, SQLite exports, signing keys, OAuth secrets, and local SDK paths must not be committed.

The repository intentionally ignores generated artifacts such as `*.db`, `debug-feed*.json`, `redacted*.json`, APKs, local Gradle state, `android/local.properties`, and secrets.

## Design Language

Sorted's governing design idea is:

> Sorted is Modern Finance Tape.

The UI should feel like a modern consumer finance app first, with private tape-like proof underneath. The user should understand Home in three seconds: how much they spent, where it went, what needs review, and what happened recently.

Important design rules:

- Every important number should open the transactions behind it.
- Uncertainty should be visible and calm.
- Transfers, investments, refunds, rewards, and income should be separated from spend.
- Refunds are signals until matching is mature; do not silently net them out.
- Use plain language: spent, payments, need review, top spending, recent spending, not counted, imports, edit transaction, auto-sorting rules.
- Avoid internal UI terms such as lines, unstamped, query, ledger, close-out, restamp, held out, and source health.
- Light mode uses Cardamom Press; dark mode uses Deep Ink.
- Amber/clay is reserved for review/action states.
- Settings is local device, privacy, imports, rules, export/delete, and appearance control.

Before changing user-facing UI, read:

- `docs/design-philosophy.md`
- `docs/design-section-scaffold.md`
- `docs/ai-design-guidance.md`
- `.codex/skills/sorted-design-philosophy/SKILL.md`

Tracked and local design references are used to explore the Modern Finance Tape language.

## Repository Structure

```text
android/     Android app written in Kotlin and Jetpack Compose
docs/        Product, parser, categorization, privacy, and design notes
prototype/   Early Kotlin parser prototype and fixtures
review/      Design exploration references
```

## Android Project

- Package/application ID: `com.sorted.app`
- Min SDK: 26
- Target SDK: 36
- Compile SDK: 36
- UI: Jetpack Compose + Material 3 primitives, styled into the Modern Finance Tape system
- Persistence: local Android storage/SQLite through the app repository layer

## Setup

Install the required tools:

- JDK 17 or newer
- Android SDK with platform `android-36`
- Android build tools for SDK 36
- Gradle, because this repo currently does not include a Gradle wrapper
- Android platform tools for `adb`

On macOS with Homebrew, one working setup is:

```bash
brew install openjdk gradle android-commandlinetools android-platform-tools
```

Create `android/local.properties` with your SDK path:

```properties
sdk.dir=/Users/you/Library/Android/sdk
```

If you installed command line tools through Homebrew, the SDK path may be:

```properties
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

Install SDK packages if needed:

```bash
sdkmanager "platforms;android-36" "build-tools;36.0.0"
```

## Build

From the Android project directory:

```bash
cd android
gradle :app:assembleDebug
```

If your JDK or SDK is not on the default path, export them first:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT="$ANDROID_HOME"
gradle :app:assembleDebug
```

The debug APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Install On A Device

Connect a phone with USB debugging enabled:

```bash
adb devices
```

Install only for the default personal Android profile:

```bash
adb install --user 0 -r android/app/build/outputs/apk/debug/app-debug.apk
```

If you need to inspect Android profiles:

```bash
adb shell pm list users
adb shell pm list packages --user 0 com.sorted.app
adb shell pm list packages --user 11 com.sorted.app
```

Avoid installing into a work profile unless you explicitly need to test that profile.

## Development Notes

- Keep UI changes aligned with the Modern Finance Tape language.
- Keep generated debug exports out of git.
- Use sanitized fixtures only.
- Do not commit `android/local.properties`, databases, APKs, secrets, keystores, or OAuth client files.
- Run `gradle :app:assembleDebug` before pushing app changes.

## Working Tagline

Sorted - your transactions, categorized privately.
