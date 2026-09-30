---
name: sorted-design-philosophy
description: Use when designing or implementing Sorted UI, screens, components, UX flows, visual systems, copy, color palettes, or design docs so work follows the Modern Finance Tape philosophy.
---

# Sorted Design Philosophy

Use this skill for Sorted product design and UI implementation work.

Sorted's governing design concept is:

> Sorted is Modern Finance Tape.

Sorted should feel like a clean modern finance app on the surface and a private money tape underneath. The surface answers fast. The tape-like layer makes every number checkable from local transactions.

## Required Reading

Before making design or UI decisions, read these repo docs:

1. `docs/design-philosophy.md`
2. `docs/design-section-scaffold.md`
3. `docs/ai-design-guidance.md`

For product scope and roadmap context, also read:

- `docs/feature-roadmap.md`
- `docs/features.md`

When a bundled design HTML file exists for the screen, follow `.codex/skills/sorted-design-html/SKILL.md` and implement the extracted phone frame as drawn. Do not implement from the wrapper thumbnail.

## Non-Negotiables

- Home must be understood in three seconds.
- Every important total, count, and percent must open the transactions behind it.
- Every eligible outgoing debit contributes to `Spent this month`, regardless of category or transaction type.
- Transfers and investments remain identifiable categories within outgoing payments.
- Incoming refunds, rewards, and income stay separate and are never netted against the outgoing total.
- Refunds are signals until matching is mature; do not silently net them out.
- Uncertainty is shown calmly with review language, not error language.
- User corrections override parser/category defaults.
- Auto-sorting rules are learned from real corrections and must be inspectable.
- The product must feel local and private: no cloud, account, or bank-linking assumptions for MVP.

## Current Palette Direction

Light mode is **Cardamom Press**:

- App background: `#F6F8F2`
- Main surface: `#F6F8F2`
- Section band: `#E9EFE2`
- Primary text: `#17241E`
- Muted text: `#566A5E`
- Review/accent: `#A9522A`
- Review dot: `#C0642F`
- Credit/refund: `#4E8471`

Dark mode is **Deep Ink**:

- App background: `#05110F`
- Main surface: `#0D2522`
- Section band: `#123330`
- Primary text: `#E7F0EC`
- Muted text: `#93AAA4`
- Review/accent: `#D79A3F`
- Credit/refund: `#7FB3A4`

Amber/clay review color is reserved for "needs review" and direct review actions.

## UI Language

Use plain, familiar words:

- Spent this month
- Payments
- Need review
- Top spending
- Recent spending
- Money in
- Imports
- Edit transaction
- Auto-sorting rules
- Stays on this phone

Avoid old internal/tape terms in user-facing UI:

- Lines
- Unstamped
- Query
- Ledger
- Close-out
- Restamp
- Held out
- Source health
- Reclassify

Tape remains in the material, rhythm, proof, and interaction model, not in confusing vocabulary.

## How To Adapt Screens

- Home: three-second understanding of the gross outgoing total and where it went.
- Review: short pile of transactions needing action.
- Insights: categories, merchants, changes, recurring, money in, imports.
- Add: amount-first manual transaction entry.
- Transaction Detail: original alert, edit actions, and trust details.
- Settings: privacy, imports, auto-sorting rules, export/delete, appearance.
- Auto-sorting Rules: learned corrections in the user's own words.

## Visual Rules

Prefer:

- Modern finance layout.
- Large readable numbers.
- Warm paper/sage surfaces.
- Deep pine or cool paper ink.
- Thin rules.
- Subtle tape bands.
- Small amber review stamps.
- Horizontal share bars.
- Tabular amounts.
- One-handed spacing.

Avoid:

- Dense receipt cosplay.
- Heavy paper texture.
- Torn edges as decoration.
- Typewriter/terminal styling as the default.
- Generic stacked dashboard cards.
- Donut charts.
- Neon gradients.
- Glass panels.
- Crypto-dashboard visuals.
- Red warning states for ordinary uncertainty.

## Review Test

Before finishing design work, ask:

1. Can the user understand the screen in three seconds?
2. Is the primary number obvious?
3. Does every total/count/percent open its transactions?
4. Do all eligible outgoing payments contribute, independent of category?
5. Is uncertainty visible without alarm?
6. Is amber used only for review/action?
7. Is the copy plain enough for a normal user?
8. Does the screen feel modern first and tape-like underneath?
9. Does it feel local and private?

If not, revise the design.
