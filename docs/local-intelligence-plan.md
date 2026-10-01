# Local Intelligence Plan

## Goal

Improve transaction detection, merchant understanding, categorization, and useful insights while keeping Sorted's financial data on the phone. Intelligence should make the current logic more adaptable, not become a second source of truth.

## Product contracts

- Transactions remain the source of truth; every total must reconcile with the transactions behind it.
- `Spent this month` is the gross sum of eligible completed outgoing debits, regardless of category or transaction type, including transfers and investments. Credits remain separate.
- User corrections and explicit local rules take priority over generic classification.
- Uncertain records remain visible for review; the app does not silently discard uncertainty or confidently invent details.
- Parsing and inference use local data only. No raw SMS, email, or transaction history is sent to a service.
- Every intelligence feature has a deterministic fallback and can be disabled without breaking imports or summaries.

## Delivery stages and gates

### Stage 0 — Establish a regression corpus and test runner

Add local JVM tests for the current pure Kotlin engine and a small, privacy-safe fixture corpus. Cover completed debits, credits, pending/failed records, investments/transfers, malformed and promotional messages, known false positives, and user-correction precedence where repository tests are practical.

**Gate:** tests run from the Android project; the accounting contract is captured in tests before engine behavior changes.

### Stage 1 — Make transaction eligibility one explicit policy

Route all headline and insight aggregates through one policy: completed, positive-amount outgoing debits count regardless of category/type. Credits are kept separate. Remove category- or type-based exclusions from the gross outgoing calculation.

**Gate:** policy unit tests pass; totals and lists use the same eligibility rule.

### Stage 2 — Make import decisions evidence-based

Separate extraction from the decision to accept an imported record. Capture source provenance and signals such as amount, direction, payment action, completion status, reference/account evidence, and promotional/link language. Return an explainable result: accepted, ignored, or needs review. Avoid merchant-specific false-positive blacklists; a real transaction from an unfamiliar sender must still be recoverable.

**Gate:** SMS and Gmail fixture tests cover both actual payment alerts and lookalike messages; previously corrected or ignored records remain stable after re-import.

### Stage 3 — Normalize merchant and category inputs

Create reusable normalization for casing, punctuation, UPI suffixes, gateway prefixes, and common identifier noise. Keep merchant identity, user-facing category, and transaction type as separate fields. Keep a small category vocabulary with a safe `Other` fallback instead of growing special category exceptions.

**Gate:** normalization fixtures pass across SMS/Gmail variants; categories and transaction types never affect gross outgoing eligibility.

### Stage 4 — Personalize locally from user actions

Use explicit corrections as the strongest signal and saved rules as inspectable overrides. Similarity-based suggestions may use only local corrected examples and should be confidence-gated. Corrections must survive rescans and not rewrite unrelated merchants.

**Gate:** tests prove precedence, stability across import, deterministic matching, and a review fallback for uncertain suggestions.

### Stage 5 — Add deterministic insights

Derive trends, recurring candidates, merchant rankings, and unusual changes from transaction queries with explicit date ranges and eligibility. Keep the calculated evidence attached to every insight so its amount opens the exact contributing transactions. The first wording can be templates; do not use a generative model for arithmetic.

**Gate:** aggregate tests prove each insight reconciles with its transaction set and behaves correctly for sparse history, credits, corrections, and month boundaries.

### Stage 6 — Add optional on-device model assistance

Only after the earlier stages are stable, evaluate a compact local classifier for category suggestions. Optionally use a supported on-device language model to explain precomputed insight facts. Validate model output against app-owned category/type enums, show uncertainty, and fall back when unsupported or unavailable. Never let model text change amounts, totals, source data, or user rules.

#### Runtime evaluation

- The app supports Android API 26+. ML Kit's Prompt API also has API 26 as its minimum, but it relies on Gemini Nano through AICore, requires feature-status checks and may need a model download. Prompt API support is limited to a changing list of device models, so it cannot be the only categorizer for Sorted. See [Prompt API setup and availability](https://developers.google.com/ml-kit/genai/prompt/android/get-started) and [current GenAI device support](https://developers.google.com/ml-kit/genai).
- LiteRT in Google Play services is available wherever a current Play services runtime exists, and input inference runs on-device. Its documentation also describes device, app, runtime, and performance metrics sent to Google. That needs to be considered against Sorted's privacy expectations before choosing it. See [LiteRT in Google Play services](https://developers.google.com/edge/litert/android/play_services).
- Do not use a generative model for totals, trend arithmetic, or transaction eligibility. Keep the first model task to a bounded category suggestion that can be validated against app-owned values and declined when confidence is low.
- Before adding a runtime, build a sanitized evaluation set from varied merchant/message patterns and user-correction examples, document category-level precision and abstention rates, and compare model size, cold-start latency, peak memory, and battery cost on API 26+ CPU-only hardware. Preserve a deterministic path for every device.
- Personalization belongs to the Android app profile that owns the local database. A shared starter engine may ship with the app, while accepted corrections remain in that profile's private database; no correction examples are synced or sent to a model service.

#### First implementation slice

1. Define a local category-suggestion interface and a result type that includes confidence and abstention; do not let suggestions write to the transaction table. **Done.**
2. Build deterministic fixtures and a baseline evaluator from privacy-safe examples. Include confusing same-merchant cases, investments/transfers, unknown merchants, and corrected examples. **Initial synthetic baseline done; adversarial coverage remains.**
3. Select a small classifier/runtime only if it beats the baseline at an agreed high precision while abstaining on uncertain records. Keep Gemini Nano as an optional explanation layer, not a dependency for core categorization.
4. Surface suggestions for explicit user acceptance, then add locally persisted training signals only after acceptance flow and correction precedence tests pass. **Done for exact normalized-merchant matches:** the detail view offers a local correction suggestion; accepting it writes the normal user correction audit. Note/status-only edits are not training examples, and contradictory local examples abstain.

**Gate:** supported-device checks, offline behavior, privacy review, output validation, battery/latency/storage measurements, and deterministic fallback all pass before enabling the feature by default.

## Work sequence

Implement one stage at a time. After each stage, run its focused tests and the Android build. If a gate fails, fix that stage before starting the next one. Do not add a model dependency until Stage 6.

## Current implementation pass

Completed in this pass:

- Added a JVM test runner and regression tests for outgoing totals, imports, merchant normalization, and local rule matching.
- Corrected `OutflowPolicy` so transaction type cannot hide an otherwise eligible completed debit.
- Tightened Gmail `Shop now` link handling so generic promotional mentions of a payment are not enough to import a transaction; explicit completed movement evidence still imports.
- Added shared merchant text normalization and applied it to categorization and saved-rule matching.
- Kept personalized matching limited to exact or user-authored contains rules; no fuzzy rule is applied automatically.
- Added a shared accepted/review/ignore assessment that separates transaction evidence from category confidence. Gmail can now retain a clear debit with an unfamiliar category for review instead of dropping it just because categorization is uncertain.
- Applied the same assessment in repository imports and protected corrected Gmail records from parser-driven deletion during rescan.
- Added `SpendAnalytics` as the shared, tested path for eligible totals, month totals, category groups, and merchant groups. Insight rows retain source hashes needed to open contributing payments.
- Replaced unused merchant-keyword recurring logic with date, amount, and normalized-merchant evidence. Added repeated-payment insights whose estimates open their source payments.
- Verified Stage 5 with JVM tests for investments and transfers in gross spend, credits and pending items excluded, month boundaries, recurring merchant normalization, and evidence totals. `:app:testDebugUnitTest` and `:app:assembleDebug` pass.
- Added a local category-suggestion contract with validated category/type/confidence output, explicit abstention, and a rule-based adapter for baseline measurement. It returns proposals only and is not connected to transaction writes or imports.
- Added a test-only evaluator for coverage, accuracy when suggesting, and per-category precision, with 14 synthetic examples. The current rule baseline suggests on 13 and is correct on those 13 controlled examples; this fixture score is not a real-world accuracy claim. The complete JVM suite and debug APK build pass.
- Added a correction-based suggestion engine backed by category changes in the current profile's private database. It learns exact normalized-merchant mappings only and abstains when the profile has conflicting corrections; the existing deterministic import categorizer remains unchanged.
- Added a transaction-detail suggestion card with an explicit “Use suggestion” action. The suggestion remains separate from saved transactions until accepted. Tests give two simulated profiles conflicting corrections for the same merchant and verify independent suggestions; the focused tests, full JVM suite, and APK build pass.

Next, add adversarial synthetic cases for ambiguous merchants, noisy descriptors, and category conflicts, then measure where the shared baseline and local correction learner abstain or misclassify. Use that evidence to choose a compact local classifier and benchmark its precision, coverage, model size, cold-start latency, memory, and offline behavior before adding a runtime dependency. Keep arithmetic and import decisions deterministic. The AEPS fixture distinguishes informational text from an explicit completed debit; if the on-device false positive has different alert wording, add that exact sanitized shape to the fixture corpus before changing its decision logic.
