# Sorted Section Scaffold

This document applies [Modern Finance Tape](design-philosophy.md) to each main app area.

The product should read like a modern finance app first. Tape cues remain underneath as proof, rhythm, and material.

## Navigation

Sorted navigation should be plain, one-handed, and Android-friendly.

Candidate primary destinations:

- Home
- Insights
- Add
- Review
- Settings

Rules may live inside Settings as **Auto-sorting rules** until they become important enough for a primary destination. Avoid novelty labels in navigation.

## Home

Home is the user's month at a glance.

### Purpose

Home answers four questions in order:

- How much went out this month?
- Where did it go?
- What needs review?
- What happened recently?

### Structure

Recommended order:

1. Header with Sorted and month selector.
2. Hero: `Spent this month`, amount, payment count.
3. Amber review stamp if anything needs review.
4. Top spending band with three category bars.
5. Recent spending rows.
6. A separate, muted `Money in` line if incoming credits exist.
7. Quiet privacy/import note only when useful.

### Hero

The `Spent this month` amount owns the screen. It is the gross total of eligible outgoing debits, not a measure of pure consumption, and should open the exact transactions behind it.

Show:

- `Spent this month`
- `₹42,380`
- `123 payments`

Do not show audit rows above the fold. Details such as category/type breakdown, money back, imports, and review risk belong behind taps or lower on the screen.

### Top Spending

Use three share bar rows:

- Category name
- Horizontal bar
- Percent

Bars are for scanning, not decoration. They open category transactions. Category totals include every eligible outgoing debit in that category, including transfers and investments, and reconcile with the Home headline.

### Recent Spending

Rows should be modern and readable:

- Initial or simple merchant mark.
- Merchant.
- Category and date subtitle.
- Amount on the right.
- Small `REVIEW` stamp only if needed.

Avoid showing source, parser notes, confidence, or raw alert text on Home unless the row needs review.

### Review

Review appears as one amber action, for example:

- `4 need review`

It opens Review. If there are no review items, it disappears.

### Money In

Incoming refunds, rewards, and income remain separate from the gross outgoing total. They never reduce `Spent this month` and should not compete with the hero.

Use:

- `Money in` or a specific credit category such as `Refunds`.
- A count or amount when useful.
- A tap into the exact supporting transactions.

### Empty State

Empty Home should still answer the four questions:

- Gross outgoing total is zero or unknown.
- No payments found yet.
- One action: connect SMS/Gmail or add manually.
- Privacy line: `Stays on this phone`.

## Review

Review is a short, calm correction flow.

### Purpose

Review answers:

- Which transactions need the user?
- Why?
- What is the quickest fix?

### Structure

Prefer one transaction at a time or a short finishable list.

Each review item shows:

- Merchant.
- Amount.
- Category/date subtitle.
- One reason: `Amount unclear`, `New place - which category?`, `Looks like a transfer`.
- Two or three clear actions to correct category, transaction type, or amount, or `Keep as is`.

Changing a category or type classifies an outgoing debit but does not remove it from `Spent this month`. Correcting its amount updates the headline and transaction list together.

Progress can read `3 of 4`.

### Completion

When review is complete:

- Show `Nothing needs review`.
- Show the current month's spend below it.
- Offer no extra ceremony.

## Insights

Insights is where the user explores the month.

### Purpose

Insights answers:

- Which categories mattered?
- Which merchants mattered?
- What changed from last month?
- What repeats?
- What money came back?
- Are imports healthy?

### Structure

Use modern finance sections with tape-native proof:

- All categories as share bars.
- Top merchants as ranked rows.
- Spending by month as a simple bar strip.
- Biggest changes as short facts.
- Recurring payments.
- Refunds/rewards/income as separate `Money in` sections, never netted against gross outgoing payments.
- Imports as a coverage section.

Every amount, percent, and count opens filtered transactions.

### Charts

Use horizontal bars and strips. Avoid donuts and decorative chart widgets.

### Language

Use:

- `Categories`
- `Merchants`
- `Recurring`
- `Changed`
- `Money in`
- `Imports`

Avoid:

- `Where it went`
- `Who took it`
- `Held`
- `Source health`
- `Indexed`

## Transaction Detail

Transaction Detail is where trust is earned.

### Purpose

Transaction Detail answers:

- What happened?
- Why did Sorted categorize it this way?
- What can I fix?
- Where did it come from?

### Structure

Show:

- Amount.
- Merchant.
- Date.
- Category.
- Account/payment mode if known.
- Import source.
- Original alert text on a subtle tape strip.

Actions:

- `Edit transaction`
- `Change category` or `Change payment type`
- `Make a rule`

Category and type remain classifications, not switches for inclusion in `Spent this month`. An eligible outgoing debit remains in the gross total after a category/type edit. Keep the headline and its filtered list reconciled after amount edits.

If edited, show `Edited by you`.

Avoid:

- `Amend`
- `Restamp`
- `Line`

## Add

Add is for cash and transactions no alert covers.

### Purpose

Add answers:

- What went out?
- Where?
- Which category?

### Structure

Optimized path:

1. Amount.
2. Merchant/place.
3. Category.
4. Done.

Everything else is optional. Added transactions get `Added by you` so the record remains honest.

## Settings

Settings is local control.

### Purpose

Settings answers:

- What does Sorted read?
- What stays on this phone?
- What can I export or delete?
- What imports are enabled?
- What rules has Sorted learned?

### Sections

Recommended order:

- Privacy.
- Imports.
- Auto-sorting rules.
- Categories.
- Appearance.
- Export or delete data.
- About.

### Privacy

Say plainly:

- No bank login.
- No cloud account.
- Transaction data stays on this phone.
- Exports happen only when the user asks.

## Auto-Sorting Rules

Auto-sorting rules are learned from user corrections.

### Purpose

Rules answer:

- What has Sorted learned from me?
- Which transactions does this affect?
- Can I change or turn it off?

### Structure

Use plain user language:

- `Swiggy -> Food`
- `34 payments`
- `Applied to past payments`

Each rule opens to:

- Change category.
- Apply to past payments.
- Turn off.
- View affected transactions.

Rules should be created from real corrections, not an empty rule-builder.

## Imports

Imports cover SMS, Gmail, and manual additions.

Show:

- On/off status.
- Last read time.
- Payment count.
- Review count.
- Permission status.

Use `Imports`, not `Source health`.

## Search And Months

Search and month navigation are utility surfaces.

Search supports:

- Merchant.
- Category.
- Amount.
- Date.
- Import.
- Type.

Month navigation should make it easy to compare this month with previous months without replacing Insights.

## Heavy Data State

For large histories:

- Keep Home fast.
- Show recent rows first.
- Use pagination or collapsed days below the fold.
- Avoid rendering raw alert details in lists.

## Copy Guardrail

If a label sounds like a parser, database, accountant, or receipt metaphor, simplify it before shipping.
