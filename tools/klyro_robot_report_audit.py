#!/usr/bin/env python3
"""Audit UIAutomator XML snapshots from a physical-device robot run.

Usage: python3 tools/klyro_robot_report_audit.py --out report.md 000-initial.xml 001-after-plugins.xml ...
The tool checks observed navigation, reachable targets, semantics and bounds.
It never claims that an action succeeded merely because a screenshot exists.
"""
import argparse
from collections import Counter
from pathlib import Path
import xml.etree.ElementTree as ET

EXPECTED = {
    "initial": ("KLYRO", "Launch Bash", "Plugins", "Settings"),
    "plugins": ("Plugins", "Installed", "Explore"),
    "home": ("KLYRO", "Launch Bash", "Plugins", "Settings"),
    "settings": ("Settings", "Appearance", "Keyboard", "Storage", "Privacy"),
    "appearance": ("Appearance", "Launcher icon", "App theme"),
}

def inspect(path):
    root = ET.parse(path).getroot()
    nodes = list(root.iter("node"))
    texts = [n.get("text", "") for n in nodes]
    descs = [n.get("content-desc", "") for n in nodes]
    strings = texts + descs
    bounds = []
    for n in nodes:
        raw = n.get("bounds", "")
        try:
            x1, y1, x2, y2 = (int(v) for v in raw.replace("][", ",").strip("[]").split(","))
            bounds.append((n, x1, y1, x2, y2))
        except (ValueError, AttributeError):
            continue
    interactive = [n for n in nodes if n.get("clickable") == "true" and n.get("enabled") == "true"]
    # UIAutomator may expose a parent with text descendants; don't flag these as unlabeled.
    unlabeled = []
    for n in interactive:
        labels = [n.get("text", ""), n.get("content-desc", "")]
        labels += [s.get("text", "") or s.get("content-desc", "") for s in n.iter("node")]
        if not any(x.strip() for x in labels):
            unlabeled.append(n.get("class", "View") + " " + n.get("bounds", ""))
    small = []
    for n, x1, y1, x2, y2 in bounds:
        if n in interactive and (x2-x1 < 96 or y2-y1 < 96):
            # 96 px is a heuristic, not a verified 48dp threshold (density unknown).
            small.append(n.get("bounds", ""))
    key = next((k for k in EXPECTED if k in path.stem.lower()), None)
    missing = [s for s in EXPECTED.get(key, ()) if not any(s in v for v in strings)]
    return dict(name=path.name, stage=key or "unknown", nodes=len(nodes),
                interactive=len(interactive), missing=missing,
                unlabeled=unlabeled, small=small,
                switches=sum(n.get("checkable") == "true" for n in nodes),
                selected=sum(n.get("selected") == "true" for n in nodes))

def report(results):
    lines = ["# Klyro robot UIAutomator evidence audit", "",
             "This is an observational audit, not a device interaction test.",
             "Pixel target-size warnings use a 96px heuristic; verify actual display density.",
             "", "| Snapshot | Stage | Nodes | Enabled targets | Missing markers | Unlabeled targets |",
             "|---|---|---:|---:|---|---:|"]
    for r in results:
        lines.append(f"| {r['name']} | {r['stage']} | {r['nodes']} | {r['interactive']} | {', '.join(r['missing']) or 'None'} | {len(r['unlabeled'])} |")
    lines += ["", "## Actionable findings", ""]
    for r in results:
        for missing in r["missing"]:
            lines.append(f"- **{r['name']}**: expected visible marker not found: `{missing}`. Check navigation and scroll position.")
        for item in r["unlabeled"][:12]:
            lines.append(f"- **{r['name']}**: clickable node without its own or descendant accessible label: `{item}`. Inspect Compose semantics.")
        if r["small"]:
            lines.append(f"- **{r['name']}**: {len(r['small'])} potential small touch targets (pixel heuristic; density needed).")
    if not any(r["missing"] or r["unlabeled"] or r["small"] for r in results):
        lines.append("- No issues detected by these limited static heuristics.")
    lines += ["", "## Follow-up device tests", "",
              "- Tap each action and compare before/after XML and screenshot; presence alone is not proof of navigation.",
              "- Confirm plugin removal, installation and theme changes with explicit user-approved safe test data.",
              "- Test system Back, outside-sheet dismissal, TalkBack labels, landscape, large font, and process recreation.",
              "- Keep the Android status-bar seam as a separate visual-only check.",
              ""]
    return "\n".join(lines)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("xml", nargs="+", type=Path)
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    results = [inspect(p) for p in args.xml]
    output = report(results)
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(output, encoding="utf-8")
    print(output)

if __name__ == "__main__":
    main()
