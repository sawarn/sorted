# Sorted Agent Guidance

This repo contains a product-specific design philosophy. Before changing any user-facing UI, design system, screen structure, copy, or UX flow, read:

1. `docs/design-philosophy.md`
2. `docs/design-section-scaffold.md`
3. `docs/ai-design-guidance.md`

The governing concept is:

> Sorted is Modern Finance Tape.

Preserve this as the single design philosophy. Sorted should feel like a clean modern finance app on the surface and a private money tape underneath: fast to understand, plain-spoken, local, and checkable.

Important invariants:

- Every important number should open the transactions behind it.
- Uncertainty should be visible and calm.
- Transfers, investments, refunds, rewards, and income should be separated from spend.
- Refunds are signals until matching is mature; do not silently net them out.
- Home should be understood in three seconds.
- Use normal user language: spent, payments, need review, top spending, recent spending, not counted, imports, edit transaction, auto-sorting rules.
- Avoid old internal UI words: lines, unstamped, query, ledger, close-out, restamp, held out, source health.
- Light mode is Cardamom Press; dark mode is Deep Ink.
- Amber/clay is reserved for review/action states.
- Settings is local device, privacy, imports, rules, export/delete, and appearance control.

There is also a repo-local Codex skill at `.codex/skills/sorted-design-philosophy/SKILL.md` for design and UI work.
