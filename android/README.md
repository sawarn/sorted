# Sorted Android

Native Android prototype for Sorted.

## Current Scope

- Kotlin
- Jetpack Compose
- SMS permission flow
- Local SMS transaction import
- Rule-based SMS parser
- Merchant/category categorization
- Local SQLite storage
- Monthly summary and transaction feed
- Gmail readonly import prototype wiring
- Redacted local debug export for parser tuning

## Open In Android Studio

Open this folder:

```text
Sorted/android
```

Android Studio must complete its first-launch SDK setup before this project can build.

## Build

After the Android SDK is installed and `local.properties` points to the SDK:

```bash
gradle :app:assembleDebug
```

Example `local.properties`:

```properties
sdk.dir=/Users/you/Library/Android/sdk
```

`local.properties` is intentionally ignored by git.

## Install On Device

```bash
adb install --user 0 -r app/build/outputs/apk/debug/app-debug.apk
```

## Privacy Notes

- Transaction data is stored only in the app's local SQLite database.
- Exported debug feeds and pulled databases are local development artifacts and are ignored by git.
- Gmail import uses readonly access and is prototype-only until OAuth verification and privacy review are complete.
