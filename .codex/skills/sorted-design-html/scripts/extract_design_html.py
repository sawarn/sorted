#!/usr/bin/env python3
"""Extract the real screen markup from a Sorted bundled design HTML file.

The outer file is a bundler wrapper. Its thumbnail SVG is not the design.
The design document is a JSON string inside <script type="__bundler/template">.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path


def load_template(path: Path) -> str:
    text = path.read_text(errors="replace")
    marker = '<script type="__bundler/template">'
    start = text.find(marker)
    if start < 0:
        raise SystemExit(f"{path} has no __bundler/template script. It is not a bundled design file.")
    start = text.find(">", start) + 1
    end = text.find("</script>", start)
    if end < 0:
        raise SystemExit(f"{path} template script is not closed.")
    raw = text[start:end].strip()
    template = json.loads(raw)
    if not isinstance(template, str):
        raise SystemExit(f"{path} template JSON is not an HTML string.")
    return template


def visible_text(html: str) -> str:
    without_style = re.sub(r"<style[\s\S]*?</style>", " ", html, flags=re.I)
    without_script = re.sub(r"<script[\s\S]*?</script>", " ", without_style, flags=re.I)
    text = re.sub(r"<[^>]+>", "\n", without_script)
    lines = [re.sub(r"\s+", " ", line).strip() for line in text.splitlines()]
    return "\n".join(line for line in lines if line)


def screen_labels(html: str) -> list[str]:
    return re.findall(r'data-screen-label="([^"]+)"', html)


def keyframes(html: str) -> str:
    blocks: list[str] = []
    for match in re.finditer(r"@keyframes\s+[\w-]+\s*\{", html):
        start = match.start()
        depth = 0
        for index in range(match.end() - 1, len(html)):
            char = html[index]
            if char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0:
                    blocks.append(html[start : index + 1])
                    break
    return "\n\n".join(blocks)


def slice_around(html: str, needle: str, before: int, after: int) -> str:
    index = html.lower().find(needle.lower())
    if index < 0:
        raise SystemExit(f"No match for {needle!r}.")
    return html[max(0, index - before) : index + after]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("html", type=Path, help="Bundled design HTML file")
    parser.add_argument("--list", action="store_true", help="List data-screen-label values")
    parser.add_argument("--copy", action="store_true", help="Print visible text, in order")
    parser.add_argument("--keyframes", action="store_true", help="Print CSS keyframes")
    parser.add_argument("--screen", help="Print markup around this label or heading")
    parser.add_argument("--before", type=int, default=400)
    parser.add_argument("--after", type=int, default=8000)
    parser.add_argument("--write", type=Path, help="Write the inner design HTML to this path")
    args = parser.parse_args()

    template = load_template(args.html)
    if args.write:
        args.write.write_text(template)
        print(f"Wrote {args.write} ({len(template)} chars)", file=sys.stderr)

    if args.list:
        labels = screen_labels(template)
        print("\n".join(labels) if labels else "(no data-screen-label attributes)")
        return
    if args.copy:
        print(visible_text(template))
        return
    if args.keyframes:
        found = keyframes(template)
        print(found if found else "(no keyframes)")
        return
    if args.screen:
        print(slice_around(template, args.screen, args.before, args.after))
        return

    labels = screen_labels(template)
    print(f"template_chars {len(template)}")
    print("screens:")
    for label in labels:
        print(f"- {label}")
    if not labels:
        print("(no data-screen-label attributes; use --copy or --screen)")


if __name__ == "__main__":
    main()
