---
name: sorted-design-html
description: >-
  Extract and implement Sorted screens from bundled design HTML files such as
  Sorted Home Screen.html and Sorted Logo and Loading.html. Use when
  implementing, matching, or comparing a Sorted screen against a design HTML
  file, a phone frame, Cardamom Press, Deep Ink, or when the user says to
  follow the HTML as it is.
---

# Sorted Design HTML

Implement the screen that is inside the bundled design file. Do not redesign it, and do not treat the outer thumbnail as the design.

Also follow `.codex/skills/sorted-design-philosophy/SKILL.md` for product rules. This skill is only about reading the HTML accurately and matching it.

## What the file actually is

Files such as `Sorted Home Screen.html` and `Sorted Logo and Loading.html` are bundler wrappers.

- The SVG in `#__bundler_thumbnail` is a preview. Ignore it.
- The design is a JSON string inside `<script type="__bundler/template">`.
- That string is a second HTML document: phone frames, inline styles, copy, and CSS keyframes.
- `data-screen-label` names a screen, for example `Loading · Cardamom Press`.
- `{{ tokens }}`, `sc-for`, and `sc-if` are bindings. The visible frame still states the structure. A `renderVals` script near the end of the template computes sample numbers.

## Extract before reading

Run this from the repo root. Do not grep the wrapper and guess.

```bash
python3 .codex/skills/sorted-design-html/scripts/extract_design_html.py "Sorted Home Screen.html" --list
python3 .codex/skills/sorted-design-html/scripts/extract_design_html.py "Sorted Home Screen.html" --copy
python3 .codex/skills/sorted-design-html/scripts/extract_design_html.py "Sorted Home Screen.html" --screen "Top spending"
python3 .codex/skills/sorted-design-html/scripts/extract_design_html.py "Sorted Logo and Loading.html" --keyframes
```

`--screen` prints the inline-styled markup around a heading or `data-screen-label`. Read that markup. It is the spec.

There is a light frame and a dark frame. Implement both. Cardamom Press is light. Deep Ink is dark.

## Write a spec, then code

Before editing the app, write down the chosen frame in this order:

1. Region order from top to bottom.
2. Exact visible copy, including case. `text-transform: uppercase` means the app shows uppercase.
3. For each text node: `font-size`, `font-weight`, `letter-spacing`, `line-height`, color.
4. For each block: width, height, padding, gap, radius, background.
5. Animation name, duration, easing, delay, and keyframes.
6. What a tap opens, if the frame has a click binding.

Then implement that list. A section that exists in the frame exists in the app, including its title, meta text, caption, and stated height. Below-the-fold content is still part of the screen.

## Map values directly

- CSS px in these frames maps 1:1 to Android dp/sp. A 34px strip is 34.dp, not a thinner bar.
- `letter-spacing: .16em` at 11px is `1.76.sp`. Compose letter spacing is absolute, not em.
- Inter Tight is the UI face. Weight numbers in the frame are the weights to draw. The app font is variable: set the `wght` axis, or the weight will stay at the default cut.
- Colors are the hex values in the frame for that theme. Do not swap in a nearby palette color.
- Ignore the fake status bar and home indicator drawn inside the phone chrome.
- The frame is 412 by 892. Header and tab bar stay fixed when the markup puts them outside the scrolling region.

## Animations

Copy the keyframe. A loading tick that runs `om-tick` for 1.6s, ease-in-out, alternate, with a staggered delay, must breathe from opacity 0.22 to 1 in that order. A rule that runs `om-slide` must travel the distance in the keyframe, then back.

Use one continuous clock so the loop does not jump. Ease-in-out is `cubic-bezier(0.42, 0, 0.58, 1)`.

## When the script and the caption disagree

The phone frame and the "What changed, and why" captions are the design. `renderVals` is sample data plus prototype math.

If the prototype math contradicts the caption, follow the caption and the label a person would read. Example: the home caption says each merchant shows one tick per payment, while `renderVals` uses `Math.max(3, ...)`. One payment is one tick. Do not copy the minimum of 3.

## Done check

Match the built screen against the spec:

- Same regions, in the same order, with the same copy.
- Same sizes, weights, colors, and gaps.
- Light and dark both follow their own frame.
- Nothing important from the frame was dropped because it sat low on the page.
- The outer thumbnail SVG was not used as the logo or layout.
