# Sorted Agent Guidance

This repo contains a product-specific design philosophy. Before changing any user-facing UI, design system, screen structure, copy, or UX flow, read:

1. `docs/design-philosophy.md`
2. `docs/design-section-scaffold.md`
3. `docs/ai-design-guidance.md`

The governing concept is:

> Sorted is a private money tape.

Preserve this as the single design philosophy. Different screens may use different structures, but they should all feel like printed lines, stamps, amendments, close-out totals, source metadata, tape indexes, or controls for the same local record.

Important invariants:

- Every important number should expose the lines behind it.
- Uncertainty should be visible and calm.
- Transfers, investments, refunds, rewards, and income should be separated from spend.
- Refunds are signals until matching is mature; do not silently net them out.
- Rules are saved stamps.
- Capture is adding a missing line.
- Settings is local device, privacy, source, export, and permission control.

There is also a repo-local Codex skill at `.codex/skills/sorted-design-philosophy/SKILL.md` for design and UI work.
