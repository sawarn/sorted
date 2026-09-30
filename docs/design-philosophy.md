# Sorted Design Philosophy

## Core Thesis

Sorted is **Modern Finance Tape**.

Sorted should feel like a clean modern finance app on the surface and a private money tape underneath. The surface answers fast. The tape lets the user check any number down to the transaction that made it.

The user should understand Home in three seconds:

- How much did I spend?
- Where did it go?
- What needs review?
- What happened recently?

Three commitments never bend:

- **Transactions are the truth.** Every total is a sum of transactions the user can open and read.
- **Spent this month is gross outgoing money.** Every eligible outgoing debit contributes, regardless of category or transaction type, including transfers and investments. Category and type explain the debit; they do not remove it from the headline. Incoming refunds, rewards, and income stay separate and are never netted against this gross total.
- **Doubt is shown calmly.** When Sorted is unsure, it says so in one amber mark and offers one tap to fix it. It never guesses quietly and never panics.

`Spent this month` is a gross outgoing-debit measure, not pure consumption. Every total must reconcile with the exact transaction list behind it.
Only completed debits with a positive amount and a usable INR value enter the total. Payments waiting on status or currency conversion remain visible for review and are not silently dropped.

## Product Personality

Sorted is the quiet, careful friend who keeps good records.

It is:

- Calm
- Precise
- Plain-spoken
- Private
- Checkable
- One-handed
- Fast to read
- Patient with mistakes

It is not:

- Playful
- Gamified
- Scolding
- Chatty
- Clever for its own sake
- Dense
- Accountant-flavoured
- Begging for attention

## Visual Principles

### 1. One Number Owns The Screen

The month's spend is the largest thing on Home by a wide margin. Nothing competes with it.

### 2. Rules Instead Of Boxes

Sections are separated by hairlines, paper tone changes, and spacing rather than stacks of rounded cards.

### 3. Printed Rhythm

Rows repeat at a steady height with aligned numbers, so the eye scans down a column like a printed strip.

### 4. Paper, Lightly

Use warm paper surfaces, faint ruled lines, and small tape cues. Texture stays subtle and functional, never decorative.

### 5. Amber Is The Only Interruption

Amber means "needs review" or a direct action related to review. It appears sparingly and should not become a general highlight color.

### 6. Bars, Not Pies

Share of spending is a plain horizontal bar with the percentage in type. Avoid donuts, radial charts, neon gradients, and decorative analytics.

### 7. Thumb-First Layout

The most-used actions and first correction path must be reachable one-handed. Primary actions should sit in the lower half when possible.

## UX Principles

### 1. Every Total Is A Door

Tapping any amount, percentage, or count opens the transactions behind it, already filtered.

### 2. One Tap To Correct

Wrong category, transaction type, amount, or unclear merchant should be fixable from the row itself. Category or type corrections do not change inclusion in `Spent this month` while a transaction remains an eligible outgoing debit. Correcting its amount updates the applicable total and transaction list together.

### 3. Review Is A Short Pile

Sorted shows how many transactions need review and makes the pile feel finishable. Finishing review should leave the screen calm and empty.

### 4. Keep Money In And Money Out Distinct

All eligible outgoing debits contribute to `Spent this month`, including transfers and investments. Incoming credits such as refunds, rewards, and income remain visible in separate `Money in` views and never silently reduce the gross outgoing total.

### 5. Teach By Doing

Fixing a transaction may offer to make an auto-sorting rule. Rules come from real corrections, not blank rule-builder work.

### 6. Privacy Is Stated Once, Gently

Use a quiet line such as "Stays on this phone" and a clear Settings page. Do not plaster privacy badges everywhere.

### 7. Empty States Still Answer The Question

No payments yet, imports paused, permission missing, or Gmail not connected should explain what is known, what is missing, and the one action that fixes it.

## Information Hierarchy

Home is ordered by four questions, always:

| Rank | Question | What Shows | Weight |
| --- | --- | --- | --- |
| 1 | How much went out? | Spent this month, outgoing payment count | Largest type |
| 2 | What needs review? | Amber stamp with count | Small but visible |
| 3 | Where did it go? | Top spending, three bars | Mid-weight rows |
| 4 | What happened recently? | Recent spending rows | Scan list |
| 5 | Did money come back? | Credits such as refunds, rewards, and income, when present | Secondary |

Below the fold, Home should repeat the same pattern rather than introduce new concepts.

## Language Rules

Use sentence case, short phrases, and familiar money words. If a word needs explaining, it is the wrong word.

| Say | Never Say | Why |
| --- | --- | --- |
| Spent this month | Total outflow, debits | Familiar headline; clarify it is gross outgoing money, not pure consumption |
| Payments | Lines, entries, records | A payment is a thing that happened |
| Need review | Unstamped, query, pending action | Says who acts and why |
| Money in | Net spend, offsets | Keeps incoming credits distinct from outgoing payments |
| Top spending | Category breakdown, spend mix | Everyday phrasing |
| Recent spending | Latest activity feed | Matches the section |
| Imports | Sources, source health, sync | Describes what the user set up |
| Edit transaction | Amend, restamp, reclassify | Ordinary app language |
| Auto-sorting rules | Ledger rules, mapping logic | Says what it does |
| Stays on this phone | On-device, zero-knowledge, E2E | Privacy in believable words |

Shape rules:

- Section labels are one or two plain words.
- Buttons are verbs: Review, Edit, Add, Keep, Correct transaction.
- Amounts on Home always carry `₹` and thousands separators, without decimals.
- Uncertainty fits one calm line: "Amount unclear", "New place - which category?", "Looks like a transfer".
- Never use an error tone for ordinary uncertainty.

## Color And Material Direction

Sorted uses **Cardamom Press** in light mode and **Deep Ink** in dark mode.

### Light: Cardamom Press

- App background: pale sage paper.
- Main surface: soft green-white.
- Section band: muted sage.
- Primary ink: deep pine.
- Muted text: grey-sage.
- Review/accent: clay amber.
- Category bars: pine, muted teal, clay, and restrained secondaries.
- Credit/refund: calm green, separate from review.

Implementation tokens:

```text
appBackground   #F6F8F2
mainSurface     #F6F8F2
sectionBand     #E9EFE2
primaryText     #17241E
mutedText       #566A5E
softFill        rgba(23,36,30,0.07)
rule            rgba(23,36,30,0.13)
faintRule       rgba(23,36,30,0.09)
review          #A9522A
reviewDot       #C0642F
categoryOne     #17241E
categoryTwo     #4E8471
categoryThree   #A9522A
credit          #4E8471
```

### Dark: Deep Ink

- App background: near-black green ink.
- Main surface: deep ink green.
- Section band: darker teal-green.
- Primary ink: cool paper.
- Muted text: desaturated sage-blue.
- Review/accent: warm amber.
- Category bars: cool paper, muted teal, amber, and restrained secondaries.
- Credit/refund: soft teal-green.

Implementation tokens:

```text
appBackground   #05110F
mainSurface     #0D2522
sectionBand     #123330
primaryText     #E7F0EC
mutedText       #93AAA4
softFill        rgba(231,240,236,0.08)
rule            rgba(231,240,236,0.13)
faintRule       rgba(231,240,236,0.08)
review          #D79A3F
reviewDot       #D79A3F
categoryOne     #E7F0EC
categoryTwo     #7FB3A4
categoryThree   #D79A3F
credit          #7FB3A4
```

Material rules:

- Ink carries every number.
- Muted carries labels and metadata.
- Amber carries review only.
- Green appears only on money coming back: refunds, rewards, income.
- No shadows except a single soft shadow on a sheet that slides over content.
- No gradients, glass, glow, or crypto-dashboard light.
- Body text must meet readable contrast in light and dark modes.

## Typography Direction

Use one clean modern sans family throughout. Amounts use tabular numerals.

Recommended hierarchy:

- Hero amount: very large, 600 weight, tight line height.
- Screen title: medium, calm, readable.
- Row title: merchant/category name, 500 weight.
- Row amount: right-aligned, 600 weight, tabular.
- Meta: small muted text.
- Section label: small, tracked, restrained.
- Review stamp: small uppercase, amber, used sparingly.

Avoid turning the whole app into a terminal. Monospace belongs only in raw alert text or debug-like source views.

## Component System

The core components:

- **Month header:** wordmark plus month selector.
- **Hero total:** label, big amount, payment count. One per screen, tappable.
- **Review stamp:** dashed amber outline, dot, count. Hidden at zero.
- **Share bar row:** category, 8px bar, percent.
- **Transaction row:** initial or icon, merchant, category/date subtitle, amount.
- **Tape band:** full-width section band with hairlines. Use sparingly.
- **Section label:** small plain label above a list.
- **Count line:** Payments, Money in, Need review.
- **Choice sheet:** bottom sheet with plain options.
- **Rule card:** "Swiggy -> Food", count of payments.
- **Bottom bar:** plain words, no badges unless review truly requires it.

If a screen needs many more component types, simplify the screen.

## Motion

Motion should be quiet and functional:

- Sheets rise.
- Lists cross-fade.
- A cleared review fades its stamp out.
- Added transactions insert into the list.
- Month changes should feel like replacing a statement, not spinning a carousel.

Use 200-300ms ease-out motion. Nothing bounces.

## Anti-Patterns

Avoid:

- Donut charts.
- Stacked generic dashboard cards.
- Neon gradients.
- Glass panels.
- Mascots, streaks, or scores.
- A total the user cannot open.
- A number without transactions behind it.
- Netting incoming refunds, rewards, or income against gross outgoing payments.
- Red for ordinary uncertainty.
- More than two amber review marks on one screen.
- Any word from the "Never Say" column.
- Explaining the app on the app.
- Heavy paper texture, torn edges, coffee stains, typewriter type.
- Silent auto-fixes.

## Design Review Checklist

A screen ships when every line is true:

- It answers its question in three seconds.
- One number is clearly the biggest thing on screen when the screen is summary-led.
- Every total, count, and percent opens its transactions.
- `Spent this month` is the gross sum of eligible outgoing debits, regardless of category or transaction type.
- Incoming credits are separate and do not reduce the gross outgoing total.
- Category/type corrections alone do not change whether an outgoing debit contributes to the headline.
- Every headline, category, merchant, and credit total reconciles with its tappable transaction list.
- Amber appears at most twice and only for review.
- Uncertainty is stated in one calm line with one tap to fix.
- No word from the "Never Say" column appears.
- Labels use plain words.
- Primary actions are reachable and at least 44px.
- Tape cues are present but subtle.
- Body text passes contrast in light and dark.
- Nothing implies a server, account, or cloud sync.
- Empty, loading, and paused states still answer the screen's core question.
- The screen can be used one-handed on a train.
