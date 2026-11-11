#!/usr/bin/env python3
"""Audit UIAutomator XML snapshots from a physical-device robot run.

Usage: python3 tools/klyro_robot_report_audit.py --out report.md 000-initial.xml 001-after-plugins.xml ...
The tool checks observed navigation, reachable targets, semantics and bounds.
It never claims that an action succeeded merely because a screenshot exists.
"""
import argparse
import math
import re
from pathlib import Path
import xml.etree.ElementTree as ET

EXPECTED = {
    "initial": ("KLYRO", "RUNTIME STATUS", "Plugins", "Settings"),
    "plugins": ("Plugins", "Installed", "Explore"),
    "home": ("KLYRO", "RUNTIME STATUS", "Plugins", "Settings"),
    "settings": ("Settings", "Appearance", "Keyboard", "Storage", "Privacy"),
    "appearance": ("Appearance", "Launcher icon", "App theme"),
    "keyboard": ("Keyboard", "Modifier status"),
    "storage": ("Files & storage",),
    "privacy": ("Privacy", "Android permissions"),
    "diagnostics": ("Diagnostics", "Recheck environment"),
    "about": ("About Klyro Shell", "JetBrains Mono licence"),
}

def visible_nodes(root):
    result = []
    stack = [(root, True)]
    while stack:
        n, parent_visible = stack.pop()
        visible = parent_visible and n.get("visible-to-user") != "false"
        if visible:
            result.append(n)
        stack.extend((child, visible) for child in reversed(list(n)))
    return result

def inspect(path, density=None):
    root = ET.parse(path).getroot()
    nodes = list(root.iter("node"))
    visible = visible_nodes(root)
    texts = [n.get("text", "") for n in visible]
    descs = [n.get("content-desc", "") for n in visible]
    strings = texts + descs
    bounds = []
    invalid_bounds = []
    # Hidden descendants must not inflate reported geometry defects.
    for n in visible:
        raw = n.get("bounds", "")
        try:
            x1, y1, x2, y2 = (int(v) for v in raw.replace("][", ",").strip("[]").split(","))
            if x2 <= x1 or y2 <= y1:
                invalid_bounds.append(raw)
            else:
                bounds.append((n, x1, y1, x2, y2))
        except (ValueError, AttributeError):
            if raw:
                invalid_bounds.append(raw)
            continue
    interactive = [n for n in visible if n.get("clickable") == "true" and n.get("enabled") != "false"]
    # UIAutomator may expose a parent with text descendants; don't flag these as unlabeled.
    unlabeled = []
    for n in interactive:
        labels = [n.get("text", ""), n.get("content-desc", "")]
        # Include every descendant label, not only the first non-empty field.
        # A whitespace-only text attribute must not hide a valid description.
        for child in visible_nodes(n):
            labels.extend((child.get("text", ""), child.get("content-desc", "")))
        if not any(x.strip() for x in labels):
            unlabeled.append(n.get("class", "View") + " " + n.get("bounds", ""))
    small = []
    small_details = []
    threshold = 48 * density if density is not None else None
    for n, x1, y1, x2, y2 in bounds:
        if threshold is not None and n in interactive and (x2-x1 < threshold or y2-y1 < threshold):
            small.append(n.get("bounds", ""))
            labels = [value.strip() for child in visible_nodes(n)
                      for value in (child.get("text", ""), child.get("content-desc", ""))
                      if value.strip()]
            small_details.append({
                "label": next(iter(labels), "(unlabeled)"),
                "bounds": n.get("bounds", ""),
                "width_dp": round((x2-x1) / density, 1),
                "height_dp": round((y2-y1) / density, 1),
            })
    # A target clipped by the display cannot be reliably tapped or read.
    # Report it separately from the 48dp check; this is measured geometry,
    # not an inferred judgment about colors or visual styling.
    # UIAutomator XML uses an unbounded <hierarchy> wrapper. The first
    # bounded visible <node> is the display viewport. Using root bounds
    # silently disabled clipping detection in the synthetic and Pixel XMLs.
    screen_rect = tuple(bounds[0][1:]) if bounds else None
    clipped = []
    if screen_rect:
        sx1, sy1, sx2, sy2 = screen_rect
        for n, x1, y1, x2, y2 in bounds:
            if n not in interactive:
                continue
            if x1 < sx1 or y1 < sy1 or x2 > sx2 or y2 > sy2:
                label = next((v.strip() for child in visible_nodes(n)
                              for v in (child.get("text", ""), child.get("content-desc", ""))
                              if v.strip()), "(unlabeled)")
                clipped.append({
                    "label": label, "bounds": n.get("bounds", ""),
                    "screen_bounds": f"[{sx1},{sy1}][{sx2},{sy2}]"
                })
    # Match complete stage tokens: e.g. "plugins" must not match "plugins-old".
    stage_tokens = re.split(r"[^a-z]+", path.stem.lower())
    key = next((k for k in EXPECTED if k in stage_tokens), None)
    missing = []
    # Scrolling often moves a section title offscreen: that is not a broken route.
    expected = () if "scroll" in stage_tokens else EXPECTED.get(key, ())
    for marker in expected:
        options = marker if isinstance(marker, tuple) else (marker,)
        if not any(any(option.casefold() in v.casefold() for option in options) for v in strings):
            missing.append(" / ".join(options))
    return dict(name=path.name, stage=key or "unknown", nodes=len(nodes),
                visible_strings=[x for x in strings if x.strip()],
                interactive=len(interactive), missing=missing,
                unlabeled=unlabeled, small=small, small_details=small_details,
                clipped=clipped, invalid_bounds=invalid_bounds,
                density=density, empty=(len(nodes) == 0),
                switches=sum(n.get("checkable") == "true" for n in nodes),
                selected=sum(n.get("selected") == "true" for n in nodes))

def reconcile_expected_markers(results):
    """One sheet may need several viewports before all its labels are visible.

    Keep per-snapshot inspect() findings independent for diagnostics, but avoid
    reporting a missing route marker when the same stage exposes it further
    down the scroll. Emit genuine missing-marker issues once per stage.
    """
    observed = {}
    for row in results:
        stage = row.get("stage", "unknown")
        observed.setdefault(stage, []).extend(row.get("visible_strings", []))
    seen = set()
    normalized = []
    for row in results:
        stage = row.get("stage", "unknown")
        available = observed.get(stage, [])
        missing = []
        for marker in row.get("missing", []):
            variants = marker.split(" / ")
            if any(option.casefold() in value.casefold()
                   for option in variants for value in available):
                continue
            key = (stage, marker)
            if key in seen:
                continue
            seen.add(key)
            missing.append(marker)
        normalized.append({**row, "missing": missing})
    return normalized

def report(results):
    results = reconcile_expected_markers(results)
    lines = ["# Klyro robot UIAutomator evidence audit", "",
             "This is an observational audit, not a device interaction test.",
             "Touch targets are evaluated against 48dp only when --density is supplied; without it, size is unverified.",
             "", "| Snapshot | Stage | Nodes | Enabled targets | Missing markers | Unlabeled targets |",
             "|---|---|---:|---:|---|---:|"]
    for r in results:
        safe_name = r["name"].replace("|", "\\|").replace("\n", " ")
        lines.append(f"| {safe_name} | {r['stage']} | {r['nodes']} | {r['interactive']} | {', '.join(r['missing']) or 'None'} | {len(r['unlabeled'])} |")
    lines += ["", "## Actionable findings", ""]
    for r in results:
        if r["empty"]:
            lines.append(f"- **{r['name']}**: no UI nodes were captured; snapshot is inconclusive, not a successful test.")
        for missing in r["missing"]:
            lines.append(f"- **{r['name']}**: expected visible marker not found: `{missing}`. Check navigation and scroll position.")
        for item in r["unlabeled"][:12]:
            lines.append(f"- **{r['name']}**: clickable node without its own or descendant accessible label: `{item}`. Inspect Compose semantics.")
        if r["small"]:
            lines.append(f"- **{r['name']}**: {len(r['small'])} touch targets below 48dp at density {r['density']}.")
            for item in r["small_details"][:15]:
                lines.append(
                    f"  - {item['label']}: {item['width_dp']} × {item['height_dp']} dp "
                    f"at {item['bounds']}; expand hit area and verify navigation-bar insets."
                )
        if r["clipped"]:
            lines.append(f"- **{r['name']}**: {len(r['clipped'])} clickable target bounds outside the captured display; inspect clipping and Compose insets.")
            for target in r["clipped"][:12]:
                lines.append(f"  - {target['label']} at {target['bounds']}; screen {target['screen_bounds']}.")
        if r["invalid_bounds"]:
            lines.append(f"- **{r['name']}**: {len(r['invalid_bounds'])} malformed or non-positive bounds; inspect XML extraction.")
    if not any(r["empty"] or r["missing"] or r["unlabeled"] or r["small"] or r["clipped"] or r["invalid_bounds"] for r in results):
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
    parser.add_argument("--density", type=float, help="Physical pixels per dp from device wm density; must be positive")
    args = parser.parse_args()
    if args.density is not None and (not math.isfinite(args.density) or args.density <= 0):
        parser.error("--density must be positive")
    results = [inspect(p, density=args.density) for p in args.xml]
    output = report(results)
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(output, encoding="utf-8")
    print(output)

if __name__ == "__main__":
    main()
