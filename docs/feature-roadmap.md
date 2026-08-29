# Sorted Feature Roadmap

This roadmap keeps Sorted focused on accurate local transaction organization before adding heavier finance features.

## Product Rule

Sorted should be trusted before it becomes broad.

Every new feature should protect the core promise:

- Transactions stay on device.
- Monthly spend is separate from money moved.
- Corrections are fast and remembered locally.
- Unclear data is visible instead of silently counted as truth.

## Phase 1: Data Trust

Goal:

Make incorrect data easy to find and fix.

Features:

- Unsorted review section for low-confidence and Other-category transactions.
- Transaction detail actions.
- Edit merchant, category, and transaction type.
- Save correction as a local rule for future matching transactions.
- Ignore a wrong transaction so it stops returning on future scans.

Done when:

- A wrong transaction can be corrected from its detail sheet.
- A correction updates the current row immediately.
- Future imports use the local rule before built-in categorization.
- Ignored SMS/Gmail rows do not reappear after rescans.

## Phase 2: Better Review

Goal:

Help the user clean up data without opening every transaction.

Features:

- Dedicated Unsorted tab or screen.
- Bulk categorize similar merchants.
- Review filters by source, category, merchant, and confidence.
- Suggested category chips based on merchant text.
- Rule management in Settings.

Done when:

- The user can clean the top unclear merchants in a few taps.
- User rules can be reviewed, disabled, or replaced.

## Phase 3: Spend Intelligence

Goal:

Turn clean data into useful monthly understanding.

Features:

- Spend trend by month.
- Top merchant/category changes versus previous month.
- New recurring payments detected.
- Unusual spend spikes.
- Refund-adjusted spend view.
- Subscription list.

Done when:

- Insights explains why the month looks high or low.
- Recurring payments and new spikes are visible without manual searching.

## Phase 4: Capture And Sources

Goal:

Improve coverage without weakening privacy.

Features:

- Faster manual add with recent merchants.
- Manual add suggestions from existing rules.
- Gmail source controls.
- Source health summary.
- CSV export.
- Encrypted local backup and restore.

Done when:

- Missing transactions can be added quickly.
- Source quality can be understood and controlled by the user.

## Phase 5: Publishing Readiness

Goal:

Prepare for Play Store testing.

Features:

- SMS permission rationale and compliance copy.
- Debug export behind explicit user action only.
- No raw message retention in release builds.
- Privacy policy.
- Crash-safe import flow.
- Parser fixture tests for known bank formats.

Done when:

- The app can be shared with testers without exposing personal data.
- Core parsing behavior is covered by repeatable tests.
