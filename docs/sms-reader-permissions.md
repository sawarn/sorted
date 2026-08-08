# SMS Reader And Permissions

The SMS Reader is the sixth core component of Sorted.

It is responsible for getting transaction messages from the user's phone and sending them to the SMS Parser.

## Goal

Read SMS transaction alerts locally on the device after explicit user permission.

The SMS Reader should:

- Request SMS permission clearly
- Scan existing messages during setup
- Detect new messages after setup
- Send only message text/metadata to the local parser
- Avoid duplicate imports
- Never upload SMS content

## Important Android Reality

SMS access is a sensitive Android permission.

For local testing on a personal device, we can build and install the app directly.

For Play Store publishing, SMS permissions require careful policy handling. Google Play restricts SMS and Call Log permissions. Google's policy lists SMS-based money management, such as budget tracking, as an eligible exception category, but access must be tied to the app's critical core functionality and declared through Play Console.

Sorted's core functionality should therefore be described clearly as:

> Local SMS-based transaction organization and money management.

Sources checked:

- Google Play Console Help: Use of SMS or Call Log permission groups
- Android Developers: `READ_SMS` and `RECEIVE_SMS` permissions

## Required Permissions

Likely MVP permissions:

```text
READ_SMS
RECEIVE_SMS
```

Purpose:

- `READ_SMS`: scan existing SMS messages
- `RECEIVE_SMS`: detect new incoming SMS messages

We should avoid requesting:

- `SEND_SMS`
- `WRITE_SMS`
- Call log permissions
- Contacts permissions

## Permission Principles

Sorted should request the smallest useful permission set.

Rules:

- Ask only when needed
- Explain why before the Android permission prompt
- Do not block manual mode if SMS permission is denied
- Make privacy promise clear
- Do not collect unrelated SMS data
- Process messages locally

## Onboarding Flow

Suggested flow:

1. Show privacy-first explanation screen.
2. Explain that SMS access is used only to find transaction alerts.
3. Explain that data stays on the phone.
4. Ask for SMS permission.
5. If allowed, run initial scan.
6. If denied, continue with manual entry mode.

Example copy:

```text
Sorted reads transaction SMS alerts on this phone to build your private spending feed. Your messages and transactions stay on this device.
```

## Initial SMS Scan

After permission is granted, Sorted should scan existing messages.

Scan behavior:

- Read SMS inbox
- Process messages newest first
- Run ignore rules early
- Parse only transaction-like messages
- Store parsed transaction data
- Store message ID/hash for duplicate detection
- Do not store full raw SMS body by default

Possible scan limits for MVP:

- Last 90 days
- Or last 1,000 messages

This keeps setup fast and avoids unnecessary processing.

## New SMS Detection

After setup, Sorted should detect new incoming messages.

Flow:

1. SMS arrives.
2. Sorted receives SMS event.
3. Message goes through ignore detection.
4. Transaction messages are parsed.
5. Categorization Engine runs.
6. Transaction is stored locally.
7. Feed updates.

## Duplicate Prevention

Sorted should not import the same transaction twice.

Use:

- Android SMS message ID when available
- Cleaned message hash
- Amount + date + merchant + account hint fallback

## Manual Mode Without SMS

If the user denies SMS permission, Sorted should still work.

Available features:

- Manual transaction entry
- Local categorization
- Transaction feed
- Category correction

Unavailable features:

- SMS auto-import
- Automatic transaction detection

## What The SMS Reader Should Not Do

Do not:

- Upload messages
- Read contacts
- Send SMS
- Modify SMS
- Store OTPs
- Store raw SMS bodies by default
- Parse promotional messages into transactions
- Import consent requests as completed spends

## Internal Flow

```text
SMS Reader
  -> Message Cleaner
  -> Ignore Detector
  -> SMS Parser
  -> Categorization Engine
  -> Local Database
  -> Transaction Feed
```

## MVP Behavior

For the first version:

- Ask for `READ_SMS`
- Optionally add `RECEIVE_SMS` for new incoming messages
- Scan recent SMS messages
- Process everything locally
- Store only parsed transaction data
- Support manual-only mode if permission is denied
- Keep SMS access as part of the app's clearly stated core functionality

## Play Store Notes

Before publishing, we need:

- Clear app description explaining SMS-based money management
- Prominent in-app disclosure
- Privacy policy
- Play Console permissions declaration
- Proof that SMS access is core to the app
- No unrelated SMS collection
- No cloud upload of SMS data

