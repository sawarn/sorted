# Sorted Design Philosophy

## Core Thesis

Sorted is a private money tape.

The app should feel like a continuous, local receipt of the user's money life. Transaction alerts arrive on the phone, are parsed into lines, stamped with category and source, and printed back as an inspectable monthly tape. A total is never just a dashboard number. It is a close-out block derived from visible lines. A correction is not a silent database edit. It is an amendment or re-stamp. A source is not backend plumbing. It is receipt metadata.

The design should make one promise repeatedly:

> Every important number can show the lines that created it.

## Principles

### 1. Nothing Is Asserted

Every printed total must be able to open the included lines, excluded lines, uncertain lines, and source coverage behind it. If the app cannot show the evidence for a number, it should not print the number.

### 2. Append, Amend, Re-stamp

Corrections should feel like amendments to a record. The current value can be clean, but the UI should preserve trust by making clear when a line was corrected, learned from, ignored, or held out.

### 3. Doubt Is Printed

Unparsed messages, low-confidence categories, possible duplicates, raw UPI handles, Gmail-only high-value rows, and unclear money movement must remain visible. Sorted should ask calmly rather than pretend.

### 4. Held Out, Not Hidden

Transfers, investments, refunds, rewards, income, and other non-spend movement should not disappear. They should print in separate sections with clear reasons so users understand why they are excluded from spend.

### 5. The Tape Is The Source Of Truth

Home, Insights, Capture, Rules, and Settings can have different structures, but they should all feel like different ways of reading, indexing, printing, or controlling the same tape.

### 6. Private By Material

The product should feel local, inspectable, and owned by the user. Receipt paper, ink, stamps, margins, ruled lines, source marks, and printed metadata support the privacy story better than abstract dashboards.

### 7. Premium Through Restraint

The tape metaphor should be tactile but not childish. Avoid cartoon skeuomorphism. Use exact spacing, high-quality type, tabular numbers, quiet motion, and disciplined marks.

## Emotional Target

Sorted should feel:

- Calm
- Private
- Exact
- Tactile
- Trustworthy
- Slightly retro
- Quietly intelligent

It should not feel:

- Like a bank app
- Like a crypto dashboard
- Like a budget tracker
- Like a generic analytics dashboard
- Like a gamified expense toy
- Like a receipt gimmick

## Visual Language

### Overall Aesthetic

Sorted should look like a premium printed record rendered on a phone. The base language is receipt tape plus editorial ledger: lines, day dividers, stamps, close-out blocks, small metadata, and clear totals.

The app can use blocks and grouped sections, especially in Insights and Rules, but those blocks should feel like printed sections, index slips, folded tape, or stamped panels rather than floating dashboard cards.

### Dark Mode

Dark mode is the primary personality:

- Ground: near black or deep ink-blue-black.
- Tape: low-contrast charcoal, warm off-black, or subtle paper strip when needed.
- Ink: warm off-white.
- Accent: amber/yellow spectrum for active totals, review marks, and important stamps.
- Secondary marks: muted green, rust, violet, and blue only when they carry meaning.

Dark mode should feel like a receipt on a dark desk, not like neon fintech.

### Light Mode

Light mode can feel warmer and more physical:

- Ground: warm paper, not sterile white.
- Tape: off-white or soft ivory.
- Ink: deep brown-black or charcoal.
- Accent: amber/rust.
- Category colors can be more expressive, but never become the only source of meaning.

Light mode should feel like paper in daylight, not like a colorful budgeting app.

### Typography

Use a clean, highly readable sans for most UI. Pair it with tabular numeric styling everywhere amounts appear. A monospaced or mono-adjacent style may be used for receipt metadata, source rows, rule patterns, and printed summaries, but avoid turning the entire app into a terminal.

Recommended hierarchy:

- Month total: large, tabular, calm.
- Section totals: medium, tabular.
- Transaction merchant: readable body size.
- Metadata and stamps: small, uppercase or compact.
- Raw SMS excerpts: mono or mono-adjacent, subdued.

### Number Styling

Numbers are the product's promises. They should align, scan, and open.

Rules:

- Use tabular numerals.
- Right-align amounts in transaction lines.
- Keep currency formatting consistent.
- Never show a total without a tap target or drilldown path.
- Use signs and labels for credits, refunds, and held-out rows.

### Texture

Texture should be subtle and functional:

- Very light paper grain is acceptable.
- Faint ruled lines are useful.
- Perforation, torn edges, and thermal-printer effects should be used sparingly.
- Avoid decorative distress that harms readability.

The metaphor should support trust, not perform nostalgia.

## Marks And Stamps

Sorted should use stamps instead of generic chips wherever possible.

Stamp types:

- Category stamp: FOOD, SHOPPING, BILLS, TRANSPORT.
- Type stamp: SPEND, MOVED, INVESTED, REFUND, REWARD, INCOME.
- Source stamp: SMS, GMAIL, MANUAL.
- Confidence stamp: CHECKED, QUERY, FALLBACK, DUPLICATE?.
- Rule stamp: LEARNED, DEFAULT, USER.
- State stamp: HELD OUT, IGNORED, AMENDED.

Stamp rules:

- Stamps must remain legible at small sizes.
- Stamps should not rely on color alone.
- Query and review states should be calm, not alarming.
- Learned/user-created stamps should feel owned and inspectable.

## Motion

Motion should feel like printing, sliding, stamping, folding, or revealing.

Use:

- Short expansion for tape sections.
- Small stamp press for corrections.
- Gentle line insertion when a transaction is added.
- Vertical tape movement for month switching.
- Subtle haptics for print, stamp, undo, and section close-out.

Avoid:

- Bouncy finance-app animations.
- Floating particle backgrounds as default UI.
- Motion that makes the tape feel unstable.

## Anti-Patterns

Avoid these unless there is a very strong product reason:

- Dashboard cards as the default layout.
- Donut charts, large pie charts, or decorative graphs above the fold.
- Totals without evidence.
- Hidden exclusions.
- Red warning styling for ordinary uncertainty.
- Generic "Insights" widgets that could exist in any finance app.
- Overly literal receipt skeuomorphism that reduces readability.

## Core Test

For any new Sorted screen or component, ask:

1. Could this plausibly be printed, stamped, indexed, or folded from a private money tape?
2. Does every important number open the lines behind it?
3. Are uncertainty and exclusions visible?
4. Does the screen feel local and inspectable rather than cloud-generated and magical?

If the answer is no, the design has drifted.
