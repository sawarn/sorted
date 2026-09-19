---
name: sorted-design-philosophy
description: Use when designing or implementing Sorted UI, screens, components, UX flows, visual systems, or design docs so work follows the private money tape philosophy.
---

# Sorted Design Philosophy

Use this skill for Sorted product design and UI implementation work.

Sorted's governing design concept is:

> Sorted is a private money tape.

The app should feel like a continuous, local receipt of the user's money life. Transaction alerts are parsed into lines, stamped with category and source, and printed back as an inspectable monthly tape. Totals must be explainable from visible lines. Corrections are amendments or re-stamps. Sources are receipt metadata.

## Required Reading

Before making design or UI decisions, read these repo docs:

1. `docs/design-philosophy.md`
2. `docs/design-section-scaffold.md`
3. `docs/ai-design-guidance.md`

For product scope and roadmap context, also read:

- `docs/feature-roadmap.md`
- `docs/features.md`

## Non-Negotiables

- Preserve Tape as the single design philosophy.
- Do not blend in unrelated dashboard, banking, budgeting, or crypto-app patterns.
- Every important number should open the lines behind it.
- Uncertainty should be printed calmly, not hidden.
- Transfers, investments, refunds, rewards, and income should be separated from spend.
- Refunds are signals until matching is mature; do not silently net them out.
- Rules should feel like saved stamps.
- Capture should feel like adding a missing line.
- Settings should feel like local device, privacy, source, and export controls.

## How To Adapt Screens

Different sections may use different structures, but they must still feel tape-native:

- Home: live monthly tape.
- Insights: indexed and explained tape.
- Capture: print a missing line.
- Rules: saved stamps.
- Settings: device and privacy receipt.

Blocks and tiles are allowed only when they feel like printed sections, tape indexes, stamp ledgers, folded slips, or merchant/category indexes from the same tape.

## Review Test

Before finishing design work, ask:

1. Could this plausibly be printed, stamped, indexed, or folded from a private money tape?
2. Does every important number expose evidence?
3. Are doubt and exclusions visible?
4. Does this feel local and inspectable rather than cloud-generated and magical?

If not, revise the design.
