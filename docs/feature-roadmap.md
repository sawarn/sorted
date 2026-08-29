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
- Spend, transfers, investments, refunds, rewards, and income must be separated.
- Every computed number should have a transaction list behind it.
- User corrections should override parser/category defaults.
- A corrected transaction should not be overwritten by the next SMS/Gmail rescan.

### Speed

- The first screen should render from local storage.
- Scans and syncs should update the feed after the UI is already usable.
- Heavy parsing improvements should be tested with fixtures before touching UI.

### Design

- Dark mode: pitch black, premium, quiet, yellow spectrum highlights.
- Light mode: expressive, colorful, playful, different personality from dark mode.
- Bottom navigation remains: Home, Insights, Capture, Sources.
- Settings stays at top right.

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
- Review amount for current month spend rows.
- Filter chips: All, Merchant, Category, Type, Source.
- Reason label for each row.
- Transaction rows that open the correction sheet.

### Actions

- Correct merchant.
- Correct category.
- Correct transaction type.
- Remember correction as a local rule.
- Apply correction only to this transaction.
- Ignore transaction permanently.

### Data

- Uses `transactions`.
- Uses `user_corrections`.
- Uses `category_rules`.
- Uses `ignored_transactions`.

### Done

- The user can find every uncertain current-month spend row in one screen.
- Correcting a row removes it from review if the correction makes it trusted.
- Ignored rows do not return after rescan.

## Feature 2: Why This Number?

### Problem

When a monthly spend amount changes, the user needs to know exactly why. This was the central reliability issue during early testing.

### Product Behavior

The monthly spend card should open a breakdown screen that explains:

- Included spend total.
- Included spend count.
- Money moved total.
- Investments total.
- Transfers total.
- Refunds and credits total.
- Gmail-only transaction impact.
- Foreign-currency converted impact.
- Review risk amount.

### Screen

The explanation screen should contain:

- Main monthly spend value.
- Included section: spend/subscription debits.
- Excluded section: transfers and investments.
- Adjustment section: refunds, rewards, income.
- Coverage section: SMS, Gmail, Manual counts.
- Review section: uncertain rows that can be opened.

### Rules

- Spend = debit transactions where type is Expense or Subscription.
- Money moved = all debit transactions.
- Investments are shown separately and excluded from spend.
- Transfers are shown separately and excluded from spend.
- Refunds and rewards are credits and should not reduce spend until refund matching is mature.

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

- Merchant pages opened from Home are spend-only.
- Merchant pages opened from Insights may include all debit movement.
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

- Tapping Food/Groceries/Shopping shows every included transaction and merchant contribution.

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

- Do not automatically reduce spend yet.
- Show gross spend and refund signals separately.
- Net spend can be added after matching accuracy is proven.

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
- Investments separated from spend.
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
- Upgrade source health summaries.
