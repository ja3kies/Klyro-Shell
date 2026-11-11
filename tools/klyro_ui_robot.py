#!/usr/bin/env python3
"""Klyro UI state-based smoke tests. Read-only by default; captures evidence."""
import argparse
import shutil
import datetime as dt
import html
import hashlib
import json
import math
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG = "com.klyro.shell"
HOME_ACTION = ("Launch Bash", "Return to terminal")
HOME_MARKERS = ("KLYRO", "RUNTIME STATUS", "Plugins", "Settings")
# Explicit screen transitions derived from the Pixel 9 XML evidence.
SETTINGS_ROOT = ("Settings", "Appearance", "Keyboard", "Storage", "Privacy")
ROUTES = [("Plugins", ("Plugins", "Installed", "Explore")),
          ("Home", HOME_MARKERS),
          ("Settings", SETTINGS_ROOT)]
SETTINGS_SUBPAGES = [
    ("Appearance", ("Appearance", "Launcher icon", "App theme")),
    ("Keyboard", ("Keyboard", "Modifier status")),
    ("Storage", ("Files & storage",)),
    ("Privacy", ("Privacy", "Android permissions")),
    ("Diagnostics", ("Diagnostics", "Recheck environment")),
    ("About", ("About Klyro Shell", "JetBrains Mono licence")),
]
SAFE = re.compile(r"^(More actions|Settings|Preferences|Plugins|Appearance|Theme|Find|Search|Home|General|Workspace|Diagnostics|About|Paramètres|Apparence|Recherche|Accueil)$", re.I)
DANGER = re.compile(r"end session|end bash|delete|remove|uninstall|clear|reset|wipe|kill|install|logout|erase|supprimer|effacer|réinitialiser|désinstaller", re.I)

def adb(*a, binary=False, timeout=45):
    p = subprocess.run(["adb", *a], capture_output=True, timeout=timeout)
    if p.returncode:
        raise RuntimeError((p.stderr or p.stdout).decode(errors="replace")[:1000])
    return p.stdout if binary else p.stdout.decode(errors="replace")

def measured_density():
    """Get pixels per dp from the connected Android device, respecting overrides."""
    data = adb("shell", "wm", "density")
    matches = re.findall(r"(?:Physical|Override) density:\s*(\d+)", data)
    if not matches:
        return None
    return int(matches[-1]) / 160.0

def installed_app_metadata():
    """Read-only package identity; source commit != installed APK by default."""
    output = adb("shell", "dumpsys", "package", PKG, timeout=50)
    version_code = re.search(r"\bversionCode=(\d+)", output)
    version_name = re.search(r"\bversionName=([^\s]+)", output)
    updated = re.search(r"\blastUpdateTime=([^\r\n]+)", output)
    return {
        "package": PKG,
        "versionCode": version_code.group(1) if version_code else None,
        "versionName": version_name.group(1) if version_name else None,
        "lastUpdateTime": updated.group(1).strip() if updated else None,
        "source_matches_installed_apk": "UNVERIFIED",
    }

def active():
    data = adb("shell", "dumpsys", "activity", "activities")
    lines = [x for x in data.splitlines() if "topResumedActivity" in x or "mResumedActivity" in x]
    return any(PKG in x for x in lines)

def nodes(root):
    return list(root.iter("node"))

def visible_nodes(root):
    """Exclude whole invisible subtrees, not just nodes marked invisible."""
    result = []
    stack = [(root, True)]
    while stack:
        node, parent_visible = stack.pop()
        visible = parent_visible and node.get("visible-to-user") != "false"
        if visible:
            result.append(node)
        stack.extend((child, visible) for child in reversed(list(node)))
    return result

def node_labels(n):
    """Visible text AND accessibility description, including Compose descendants.

    A Compose button may expose onClickLabel='Open Klyro plugins' while its
    child displays 'Plugins'. Both are valid, but only the short title should
    be used to select the intended safe navigation control.
    """
    found = []
    for node in visible_nodes(n):
        for attr in ("content-desc", "text"):
            value = (node.get(attr) or "").strip()
            if value and value not in found:
                found.append(value)
    return found

def label(n):
    return next(iter(node_labels(n)[:1]), "")

def controls(root):
    found = []
    for n in visible_nodes(root):
        if n.get("clickable") != "true" or n.get("enabled") != "true":
            continue
        if n.get("visible-to-user") == "false":
            continue
        raw = n.get("bounds", "")
        match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", raw)
        if not match:
            continue
        x1, y1, x2, y2 = map(int, match.groups())
        if x2 - x1 < 32 or y2 - y1 < 32:
            continue
        aliases = node_labels(n)
        title = next((v for v in aliases if SAFE.fullmatch(v)), aliases[0] if aliases else "")
        if title:
            found.append({"label": title, "aliases": aliases, "x": (x1 + x2) // 2,
                          "y": (y1 + y2) // 2, "bounds": (x1, y1, x2, y2)})
    return found

def state(root):
    ns = visible_nodes(root)
    labels = []
    for n in ns:
        if n.get("visible-to-user") != "false":
            for attr in ("content-desc", "text"):
                value = (n.get(attr) or "").strip()
                if value:
                    labels.append(value)
    sessions = sorted(set(re.findall(r"Session (\d+)", " ".join(labels))))
    dialog = any("End Bash session?" in x for x in labels)
    selected_tabs = []
    for node in ns:
        if node.get("selected") == "true" and node.get("visible-to-user") != "false":
            selected_tabs.extend(node_labels(node))
    return {"dialog": dialog, "sessions": sessions, "labels": labels,
            "selected_tabs": selected_tabs, "controls": controls(root),
            "keyboard": any(n.get("package", "").startswith("com.google.android.inputmethod")
                            for n in ns)}

def xml_signature(root):
    """Ignore clock/notification pixels; compare visible UI semantics and geometry."""
    attrs = ("class", "text", "content-desc", "bounds", "clickable",
             "enabled", "scrollable", "selected", "checked", "checkable")
    rows = [tuple(n.get(a, "") for a in attrs) for n in visible_nodes(root)]
    return hashlib.sha256(json.dumps(rows, ensure_ascii=False).encode("utf-8")).hexdigest()[:20]

def parse_bounds(raw):
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", raw or "")
    if not match:
        return None
    x1,y1,x2,y2 = map(int, match.groups())
    return (x1,y1,x2,y2) if x2>x1 and y2>y1 else None

def geometry(root):
    """Detect Android Material bottom sheet, drag handle and scroll viewport."""
    available = visible_nodes(root)
    bounds = [b for b in (parse_bounds(n.get("bounds")) for n in available) if b]
    screen_height = max((b[3] for b in bounds), default=None)
    screen_width = max((b[2] for b in bounds), default=None)
    touches_bottom = bool(screen_height and any(
        n.get("clickable") == "true" and (parse_bounds(n.get("bounds")) or (0,0,0,0))[3] >= screen_height - 12
        for n in available))
    sheet_top, handle = None, None
    for n in available:
        desc = (n.get("content-desc") or "").casefold()
        bounds_n = parse_bounds(n.get("bounds"))
        if "fermer la feuille" in desc or "close sheet" in desc:
            if bounds_n:
                sheet_top = bounds_n[3]
            else:
                # Fully expanded sheets may have a zero-height scrim.
                coords = re.findall(r"-?\d+", n.get("bounds", ""))
                if len(coords) == 4:
                    sheet_top = int(coords[3])
        if "poignée de déplacement" in desc or "drag handle" in desc:
            handle = bounds_n
    if sheet_top is None and handle:
        sheet_top = max(0, handle[1] - 70)
    scrollables = [parse_bounds(n.get("bounds")) for n in available
                   if n.get("scrollable") == "true"]
    scrollables = [b for b in scrollables if b and (sheet_top is None or b[3] > sheet_top)]
    scroll = max(scrollables, key=lambda r: (r[2]-r[0])*(r[3]-r[1]), default=None)
    labels = [v for n in available for v in (n.get("text"), n.get("content-desc")) if v]
    joined = " ".join(labels).casefold()
    if "local extensions" in joined:
        stage = "Plugins"
    elif sheet_top is None:
        stage = "Home"
    elif "launcher icon" in joined:
        stage = "Appearance"
    elif "modifier status" in joined:
        stage = "Keyboard"
    elif "files & storage" in joined:
        stage = "Storage"
    elif "android permissions" in joined:
        stage = "Privacy"
    elif "local checks" in joined and "diagnostics" in joined:
        stage = "Diagnostics"
    elif "about klyro shell" in joined:
        stage = "About"
    else:
        stage = "Settings"
    return {"sheet_top": sheet_top, "handle": handle, "scroll": scroll,
            "screen_height": screen_height, "screen_width": screen_width,
            "touches_bottom": touches_bottom, "stage": stage}

class Robot:
    def __init__(self, out, delay):
        self.out, self.delay, self.steps, self.warnings = out, delay, [], []
        self.serial = 0
        self.seen_screens = {}
        self.duplicates = 0
        self.sheet_gallery = {}
        self.last_snapshots = []
    def snap(self, name):
        if not active():
            raise RuntimeError("Klyro not foreground; no further interaction")
        base = f"{self.serial:03d}-{re.sub('[^a-z0-9-]','-',name.lower())[:36]}"
        self.serial += 1
        # Keep the transient hierarchy out of public shared storage. Always
        # remove it before capturing, so a failed dump cannot reuse stale XML.
        remote = "/data/local/tmp/klyro-qa-current.xml"
        adb("shell", "rm", "-f", remote)
        try:
            adb("shell", "uiautomator", "dump", remote, timeout=65)
            xml = adb("exec-out", "cat", remote)
        finally:
            # No terminal app files or user settings are modified.
            try:
                adb("shell", "rm", "-f", remote, timeout=15)
            except (RuntimeError, subprocess.TimeoutExpired) as error:
                self.warnings.append("Temporary hierarchy cleanup failed: " + str(error))
        root = ET.fromstring(xml)
        signature = xml_signature(root)
        result = state(root)
        result.update(geometry(root))
        result["signature"] = signature
        result["screen"] = tuple(sorted(set(result["labels"])))
        self.last_snapshots.append({"screen_width": result.get("screen_width"),
                                    "screen_height": result.get("screen_height"),
                                    "stage": result.get("stage"), "signature": signature})
        cached = self.seen_screens.get(signature)
        if cached:
            self.duplicates += 1
            result["shot"], result["xml"] = cached
            result["duplicate_of"] = cached[0]
            return result
        xml_name, shot_name = base + ".xml", base + ".png"
        (self.out / xml_name).write_text(xml, encoding="utf-8")
        (self.out / shot_name).write_bytes(adb("exec-out", "screencap", "-p", binary=True))
        self.seen_screens[signature] = (shot_name, xml_name)
        result["shot"], result["xml"] = shot_name, xml_name
        return result

    def tap(self, target):
        if not active():
            raise RuntimeError("Klyro lost foreground")
        adb("shell","input","tap",str(target["x"]),str(target["y"]))
        time.sleep(self.delay)
    def swipe(self, x1, y1, x2, y2, duration=500):
        if not active():
            raise RuntimeError("Klyro is no longer foreground; gesture cancelled")
        adb("shell", "input", "swipe", str(x1), str(y1), str(x2),
            str(y2), str(duration))
        time.sleep(self.delay)

    def capture_full_sheet(self, current, page, max_scrolls=5):
        """Expand sheet via its labeled drag handle, then capture scroll segments.

        This only performs reversible gestures. It never clicks settings
        toggles or requests storage permissions.
        """
        if current.get("sheet_top") is None:
            return current
        previous = self.sheet_gallery.get(page)
        if previous and previous.get("images"):
            return current
        height = current.get("screen_height") or 2424
        record = {"images": [], "scroll_segments": 0, "expanded": "not attempted",
                  "top_fraction": current["sheet_top"] / height,
                  "fully_captured": False, "observed_labels": list(current.get("labels", []))}
        self.sheet_gallery[page] = record
        handle = current.get("handle")
        # A compact sheet already at its natural expanded height does not
        # need another drag. A redundant handle swipe risks dismissing it.
        # Expand only when scrolling or bottom-edge evidence indicates that
        # some content may be obstructed.
        requires_expansion = bool(current.get("scroll") or current.get("touches_bottom"))
        if handle and requires_expansion and current["sheet_top"] > height * 0.24:
            x = (handle[0] + handle[2]) // 2
            y = (handle[1] + handle[3]) // 2
            self.swipe(x, y, x, max(int(height * .13), y - int(height * .40)), 560)
            expanded = self.snap("expanded-" + page)
            if expanded.get("sheet_top") is None:
                record["expanded"] = "unexpected navigation; capture stopped"
                raise RuntimeError(
                    "Sheet disappeared during expansion: " + page +
                    "; stopped before any further taps")
            record["expanded"] = (
                "verified" if expanded.get("sheet_top") is not None
                and expanded["sheet_top"] + 24 < current["sheet_top"]
                else "not observed")
            current = expanded
            record["observed_labels"].extend(expanded.get("labels", []))
            if (record["expanded"] != "verified" and record["top_fraction"] > .45
                    and (current.get("scroll") or current.get("touches_bottom"))):
                self.warnings.append(
                    "Sheet expansion not confirmed for potentially clipped content in " + page)
        record["images"].append(current["shot"])
        baseline = current
        scroll_rect = current.get("scroll")
        if scroll_rect:
            x1,y1,x2,y2 = scroll_rect
            x = (x1 + x2)//2
            distance = max(100, int((y2-y1) * .53))
            y_start = min(y2 - 42, y1 + int((y2-y1) * .82))
            y_stop = max(y1 + 42, y_start - distance)
            pages = 0
            end_reached = False
            for n in range(max_scrolls):
                self.swipe(x, y_start, x, y_stop, 470)
                next_view = self.snap("full-" + page + "-scroll-" + str(n+1))
                # A section title may scroll off-screen, changing the
                # inferred route name even though the same sheet remains.
                if next_view.get("sheet_top") is None:
                    raise RuntimeError("Scroll unexpectedly left the sheet: " + page)
                if next_view["signature"] == current["signature"]:
                    end_reached = True
                    break
                pages += 1
                if next_view["shot"] not in record["images"]:
                    record["images"].append(next_view["shot"])
                record["observed_labels"].extend(next_view.get("labels", []))
                current = next_view
            record["scroll_segments"] = pages
            record["fully_captured"] = end_reached
            if not end_reached and pages >= max_scrolls:
                self.warnings.append("Full sheet not proven: scroll limit reached in " + page)
            # Do not swipe down to restore a ModalBottomSheet! On the Pixel 9
            # evidence of 2026-11-11 this collapsed and then dismissed the
            # entire sheet. The caller must leave the subsection using the
            # explicit Android BackHandler, which resets its ScrollState.
            record["exit_strategy"] = "android-back"
            record["last_view"] = current["shot"]
        else:
            # A compact sheet is complete without forcing it to the top;
            # a bottom-clipped sheet without scroll is inconclusive.
            record["fully_captured"] = (
                record["expanded"] == "verified" or
                not current.get("touches_bottom", False))
            if not record["fully_captured"]:
                self.warnings.append("Complete sheet visibility not verified: " + page)
        return current

    def record(self, name, passed, before, after, detail="", target=None):
        self.steps.append({"name":name,"status":"PASS" if passed else "FAIL",
                           "before":before["shot"],"after":after["shot"],"detail":detail,
                           "target":target, "from_stage":before.get("stage"),
                           "destination":after.get("route_stage", after.get("stage")),
                           "before_signature":before.get("signature"),
                           "after_signature":after.get("signature")})
        if not passed:
            self.warnings.append(f"{name}: {detail or 'assertion failed'}")
    def record_skip(self, name, snapshot, reason):
        self.steps.append({"name": name, "status": "SKIP",
                           "before": snapshot["shot"], "after": snapshot["shot"],
                           "detail": reason, "target": None,
                           "from_stage": snapshot.get("stage"), "destination": snapshot.get("stage")})
        self.warnings.append(name + ": " + reason)
    def record_blocked(self, name, snapshot, reason):
        self.record(name, False, snapshot, snapshot, reason)
    def find(self, s, pattern):
        return next((c for c in s["controls"] if any(re.fullmatch(pattern, v, re.I) for v in c.get("aliases", [c["label"]]))), None)
    def has_markers(self, s, markers):
        labels = s["labels"]
        return all(any(option.casefold() in item.casefold() for option in
                       (marker if isinstance(marker, tuple) else (marker,))
                       for item in labels) for marker in markers)
    def wait_for_markers(self, title, markers, max_checks=3):
        current = self.snap(title)
        for _ in range(max_checks - 1):
            if self.has_markers(current, markers):
                break
            time.sleep(self.delay)
            current = self.snap(title + "-retry")
        return current
    def wait_for_route(self, title, markers, max_checks=5):
        """Wait for the destination screen, not a label on the previous one.

        Settings root already contains the words Appearance and Keyboard.
        Those labels alone cannot prove that their pages have opened.
        """
        current = self.snap("after-" + title)
        for n in range(max_checks - 1):
            matched_stage = current.get("stage", "").casefold() == title.casefold()
            if matched_stage and self.has_markers(current, markers[:1]):
                return current
            time.sleep(self.delay)
            current = self.snap("after-" + title + "-retry-" + str(n+1))
        return current

    def navigate(self, before, title, markers):
        target = self.find(before, re.escape(title))
        if target is None:
            self.record_blocked("Navigate: " + title, before, "Labeled control not found; no tap attempted")
            return None
        self.tap(target)
        # A collapsed sheet exposes the section heading before lower controls.
        # Collect every scroll viewport before verifying deeper markers; do
        # not fail Diagnostics/About only because their buttons are below fold.
        after = self.wait_for_route(title, markers)
        # A label changing is not evidence of a route transition: clock/live
        # text can change while a tap failed. Validate the screen identity
        # observed immediately after the tap, before scrolling moves headings.
        entry_stage = after.get("stage")
        expected_route = title.casefold()
        transitioned = (entry_stage is not None and
                        entry_stage.casefold() == expected_route and
                        before.get("stage", "").casefold() != expected_route)
        if after.get("sheet_top") is not None and self.has_markers(after, markers[:1]):
            page = entry_stage or title
            after = self.capture_full_sheet(after, page)
            after["route_stage"] = entry_stage
            observed = self.sheet_gallery.get(page, {}).get("observed_labels",
                                                            after.get("labels", []))
            passed = (transitioned and
                      self.has_markers({"labels": observed}, markers) and
                      after.get("sheet_top") is not None)
        else:
            passed = transitioned and self.has_markers(after, markers)
        self.record("Navigate: " + title, passed, before, after,
                    "Expected " + ", ".join(" / ".join(m) if isinstance(m, tuple) else m for m in markers),
                    target=target)
        return after if passed else None
    def probe_plugin_tabs(self, current):
        # Switching tabs does not install, enable or remove any extension.
        selected = current.get("selected_tabs", [])
        installed_selected = any(re.fullmatch(r"Installed \(\d+\)", x) for x in selected)
        explore_selected = any(x == "Explore" for x in selected)
        if installed_selected == explore_selected:
            self.record_skip("Plugin tab switch", current, "Selected tab not exposed by Android UI hierarchy")
            return current
        target_regex = "Explore" if installed_selected else r"Installed \(\d+\)"
        target = self.find(current, target_regex)
        if target is None:
            self.record_skip("Plugin tab switch", current, "Tab switch control not found; no tap attempted")
            return current
        self.tap(target)
        switched = self.snap("plugin-tab-switched")
        expected = ("Explore" if installed_selected else "Installed")
        selected_now = switched.get("selected_tabs", [])
        passed = (any(x == "Explore" for x in selected_now) if installed_selected
                  else any(re.fullmatch(r"Installed \(\d+\)", x) for x in selected_now))
        self.record("Plugin tab switch", passed, current, switched,
                    "Selected tab should change to " + expected, target=target)
        if not passed:
            return None
        return_regex = r"Installed \(\d+\)" if installed_selected else "Explore"
        back = self.find(switched, return_regex)
        if back is None:
            self.record_blocked("Plugin tab restoration", switched, "Original plugin tab control disappeared")
            return None
        self.tap(back)
        restored = self.snap("plugin-tab-restored")
        restored_tabs = restored.get("selected_tabs", [])
        ok = (any(re.fullmatch(r"Installed \(\d+\)", x) for x in restored_tabs)
              if installed_selected else "Explore" in restored_tabs)
        self.record("Plugin tab restoration", ok, switched, restored,
                    "Original selected tab should be restored", target=back)
        return restored if ok else None

    def back_to_settings(self, current, page):
        """Use Klyro's nested Settings BackHandler, never a downward drag."""
        if not active():
            self.record_blocked("Back to Settings: " + page, current,
                                "Klyro no longer in foreground")
            return None
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(self.delay)
        after = self.wait_for_markers("after-back-" + page, SETTINGS_ROOT)
        passed = (self.has_markers(after, SETTINGS_ROOT) and
                  after.get("stage") == "Settings" and
                  current.get("stage") != "Settings" and
                  after.get("sheet_top") is not None)
        self.record("Back to Settings: " + page, passed, current, after,
                    "Android BackHandler must restore Settings root without dismissing sheet")
        return after if passed else None

    def smoke(self):
        current = self.snap("initial")
        initial_home = current
        home_markers = HOME_MARKERS
        if not self.has_markers(current, home_markers):
            self.record_blocked("Initial Home", current, "Home markers missing; no tap attempted")
            return
        self.record("Initial Home", True, current, current, "Required Home controls visible")
        for name, markers in ROUTES:
            current = self.navigate(current, name, markers)
            if current is None:
                self.warnings.append("Smoke route stopped at " + name)
                return
            if name == "Plugins":
                current = self.probe_plugin_tabs(current)
                if current is None:
                    self.warnings.append("Plugin tab probe failed; stopped safely")
                    return
        # Each Settings subpage is visited and then unwound without changing
        # user preferences, shell sessions, storage grants or external apps.
        for name, markers in SETTINGS_SUBPAGES:
            detail = self.navigate(current, name, markers)
            if detail is None:
                self.warnings.append("Settings subpage not verified: " + name)
                return
            current = self.back_to_settings(detail, name)
            if current is None:
                self.warnings.append("Back to Settings not verified after " + name)
                return
        target = self.find(current, r"Close")
        if target is not None:
            self.tap(target)
            after = self.wait_for_markers("after-close", home_markers)
            ok = self.has_markers(after, home_markers) and after["screen"] != current["screen"]
            self.record("Close settings", ok, current, after, "Home visible after dismissal", target=target)
            if ok:
                # Non-destructive regression: only assert what the UI exposes.
                # We cannot infer backend PTY lifetime from XML alone.
                first_sessions = initial_home.get("sessions", [])
                final_sessions = after.get("sessions", [])
                first_action = [v for v in HOME_ACTION if any(
                    v.casefold() in x.casefold() for x in initial_home.get("labels", []))]
                final_action = [v for v in HOME_ACTION if any(
                    v.casefold() in x.casefold() for x in after.get("labels", []))]
                unchanged = first_sessions == final_sessions and first_action == final_action
                self.record("Visible terminal state preserved", unchanged,
                            initial_home, after,
                            "Session identifiers and Bash launch/resume labels must match")
        else:
            self.record_blocked("Close settings", current, "Close control missing; no tap attempted")
    def safe_cancel_dialog(self, s):
        if not s["dialog"]:
            return s
        cancel = self.find(s,r"^Keep session$")
        if cancel is None:
            raise RuntimeError("Destructive dialog without safe cancellation; stopped")
        self.tap(cancel)
        after = self.snap("dialog-cancelled")
        self.record("Cancel session closure",not after["dialog"],s,after,"Dialog must disappear after Keep session")
        return after
    def explore(self, max_actions):
        # Semantic selectors and observable assertions, inspired by Maestro/Appium.
        seen = set()
        current = self.safe_cancel_dialog(self.snap("initial"))
        stack = []
        actions = 0
        while actions < max_actions:
            current = self.safe_cancel_dialog(current)
            signature = current["screen"]
            candidates = [c for c in current["controls"]
                          if SAFE.fullmatch(c["label"]) and not DANGER.search(c["label"])
                          and (signature, c["label"]) not in seen]
            if not candidates:
                if not stack:
                    break
                before = current
                adb("shell", "input", "keyevent", "KEYCODE_BACK")
                time.sleep(self.delay)
                current = self.safe_cancel_dialog(self.snap("after-back"))
                expected = stack.pop()
                ok = current["screen"] == expected
                self.record("Back navigation", ok, before, current,
                            "Previous accessibility state must be restored")
                if not ok:
                    self.warnings.append("Navigation stack mismatch; stopped")
                    break
                continue
            target = candidates[0]
            seen.add((signature, target["label"]))
            before = current
            self.tap(target)
            after = self.safe_cancel_dialog(self.snap("after-"+target["label"]))
            new_labels = set(after["labels"]) - set(before["labels"])
            changed = after["screen"] != signature
            destinations = {
                "Plugins": r"installed|available|import|plugin",
                "Settings": r"appearance|keyboard|storage|diagnostic|theme",
            }
            expected = destinations.get(target["label"])
            destination_ok = bool(expected and any(re.search(expected, x, re.I) for x in new_labels))
            ok = changed and (destination_ok if expected else True)
            self.record("Navigate: "+target["label"], ok, before, after,
                        "Destination verified" if ok else "Destination not confirmed")
            actions += 1
            if ok:
                stack.append(signature)
                current = after
            else:
                self.warnings.append("Navigation assertion failed; stopped before further taps")
                break
        if actions == 0:
            self.warnings.append("No safe actionable controls discovered")
        if actions >= max_actions and max_actions > 0:
            self.warnings.append("Exploration action limit reached; coverage incomplete")
        return actions
    def session_smoke(self):
        before = self.safe_cancel_dialog(self.snap("session-baseline"))
        add = self.find(before,r"^New terminal$")
        if add is None:
            self.warnings.append("Session creation: New terminal not visible")
            return
        count = len(before["sessions"])
        self.tap(add)
        after = self.snap("session-added")
        self.record("Create terminal session",len(after["sessions"]) == count+1,before,after,"Expected one new session")
        if len(after["sessions"]) != count+1:
            return
        # No automatic session termination: this can kill a running shell.
        self.warnings.append("Created one new terminal session; left it running intentionally")

def report(out, steps, warnings, ux_findings=None, app_info=None):
    counts = {status: sum(step["status"] == status for step in steps)
              for status in ("PASS", "FAIL", "SKIP")}
    ux_findings = ux_findings or []
    overall = ("FAIL" if counts["FAIL"] else
               "INCOMPLETE" if warnings or counts["SKIP"] or not steps else
               "NEEDS_UX_REVIEW" if ux_findings else "PASS")
    payload={"generated":dt.datetime.now().isoformat(), "overall": overall,
             "summary": counts, "ux_findings_count": len(ux_findings),
             "installed_app": app_info or {},
             "steps": steps, "warnings": warnings}
    (out/"results.json").write_text(json.dumps(payload,indent=2,ensure_ascii=False),encoding="utf-8")
    rows=[]
    for x in steps:
        a,b=html.escape(x["before"]),html.escape(x["after"])
        rows.append(f'<tr><td>{html.escape(x["name"])}</td><td>{x["status"]}</td><td>{html.escape(x["detail"])}</td><td><a href="{a}"><img src="{a}" width="180"></a></td><td><a href="{b}"><img src="{b}" width="180"></a></td></tr>')
    warn="".join("<li>"+html.escape(x)+"</li>" for x in warnings)
    (out/"report.html").write_text(f"""<!doctype html><html lang="fr"><meta charset="utf-8"><title>Klyro QA</title><style>body{{font:16px system-ui;max-width:1300px;margin:auto;padding:24px}}table{{width:100%;border-collapse:collapse}}td,th{{border:1px solid #ccc;padding:8px}}img{{border-radius:10px}}</style><h1>Klyro — UI QA</h1><p><a href="ux-review.html">Audit complet UI / UX + code Kotlin de chaque composant</a></p><h2>État : {overall}</h2><p>PASS : {counts["PASS"]} · FAIL : {counts["FAIL"]} · SKIP : {counts["SKIP"]} · Problèmes UI/UX : {len(ux_findings)}</p><p>PASS = vérification d'état observé, pas validation complète du terminal.</p><p>{len(steps)} assertions, {len(warnings)} avertissements.</p><ul>{warn}</ul><table><tr><th>Test</th><th>Résultat</th><th>Attendu</th><th>Avant</th><th>Après</th></tr>{''.join(rows)}</table></html>""",encoding="utf-8")

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--output",default="./klyro-qa-report")
    p.add_argument("--max-actions",type=int,default=18)
    p.add_argument("--delay",type=float,default=0.8)
    p.add_argument("--create-session",action="store_true",help="Opt-in: create one real shell session; do not close it")
    p.add_argument("--mode", choices=("smoke", "explore"), default="smoke", help="Deterministic safe smoke route or bounded exploratory crawl")
    p.add_argument("--density", type=float, default=None, help="Physical device pixels per dp, for touch target audit")
    p.add_argument("--source-root", type=Path, default=None, help="Read-only Kotlin source root; source excerpts are never fabricated")
    args=p.parse_args()
    if not math.isfinite(args.delay) or args.delay < 0:
        p.error("--delay must be nonnegative and finite")
    if args.density is not None and (not math.isfinite(args.density) or args.density <= 0):
        p.error("--density must be finite and positive")
    out=Path(args.output).expanduser()
    out.mkdir(parents=True,exist_ok=True)
    robot=Robot(out,args.delay)
    app_info = {"package": PKG, "source_matches_installed_apk": "UNVERIFIED"}
    try:
        devices=adb("devices")
        if not any("\tdevice" in x for x in devices.splitlines()):
            raise RuntimeError("No authorized ADB device")
        try:
            app_info = installed_app_metadata()
        except (RuntimeError, OSError, subprocess.TimeoutExpired):
            pass
        if not active():
            adb("shell","monkey","-p",PKG,"-c","android.intent.category.LAUNCHER","1")
            time.sleep(2)
        if not active():
            raise RuntimeError("Klyro is not the resumed activity")
        if args.mode == "smoke":
            robot.smoke()
        else:
            robot.explore(max(0,min(args.max_actions,100)))
        if args.create_session and active():
            robot.session_smoke()
    except Exception as exc:
        robot.warnings.append(f"{type(exc).__name__}: {exc}")
    finally:
        density = args.density
        if density is None:
            try:
                density = measured_density()
            except (OSError, RuntimeError, ValueError, subprocess.TimeoutExpired):
                density = None
        evidence = []
        try:
            from klyro_robot_report_audit import (
                inspect as audit_inspect,
                report as audit_report,
                reconcile_expected_markers,
            )
            snapshots = sorted(out.glob("*.xml"))
            if snapshots:
                evidence = reconcile_expected_markers(
                    [audit_inspect(p, density=density) for p in snapshots]
                )
                (out / "accessibility-audit.md").write_text(audit_report(evidence), encoding="utf-8")
        except (ImportError, ValueError, OSError, ET.ParseError) as err:
            robot.warnings.append("Accessibility audit unavailable: " + str(err))
        try:
            from klyro_ux_report import make_report
            height = next((row.get("screen_height") for row in robot.last_snapshots
                           if row.get("screen_height")), None)
            width = next((row.get("screen_width") for row in robot.last_snapshots
                          if row.get("screen_width")), None)
            make_report(out, robot.steps, source_root=args.source_root,
                        density=density, sheet_gallery=robot.sheet_gallery,
                        duplicate_count=robot.duplicates, display_height=height,
                        display_width=width, accessibility_evidence=evidence,
                        installed_app=app_info)
        except Exception as err:
            robot.warnings.append("Component UX/code report failed: " +
                                  type(err).__name__ + ": " + str(err))
        try:
            ux_findings = json.loads((out / "components.json").read_text(
                encoding="utf-8")).get("findings", [])
        except (OSError, ValueError):
            ux_findings = []
        report(out, robot.steps, robot.warnings, ux_findings=ux_findings,
               app_info=app_info)
        (out / "installed-app.json").write_text(
            json.dumps(app_info, ensure_ascii=False, indent=2), encoding="utf-8")
        shutil.make_archive(str(out), "zip", root_dir=out)
        print("Report:", out/"report.html")
        print("Component review:", out/"ux-review.html")
        print("Unique screens:", len(robot.seen_screens),
              "Duplicates avoided:", robot.duplicates)
        print("Assertions:", len(robot.steps), "Warnings:", len(robot.warnings))
        print("Evidence ZIP:", str(out)+".zip")
    if any(x["status"]=="FAIL" for x in robot.steps) or robot.warnings:
        raise SystemExit(1)

if __name__=="__main__":
    main()
