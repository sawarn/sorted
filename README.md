# Sorted

Sorted is a private Android app that turns transaction alerts into a local, inspectable money tape.

It reads transaction SMS messages on the phone, can import Gmail transaction alerts with readonly access, parses useful money movement, and keeps the resulting ledger on the device. There is no Sorted account, no bank linking, and no financial data upload.

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
- Sort Inbox for uncertain or low-confidence lines
- Monthly spend close-outs with held-out movement shown separately
- Refund, reward, transfer, investment, and income separation from spend
- Source health and local permission/source controls
- Rule Center for saved merchant/category stamps

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

> Sorted is a private money tape.

The UI should feel like a continuous local receipt: printed lines, stamps, amendments, source metadata, query marks, close-out totals, and tape indexes. It should not feel like a generic card dashboard.

Important design rules:

- Every important number should reveal the lines behind it.
- Uncertainty should be visible and calm.
- Transfers, investments, refunds, rewards, and income should be separated from spend.
- Refunds are signals until matching is mature; do not silently net them out.
- Rules are saved stamps.
- Capture is adding a missing line.
- Settings is local device, privacy, source, export, and permission control.

Before changing user-facing UI, read:

- `docs/design-philosophy.md`
- `docs/design-section-scaffold.md`
- `docs/ai-design-guidance.md`
- `.codex/skills/sorted-design-philosophy/SKILL.md`

Tracked standalone HTML files in the repo are design references for the tape language.

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
- UI: Jetpack Compose + Material 3 primitives, styled into the Sorted tape system
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

- Keep UI changes aligned with the private money tape language.
- Keep generated debug exports out of git.
- Use sanitized fixtures only.
- Do not commit `android/local.properties`, databases, APKs, secrets, keystores, or OAuth client files.
- Run `gradle :app:assembleDebug` before pushing app changes.

## Working Tagline

Sorted - your transactions, categorized privately.
