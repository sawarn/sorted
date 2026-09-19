# Sorted Section Scaffold

This document applies the private money tape philosophy to the main app sections.

Read this with [design-philosophy.md](design-philosophy.md).

## Navigation

Sorted has five primary sections:

- Home
- Insights
- Capture
- Rules
- Settings

Use Android-friendly one-handed navigation. A bottom bar is acceptable, but the visual language should feel like a tape index rather than a generic app tab strip. Settings can remain a top-right control if product simplicity benefits from keeping the bottom bar to four items, but Rules should have a clear home once the feature becomes important.

Navigation labels should be plain. Avoid novelty names in the actual UI unless they improve comprehension.

## Home: The Live Monthly Tape

Home is the current month printed as a tape.

### Purpose

Home answers:

- What happened this month?
- What is counted as spend?
- What was held out?
- What does Sorted need me to review?
- Which lines created this number?

### Structure

Suggested order:

1. Month selector and source status.
2. Printed monthly spend close-out block.
3. Review/query strip if anything needs attention.
4. Day-grouped transaction tape.
5. Held-out sections for transfers, investments, refunds, rewards, and income.
6. Source footer for SMS, Gmail, Manual, last scan, skipped/unparsed count.

### Month Total

The monthly spend total should feel like a printed close-out block:

- Large tabular amount.
- Included count.
- Held-out total.
- Review risk amount.
- Source coverage.
- Tap opens the "why this number" breakdown.

Do not use a dashboard hero card. The close-out block is part of the tape.

### Transaction Line

Each line should include:

- Merchant or payee.
- Amount.
- Direction or type.
- Category stamp.
- Source stamp.
- Date/time or day grouping.
- Review/confidence mark if needed.

Optional details expand inline:

- Raw SMS/email excerpt.
- Parser notes.
- Correction history.
- Similar rule applied.
- Included/excluded reason.

### Day Grouping

Day dividers should feel like printed rules:

- Date label.
- Day spend total.
- Count.
- Optional held-out count.

### Included vs Excluded

Spend lines print normally. Held-out lines remain visible and carry a reason:

- HELD OUT - TRANSFER
- HELD OUT - INVESTMENT
- REFUND SIGNAL
- REWARD
- INCOME

Do not silently remove non-spend movement from Home.

### Uncertain Transactions

Uncertain rows should print as query lines:

- QUERY stamp.
- Plain reason.
- Suggested action.
- Tap opens correction.

Examples:

- Merchant unread.
- Category fallback.
- Spend or transfer?
- Possible duplicate.
- Gmail-only high value.

### Empty State

The empty state should look like an unprinted tape:

- Explain that no local transaction lines have been found yet.
- Offer SMS permission or manual capture.
- Reassure the user that data stays on device.

### Heavy-Data State

For large histories:

- Keep the current month fast.
- Collapse older days by default when needed.
- Provide search and month jump.
- Avoid rendering every expanded detail at once.

## Insights: The Tape Indexed And Explained

Insights should not become a chart dashboard. It is the tape indexed by question.

### Purpose

Insights answers:

- Where did spend concentrate?
- Which merchants mattered?
- Which categories mattered?
- What repeated?
- What was refunded or credited?
- How healthy are the sources?
- What story does the month tell?

### Structure

Use printed index blocks, not floating cards:

- Merchant index.
- Category index.
- Recurring index.
- Refund signals.
- Source health.
- Month story or AI brief.

Each block must open its supporting transaction lines.

### Merchant Views

Merchant intelligence can borrow the structural idea of merchant blocks or tiles, but the look should remain tape-native:

- Merchant name as a printed heading.
- Spend total.
- Count.
- Largest line.
- Average line.
- Source split.
- Category/type stamps.
- Recent lines.
- Review-needed lines.

### Category Views

Category pages should show:

- Category total.
- Merchant split.
- Payment mode split.
- Source split.
- Included lines.
- Query lines.

Use printed strips, ranked ledgers, or proportional rules instead of generic charts.

### Recurring Payments

Recurring should feel like a repeated print pattern:

- Merchant.
- Expected amount.
- Frequency.
- Last seen.
- Confidence.
- Lines behind the pattern.

### Refund Signals

Refunds should be shown as signals until matching is mature.

Important rule:

- Do not automatically reduce spend unless refund matching is proven.
- Print gross spend and refund signals separately.
- If a refund is matched later, show the match and the lines.

### Source Health

Source health is receipt metadata:

- SMS parsed.
- Gmail parsed.
- Manual lines.
- Unparsed/skipped lines.
- Last import.
- Review-needed by source.

Coverage gaps should be visible, not hidden in settings.

### Month Story / AI Brief

Dispatch-style prose belongs here or as a Home-adjacent summary, not as the whole app philosophy.

The brief should:

- Be clearly generated from the tape.
- Keep every number tappable.
- Mark itself as a NOTE or BRIEF, not as the source record.
- Avoid claims when the underlying lines are uncertain.
- Stay short enough to remain useful.

## Capture: Adding A Line

Capture is not a generic form. It is a blank line on the tape.

### Purpose

Capture answers:

- What line is missing?
- Where should it print?
- Which stamps should it carry?

### Structure

The editor should resemble the final line:

- Amount first.
- Merchant/payee.
- Date.
- Type stamp.
- Category stamp.
- Source fixed to MANUAL.
- Optional note.

The preview is the form.

### Fast Add

Manual capture should support:

- One-field amount entry.
- Recent merchants.
- Recent categories.
- Repeat last transaction.
- Suggested type from category.
- One-tap learned stamp selection.

### Commit

Adding a transaction should insert a line into the tape:

- The new line slides into chronological position.
- The day subtotal and month total reprint.
- A MANUAL stamp appears.
- Undo is available briefly.

## Rules: Saved Stamps

Rules are the stamps Sorted has learned.

### Purpose

Rules answers:

- What has Sorted learned from me?
- Which merchant/category/type stamps are automatic?
- What impact does a rule have?
- Can I edit or disable it?

### Structure

Use a stamp drawer or printed rule ledger:

- Rule trigger on the left.
- Applied stamp on the right.
- Hit count.
- Last applied.
- Source: USER, DEFAULT, IMPORTED, SYSTEM.
- Confidence.
- Example matched lines.

### Rule Types

Support:

- Merchant normalization rules.
- Category rules.
- Transaction type rules.
- Ignore rules.
- Source-specific rules if needed.

### Behavior

Rules should support:

- Enable/disable.
- Edit.
- Delete.
- View matched lines.
- View before/after impact.

No hidden categorization magic. If Sorted learns, the learned stamp should be visible somewhere.

## Settings: Device And Privacy Receipt

Settings should feel like controls and metadata for a local record, not a generic settings list.

### Sections

Recommended sections:

- Privacy promise.
- SMS permissions.
- Gmail source controls.
- Local storage.
- Export.
- Debug data boundaries.
- Theme.
- About/legal.

### Privacy

The privacy area should clearly say:

- No bank login.
- No cloud account required.
- Financial data stays on device.
- Raw messages are not exported unless the user explicitly chooses an export/debug action.

### Sources

SMS and Gmail controls should show:

- Permission status.
- Last scan.
- Parsed count.
- Skipped/unparsed count.
- Review-needed count.

### Export And Debug

Exports should be explicit and user-triggered. Debug exports must be clearly labeled and should avoid raw sensitive data by default.

## Search And Filters

Search is a way to find lines on the tape.

Support:

- Merchant.
- Category.
- Amount.
- Date.
- Source.
- Type.
- Review state.

Filters should feel like printed index tabs or stamp filters, not dashboard chips unless styled as stamps.

## Accessibility And Performance

The tape metaphor must not reduce usability.

Requirements:

- Minimum practical tap targets for all stamps and lines.
- Text contrast suitable for dark and light modes.
- Screen reader labels for amount, merchant, type, category, source, and review state.
- Reduced motion mode for print/stamp animations.
- Efficient list virtualization for transaction history.
- No texture or decorative mark should obscure text.
- Color is never the only signal.
