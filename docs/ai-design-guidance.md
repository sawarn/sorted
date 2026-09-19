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

Sorted is a private money tape.

Everything else follows from that.

## Current Product Shape

Sorted is a privacy-first Android app for India. It reads transaction alerts locally, extracts transaction lines, categorizes them, and helps the user understand the month without uploading financial data or linking bank accounts.

The core promise is:

> Your transactions, categorized privately.

The north star is:

> Every monthly total should be explainable from the tape.

## Design Decisions Already Made

- The governing concept is Tape.
- Dispatch-style AI summaries are allowed, but they belong as a brief generated from the tape.
- Merchant/category block layouts are allowed, but they must look like tape indexes, printed sections, or stamp ledgers.
- The app should not become a generic dashboard.
- Charts are secondary and should be transformed into tape-native forms when possible.
- Rules are saved stamps.
- Capture is adding a missing line.
- Settings is device/privacy/source control for a local record.

## Invariants

Do not violate these without explicit user direction:

- Every important number should open evidence.
- Uncertainty must remain visible.
- Transfers and investments are held out, not hidden.
- Refunds are signals until matching is mature; do not silently net them out.
- User corrections override parser/category defaults.
- Learned rules should be inspectable.
- No bank-linking, login, or cloud-sync assumptions for MVP.

## UI Language

Prefer:

- Receipt tape.
- Printed lines.
- Day dividers.
- Close-out total blocks.
- Stamps.
- Margins.
- Amendments.
- Source metadata.
- Tape indexes.

Avoid:

- Generic cards as the primary pattern.
- Donut charts and decorative dashboard widgets.
- Finance-bro visuals.
- Crypto-style glowing dashboards.
- Overly literal receipt gimmicks.
- Color-only meaning.

## Section Mapping

- Home: live monthly tape.
- Insights: indexed and explained tape.
- Capture: print a missing line.
- Rules: saved stamps and learned printing rules.
- Settings: local device, privacy, source, export, and permission controls.

## Implementation Notes For Android

When implementing in Jetpack Compose:

- Keep transaction rows virtualized with lazy lists.
- Use tabular numerals for amounts where the font supports them.
- Style stamps as accessible text components, not decorative images.
- Give stamps and source marks semantic labels.
- Keep texture subtle or avoid it until performance and readability are proven.
- Prefer deterministic local UI states over magical AI claims.
- Ensure dark mode is first-class.

## Design Review Checklist

Before considering a design complete, check:

- Does the screen still feel like part of the private money tape?
- Does each total expose its source lines?
- Are query/review states visible and calm?
- Are held-out rows explained?
- Can the user understand what Sorted learned?
- Does the UI work with only a few transactions?
- Does it still work with thousands?
- Is the metaphor helping comprehension rather than decorating it?
