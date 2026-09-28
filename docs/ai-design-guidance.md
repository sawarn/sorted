# AI Design Guidance For Sorted

This file is for future AI agents working on Sorted design or UI implementation.

There is also a repo-local Codex skill at:

- `.codex/skills/sorted-design-philosophy/SKILL.md`

Before changing any user-facing design, read:

1. [design-philosophy.md](design-philosophy.md)
2. [design-section-scaffold.md](design-section-scaffold.md)
3. [feature-roadmap.md](feature-roadmap.md)
4. [features.md](features.md)

## One Sentence To Preserve

Sorted is **Modern Finance Tape**: a modern finance app on the surface, with private tape-like proof underneath.

## Current Product Shape

Sorted is a privacy-first Android app for India. It reads transaction alerts locally from SMS/Gmail, categorizes them, and helps the user understand the month without bank linking, login, cloud sync, or uploading financial data.

The core promise is:

> Your transactions, categorized privately.

The north star is:

> A user understands the month in three seconds, and every number can be opened.

## Design Decisions Already Made

- Home must be glanceable before it is inspectable.
- The tape metaphor is now material and proof, not visible jargon.
- Light mode uses **Cardamom Press**.
- Dark mode uses **Deep Ink**.
- Amber/clay is reserved for review/action states.
- Category bars are horizontal and restrained.
- Donut charts, neon gradients, glass panels, and stacked dashboard cards are out.
- Review uses plain language and a short finishable flow.
- Transactions remain the source of truth.
- Auto-sorting rules are learned from corrections.

## Invariants

Do not violate these without explicit user direction:

- Every important number opens the transactions behind it.
- Uncertainty must remain visible and calm.
- Spending includes only spending.
- Transfers, investments, refunds, rewards, and income are counted apart from spending.
- Refunds are signals until matching is mature; do not silently net them out.
- User corrections override parser/category defaults.
- Learned rules must be inspectable.
- No bank-linking, login, or cloud-sync assumptions for MVP.

## UI Language

Prefer:

- Spent this month.
- Payments.
- Need review.
- Top spending.
- Recent spending.
- Not counted.
- Imports.
- Edit transaction.
- Auto-sorting rules.
- Stays on this phone.

Avoid in user-facing copy:

- Lines.
- Unstamped.
- Query.
- Ledger.
- Close-out.
- Restamp.
- Held out.
- Source health.
- Reclassify.

Use sentence case for normal labels. Use uppercase only for tiny stamps such as `REVIEW`, and use those sparingly.

## Visual Language

Prefer:

- Modern finance layout.
- Large readable numbers.
- Warm paper/sage surfaces.
- Deep pine/ink text.
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
- Typewriter/terminal styling.
- Generic card dashboards.
- Decorative charts.
- Color-only meaning.
- Red warning states for ordinary uncertainty.

## Palette

Use semantic roles, not raw colors scattered through UI code.

Light mode: **Cardamom Press**

- App background: `#F6F8F2`
- Main surface: `#F6F8F2`
- Section band: `#E9EFE2`
- Primary text: `#17241E`
- Muted text: `#566A5E`
- Rule: `rgba(23,36,30,0.13)`
- Faint rule: `rgba(23,36,30,0.09)`
- Review: `#A9522A`
- Review dot: `#C0642F`
- Credit/refund: `#4E8471`

Dark mode: **Deep Ink**

- App background: `#05110F`
- Main surface: `#0D2522`
- Section band: `#123330`
- Primary text: `#E7F0EC`
- Muted text: `#93AAA4`
- Rule: `rgba(231,240,236,0.13)`
- Faint rule: `rgba(231,240,236,0.08)`
- Review: `#D79A3F`
- Credit/refund: `#7FB3A4`

## Section Mapping

- Home: three-second month understanding.
- Review: short pile of transactions needing user action.
- Insights: categories, merchants, changes, recurring, money back, imports.
- Add: amount-first manual transaction entry.
- Transaction Detail: original alert, editable categorization, trust surface.
- Settings: privacy, imports, rules, export/delete, appearance.
- Auto-sorting Rules: learned corrections in the user's words.

## Implementation Notes For Android

When implementing in Jetpack Compose:

- Keep transaction rows virtualized with lazy lists.
- Use tabular numerals where possible.
- Keep Home copy minimal.
- Put audit/source/parser details behind taps.
- Style review stamps as accessible text components.
- Give review marks semantic labels.
- Keep texture subtle until readability and performance are proven.
- Prefer deterministic local UI states over magical AI claims.
- Ensure dark mode is first-class, not an inverted afterthought.
- Avoid adding new color literals when a semantic palette token should exist.

## Design Review Checklist

Before considering design complete, check:

- Can the user understand the screen in three seconds?
- Is the biggest number obvious?
- Does every total/count/percent open its transactions?
- Does spending contain only spending?
- Is uncertainty visible without alarm?
- Is amber used only for review/action?
- Is the copy plain enough for a normal user?
- Are raw alert/parser/source details out of the first glance?
- Does it work with a few transactions and thousands?
- Does it feel modern first and tape-like underneath?
- Does it still feel local and private?
