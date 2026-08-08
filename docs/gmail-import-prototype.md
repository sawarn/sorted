# Gmail Import Prototype

Gmail import is now part of the prototype direction.

The goal is to make Gmail a second import source beside SMS:

```text
SMS messages
Gmail messages
Manual entries
  -> Parser
  -> Categorizer
  -> Deduplication
  -> Local database
  -> Feed and summaries
```

## Current State

Done:

- Local database exists in the Android app.
- SMS imports now persist to the local database.
- Transactions are deduped by source hash.
- Feed and summaries read from persisted transactions.
- Database schema has a `source` field ready for `sms`, `gmail`, and `manual`.
- Gmail readonly authorization flow is wired in the Android app.
- Gmail candidate message fetch is wired through the Gmail REST API.
- Gmail debug export writes redacted candidate parses to app cache.
- Gmail import writes high-confidence parsed transactions to the same local DB.
- Gmail import suppresses likely SMS/Gmail duplicates using amount + date + merchant + direction.

Not done yet:

- Google Cloud OAuth setup in the user's project/account
- Real Gmail parser tuning from the first redacted Gmail debug export
- Production OAuth verification and privacy review

## Gmail Access Model

Sorted will not connect to the Gmail app directly.

It needs:

```text
Google OAuth
Gmail API
gmail.readonly scope
```

Required scope:

```text
https://www.googleapis.com/auth/gmail.readonly
```

This is a restricted Google scope. It is acceptable for private prototype testing, but public release will need careful Google OAuth verification and privacy handling.

## Prototype Privacy Rule

For the prototype:

- Fetch candidate Gmail messages only after explicit user consent.
- Parse on device.
- Store only parsed transaction fields.
- Do not store full email bodies by default.
- Do not upload Gmail data to any backend.

## Google Cloud Setup Needed

Create a Google Cloud project and enable Gmail API.

Android OAuth client details:

```text
Package name: com.sorted.app
Debug SHA-1: <your-debug-sha-1>
```

Get the debug SHA-1 locally:

```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

The Android app also shows the package name and signing SHA-1 inside the Gmail import card when authorization fails.

OAuth consent:

- App name: Sorted
- User type: External for personal Gmail testing
- Test user: add your Gmail account
- Scope: Gmail readonly

Minimum checklist:

1. Enable Gmail API in the same Google Cloud project.
2. Configure OAuth consent screen.
3. Add `https://www.googleapis.com/auth/gmail.readonly` under Data Access/scopes.
4. Add the Gmail account used on the phone as a test user.
5. Create an OAuth client of type Android.
6. Set package name to `com.sorted.app`.
7. Set SHA-1 to the debug signing certificate from the installed APK/build machine.

If the app shows:

```text
Google auth failed (8): [8] Unknown error [status=UNREGISTERED_ON_API_CONSOLE]
```

Then Google does not recognize the installed APK. Check that the OAuth client is an Android client in the same Google Cloud project where Gmail API is enabled, and that it exactly matches:

```text
Package name: com.sorted.app
SHA-1: <your-debug-sha-1>
```

After changing Google Cloud OAuth settings, wait a few minutes, force close Sorted, then retry Gmail import.

References:

- Android authorization: https://developer.android.com/identity/authorization
- Gmail message list/get: https://developers.google.com/workspace/gmail/api/guides/list-messages
- Gmail scopes: https://developers.google.com/workspace/gmail/api/auth/scopes

## Gmail Search Queries

Initial Gmail queries should be narrow.

Current app query:

```text
newer_than:365d (INR OR Rs OR debited OR credited OR spent OR deducted OR refund OR payment OR transaction OR invoice OR receipt OR UPI OR card)
```

We should avoid reading the entire mailbox.

## Gmail Parser Targets

Useful Gmail patterns:

- Card receipts
- Payment receipts
- Refund confirmations
- Subscription invoices
- App store invoices
- Travel bookings
- Razorpay/PayU/Cashfree confirmations
- Bank transaction emails

## Deduplication With SMS

Gmail and SMS may describe the same transaction.

Potential duplicate key:

```text
amount + date + merchant + direction
```

If both SMS and Gmail have same transaction:

- Prefer SMS for bank-confirmed cashflow.
- Use Gmail for extra merchant/category detail later.

## Next Implementation Steps

1. Complete Google Cloud OAuth setup.
2. Tap Gmail import in the Android app.
3. Pull `cache/sorted-gmail-debug-feed.json` from the phone.
4. Review real Gmail patterns in detail.
5. Add parser fixtures for the useful email formats.
6. Tighten deduplication after seeing actual SMS/Gmail overlap.
