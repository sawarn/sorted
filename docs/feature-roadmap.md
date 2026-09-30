# Sorted Feature Roadmap

Sorted should not become another budget tracker. Its edge is a private transaction intelligence layer that explains exactly what happened with money on the phone.

The north star:

> Every month total should be explainable in two taps.

## Product Principles

### Privacy

- SMS, Gmail-derived rows, manual rows, rules, ignores, and corrections stay on the device.
- No bank credentials or account linking.
- No raw message export unless the user explicitly triggers a debug/export action.
- Gmail should remain optional and should only improve coverage for transaction emails missed by SMS.

### Trust

- Sorted should never hide uncertainty.
- `Spent this month` is the gross total of every eligible outgoing debit, regardless of category or transaction type, including transfers and investments. It is not pure consumption.
- Categories/types remain visible classifications for outgoing debits; they do not determine headline inclusion.
- Incoming refunds, rewards, and income stay separate and are never netted against gross outgoing payments.
- Every computed number should have a transaction list behind it.
- Every total, count, and share must reconcile with the exact transactions behind it.
- User corrections should override parser/category defaults.
- A corrected transaction should not be overwritten by the next SMS/Gmail rescan.

### Speed

- The first screen should render from local storage.
- Scans and syncs should update the feed after the UI is already usable.
- Heavy parsing improvements should be tested with fixtures before touching UI.

### Design

- Governing philosophy: Sorted is Modern Finance Tape.
- Sorted should feel like a clean modern finance app on the surface and a private money tape underneath.
- Light mode: Cardamom Press, with pale sage paper, deep pine ink, muted sage labels, and clay review marks.
- Dark mode: Deep Ink, with near-black green surfaces, cool paper ink, muted sage-blue labels, and warm amber review marks.
- UI language: spent this month, payments, need review, top spending, recent spending, money back, imports, edit transaction, auto-sorting rules.
- Avoid user-facing internal terms such as lines, unstamped, query, ledger, close-out, restamp, held out, and source health.
- Home must answer the month in three seconds.
- Insights explores categories and merchants across eligible outgoing debits, changes, recurring, money back, and imports.
- Add is amount-first manual transaction entry.
- Auto-sorting rules are learned corrections in the user's words.
- Settings is local device, privacy, imports, rules, export/delete, and appearance control.
- Bottom navigation can remain for one-handed Android use, but it should use plain labels and avoid badges unless review truly requires it.

## Feature 1: Sort Inbox

### Problem

The app can parse many messages, but real-world transaction data will always contain uncertain rows. If those rows are mixed into normal insights, the user loses trust.

### Product Behavior

Sort Inbox is a dedicated review surface for transactions that need attention.

Rows enter Sort Inbox when:

- Department category is `Other`.
- Misc category is `Uncategorized`.
- Category source is fallback.
- Confidence is below the trust threshold.
- Merchant looks like a raw UPI handle or unclean identifier.
- Transaction source is Gmail-only and high value.
- Transaction is foreign-currency converted.
- Transaction is a possible duplicate.

### Screen

Sort Inbox should show:

- Review count.
- Review amount for current-month outgoing debit rows that contribute to `Spent this month`.
- Filter chips: All, Merchant, Category, Type, Source.
- Reason label for each row.
- Transaction rows that open the correction sheet.

### Actions

- Correct merchant.
- Correct category.
- Correct transaction type.
- Remember correction as a local rule.
- Apply correction only to this transaction.
- Mark a record as not a transaction or as a duplicate, where applicable.

Review dismissal is not a way to exclude a valid outgoing debit. A valid eligible debit remains in `Spent this month` while it is being reviewed. Category or type corrections do not change inclusion; amount corrections update the total and its transaction list together.

### Data

- Uses `transactions`.
- Uses `user_corrections`.
- Uses `category_rules`.
- Uses `ignored_transactions`.

### Done

- The user can find every uncertain current-month outgoing debit row in one screen; review status does not silently remove an eligible debit from the headline.
- Correcting a row removes it from review if the correction makes it trusted.
- Dismissed non-transaction or duplicate records do not return after rescan; dismissing review never silently removes a valid outgoing debit from totals.

## Feature 2: Why This Number?

### Problem

When a monthly spend amount changes, the user needs to know exactly why. This was the central reliability issue during early testing.

### Product Behavior

The `Spent this month` card should open a breakdown screen that explains the gross outgoing total and reconciles it to:

- Outgoing payment total and count.
- Category totals and counts, including investments and transfers.
- Type totals where useful, without implying that a type is excluded.
- Incoming refunds, rewards, and income as separate credit totals and counts.
- Gmail-only transaction impact.
- Foreign-currency converted impact.
- Review-needed outgoing debit amount.

### Screen

The explanation screen should contain:

- Main `Spent this month` value and outgoing payment count.
- A breakdown of outgoing debits by category/type, with transfers and investments included and labeled.
- A separate `Money in` section for incoming refunds, rewards, and income.
- Coverage section: SMS, Gmail, Manual counts.
- Review section: uncertain outgoing debit rows that can be opened.

### Rules

- `Spent this month` = the gross sum of eligible outgoing debit transactions in the selected month, regardless of category or transaction type.
- Transfers and investments contribute to the headline and remain identifiable in category/type breakdowns.
- Incoming refunds, rewards, and income are credits; show them separately and do not subtract them from the gross outgoing total.
- Only completed debits with a positive amount and usable INR value enter the total. Unresolved foreign-currency amounts and non-completed payments remain visible for review.
- Any amount, count, or share shown here opens the exact transaction set used to calculate it.

### Done

- Tapping monthly spend explains the number without needing logs.
- The screen is useful even when Gmail or FX transactions are present.

## Feature 3: Merchant Intelligence

### Problem

Sorted is supposed to be strong at merchant-level categorization. A merchant page should be more than a list.

### Product Behavior

Merchant pages should show:

- Current-month spend for that merchant.
- Transaction count.
- Average spend.
- Largest transaction.
- Payment mode split.
- Source split.
- Category split.
- Recent transactions.
- Review-needed rows for that merchant.

### Rules

- Merchant pages opened from Home or Insights use the same eligible outgoing-debit rule as `Spent this month`, including transfers and investments.
- Credit transactions remain separate from outgoing debit totals and are shown in their applicable credit views.
- User can still open any row and correct it.

### Done

- Tapping Swiggy/Amazon/Blinkit/Groww gives a focused page that explains the merchant.

## Feature 4: Category Intelligence

### Problem

Category totals are useful only when the user can see what is inside them.

### Product Behavior

Category pages should show:

- Total for category.
- Merchant split within the category.
- Source split.
- Payment mode split.
- Transactions.

### Done

- Tapping a category shows every outgoing debit in that category and merchant contribution, including investment/transfer categories when applicable; its total reconciles to the category's filtered transaction list.

## Feature 5: Rule Center

### Problem

Sorted learns from corrections. The user needs a place to see what it has learned.

### Product Behavior

Rule Center should show:

- Learned merchant pattern.
- Merchant name output.
- Category output.
- Merchant tag output.
- Transaction type output.
- Rule source.
- Last updated.

### Actions

- Initially read-only.
- Later: disable rule, edit rule, delete rule, create rule manually.

### Done

- Settings shows user-created rules.
- Rule count is visible.
- No hidden categorization magic.

## Feature 6: Refund Matching

### Problem

Refunds can make merchant/category totals misleading. Early versions should surface refund signals before automatically netting them.

### Product Behavior

Sorted should detect refund candidates:

- Credit direction.
- Transaction type Refund/Reward.
- Category Refund/Reward.
- Merchant names that match recent spend merchants.
- Keywords like refund, reversal, cashback, credited.

### Screen

Insights should show a Refund Signals card:

- Refund total.
- Candidate count.
- Matched merchant candidates.
- Rows open transaction detail.

### Rules

- Never automatically reduce `Spent this month` by a refund or other incoming credit.
- Show gross outgoing payments and incoming refund signals separately.
- A future matched-refund view may explain a relationship, but must not silently change the gross outgoing headline.

### Done

- Refunds are no longer invisible.
- The user can inspect candidate refund rows.

## Feature 7: Recurring Radar

### Problem

Recurring payments are high-signal because they are predictable and often missed until they change.

### Product Behavior

Detect recurring candidates from local transaction history:

- Same merchant appears at least twice.
- Same type is Subscription, Investment, Transfer, or repeated Expense.
- Amounts are close or transaction day is close.
- Merchants include mandate/SIP/subscription signals.

### Screen

Insights should show Recurring Radar:

- Merchant.
- Expected amount.
- Frequency confidence.
- Last seen date.
- Category/type.

### Done

- Netflix/SIP/EMI/cloud subscriptions start becoming visible without manual tagging.

## Feature 8: Source Health

### Problem

The user needs to trust coverage. SMS and Gmail may disagree or one source may miss transactions.

### Product Behavior

Sources should show:

- SMS status.
- Gmail status.
- Manual count.
- Last import label.
- Gmail-only transaction count.
- FX-converted rows.
- Review-needed rows by source.

### Done

- The user can see whether bad data is caused by parser, source coverage, or review debt.

## Feature 9: Month Story

### Problem

Charts are not enough. The app should explain the month in language-like cards while staying local and deterministic.

### Product Behavior

The month story should show:

- Top merchant.
- Top category.
- Biggest spend day.
- New high-value Gmail-only rows.
- Review debt.
- Investments visible as a category/type within gross outgoing payments.
- Refund signals.

### Done

- Insights explains the month, not only lists amounts.

## Feature 10: Manual Capture Upgrade

### Problem

Manual add should be fast enough to use for missing cash or email-only transactions.

### Product Behavior

Capture should use:

- Recent merchants.
- Recent categories.
- Recent payment modes.
- Suggested transaction type from category.
- Repeat last transaction.

### Done

- Adding a missing row takes seconds and becomes part of the same local data model.

## Implementation Order

### Batch A: Trust Foundation

- Sort Inbox full screen.
- Why This Number screen.
- Review reasons.
- Correction sheet polish.
- Rule Center read-only.

### Batch B: Insight Signals

- Merchant intelligence panels.
- Category intelligence panels.
- Refund Signals card.
- Recurring Radar card.
- Source Health upgrade.

### Batch C: Control

- Rule disable/edit/delete.
- Bulk categorize similar merchants.
- Export local CSV.
- Encrypted backup/restore.

### Batch D: Publishing

- SMS permission rationale.
- Gmail verification path.
- Privacy policy.
- Fixture tests for bank and merchant formats.

## Current Implementation Target

The current app pass should implement Batch A and the first version of Batch B:

- Add Sort Inbox screen.
- Add Why This Number screen.
- Add read-only Rule Center.
- Add refund signal detection.
- Add recurring signal detection.
- Upgrade merchant/category drilldown with intelligence panels.
- Upgrade imports and coverage summaries.
