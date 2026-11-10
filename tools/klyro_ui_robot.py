#!/usr/bin/env python3
"""Klyro UI state-based smoke tests. Read-only by default; captures evidence."""
import argparse
import shutil
import datetime as dt
import html
import json
import math
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG = "com.klyro.shell"
HOME_ACTION = ("Launch Bash", "Return to terminal")
HOME_MARKERS = ("KLYRO", HOME_ACTION, "Plugins", "Settings")
# Explicit screen transitions derived from the Pixel 9 XML evidence.
ROUTES = [("Plugins", ("Plugins", "Installed", "Explore")),
          ("Home", HOME_MARKERS),
          ("Settings", ("Settings", "Appearance", "Keyboard", "Storage", "Privacy")),
          ("Appearance", ("Appearance", "Launcher icon", "App theme"))]
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

def active():
    data = adb("shell", "dumpsys", "activity", "activities")
    lines = [x for x in data.splitlines() if "topResumedActivity" in x or "mResumedActivity" in x]
    return any(PKG in x for x in lines)

def nodes(root):
    return list(root.iter("node"))

def label(n):
    return (n.get("content-desc") or n.get("text") or "").strip()

def controls(root):
    found = []
    for n in nodes(root):
        if n.get("clickable") != "true" or n.get("enabled") != "true":
            continue
        match = re.findall(r"\d+", n.get("bounds", ""))
        if len(match) != 4:
            continue
        x1,y1,x2,y2 = map(int,match)
        if x2-x1 < 32 or y2-y1 < 32:
            continue
        labels = [label(v) for v in n.iter("node") if label(v)]
        title = label(n) or next((v for v in labels if SAFE.fullmatch(v)), "") or next((v for v in labels if v), "")
        if title:
            found.append({"label":title,"x":(x1+x2)//2,"y":(y1+y2)//2,"bounds":(x1,y1,x2,y2)})
    return found

def state(root):
    ns = nodes(root)
    labels = [label(n) for n in ns if label(n)]
    sessions = sorted(set(re.findall(r"Session (\d+)", " ".join(labels))))
    dialog = any("End Bash session?" in x for x in labels)
    return {"dialog":dialog,"sessions":sessions,"labels":labels,
            "controls":controls(root),
            "keyboard":any(n.get("package","").startswith("com.google.android.inputmethod") for n in ns)}

class Robot:
    def __init__(self, out, delay):
        self.out, self.delay, self.steps, self.warnings = out, delay, [], []
        self.serial = 0
    def snap(self, name):
        if not active():
            raise RuntimeError("Klyro not foreground; no further interaction")
        base = f"{self.serial:03d}-{re.sub('[^a-z0-9-]','-',name.lower())[:36]}"
        self.serial += 1
        adb("shell", "uiautomator", "dump", "/sdcard/klyro-qa-current.xml", timeout=65)
        xml = adb("exec-out", "cat", "/sdcard/klyro-qa-current.xml")
        (self.out / (base+".xml")).write_text(xml,encoding="utf-8")
        (self.out / (base+".png")).write_bytes(adb("exec-out","screencap","-p",binary=True))
        root = ET.fromstring(xml)
        result = state(root)
        result["shot"] = base+".png"
        result["screen"] = tuple(sorted(set(result["labels"])))
        return result
    def tap(self, target):
        if not active():
            raise RuntimeError("Klyro lost foreground")
        adb("shell","input","tap",str(target["x"]),str(target["y"]))
        time.sleep(self.delay)
    def record(self, name, passed, before, after, detail=""):
        self.steps.append({"name":name,"status":"PASS" if passed else "FAIL","before":before["shot"],"after":after["shot"],"detail":detail})
        if not passed:
            self.warnings.append(f"{name}: {detail or 'assertion failed'}")
    def find(self, s, pattern):
        return next((c for c in s["controls"] if re.fullmatch(pattern,c["label"],re.I)),None)
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
    def navigate(self, before, title, markers):
        target = self.find(before, re.escape(title))
        if target is None:
            self.warnings.append("Missing labeled control: " + title)
            return None
        self.tap(target)
        after = self.wait_for_markers("after-" + title, markers)
        passed = self.has_markers(after, markers) and after["screen"] != before["screen"]
        self.record("Navigate: " + title, passed, before, after,
                    "Expected " + ", ".join(" / ".join(m) if isinstance(m, tuple) else m for m in markers))
        return after if passed else None
    def smoke(self):
        current = self.snap("initial")
        home_markers = HOME_MARKERS
        if not self.has_markers(current, home_markers):
            self.warnings.append("Not on Klyro Home; smoke run stopped without tapping")
            return
        for name, markers in ROUTES:
            current = self.navigate(current, name, markers)
            if current is None:
                self.warnings.append("Smoke route stopped at " + name)
                return
        # Exit sheets using the visible Close button only; no Android app stop.
        target = self.find(current, r"Close")
        if target is not None:
            self.tap(target)
            after = self.wait_for_markers("after-close", home_markers)
            ok = self.has_markers(after, home_markers)
            self.record("Close settings", ok, current, after, "Home visible after dismissal")
        else:
            self.warnings.append("Close control not found on Appearance sheet")
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

def report(out, steps, warnings):
    payload={"generated":dt.datetime.now().isoformat(),"steps":steps,"warnings":warnings}
    (out/"results.json").write_text(json.dumps(payload,indent=2,ensure_ascii=False),encoding="utf-8")
    rows=[]
    for x in steps:
        a,b=html.escape(x["before"]),html.escape(x["after"])
        rows.append(f'<tr><td>{html.escape(x["name"])}</td><td>{x["status"]}</td><td>{html.escape(x["detail"])}</td><td><a href="{a}"><img src="{a}" width="180"></a></td><td><a href="{b}"><img src="{b}" width="180"></a></td></tr>')
    warn="".join("<li>"+html.escape(x)+"</li>" for x in warnings)
    (out/"report.html").write_text(f"""<!doctype html><html lang="fr"><meta charset="utf-8"><title>Klyro QA</title><style>body{{font:16px system-ui;max-width:1300px;margin:auto;padding:24px}}table{{width:100%;border-collapse:collapse}}td,th{{border:1px solid #ccc;padding:8px}}img{{border-radius:10px}}</style><h1>Klyro — UI QA</h1><p>PASS = vérification d'état observé, pas validation complète du terminal.</p><p>{len(steps)} assertions, {len(warnings)} avertissements.</p><ul>{warn}</ul><table><tr><th>Test</th><th>Résultat</th><th>Attendu</th><th>Avant</th><th>Après</th></tr>{''.join(rows)}</table></html>""",encoding="utf-8")

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--output",default="./klyro-qa-report")
    p.add_argument("--max-actions",type=int,default=18)
    p.add_argument("--delay",type=float,default=0.8)
    p.add_argument("--create-session",action="store_true",help="Opt-in: create one real shell session; do not close it")
    p.add_argument("--mode", choices=("smoke", "explore"), default="smoke", help="Deterministic safe smoke route or bounded exploratory crawl")
    p.add_argument("--density", type=float, default=None, help="Physical device pixels per dp, for touch target audit")
    args=p.parse_args()
    if not math.isfinite(args.delay) or args.delay < 0:
        p.error("--delay must be nonnegative and finite")
    if args.density is not None and (not math.isfinite(args.density) or args.density <= 0):
        p.error("--density must be finite and positive")
    out=Path(args.output).expanduser()
    out.mkdir(parents=True,exist_ok=True)
    robot=Robot(out,args.delay)
    try:
        devices=adb("devices")
        if not any("\tdevice" in x for x in devices.splitlines()):
            raise RuntimeError("No authorized ADB device")
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
        report(out,robot.steps,robot.warnings)
        try:
            from klyro_robot_report_audit import inspect as audit_inspect, report as audit_report
            snapshots = sorted(out.glob("*.xml"))
            if snapshots:
                density = args.density
                if density is None:
                    try:
                        density = measured_density()
                    except (OSError, RuntimeError, ValueError, subprocess.TimeoutExpired):
                        density = None
                evidence = [audit_inspect(p, density=density) for p in snapshots]
                (out / "accessibility-audit.md").write_text(audit_report(evidence),encoding="utf-8")
        except (ImportError, ValueError, OSError, ET.ParseError) as err:
            robot.warnings.append("Accessibility audit unavailable: " + str(err))
            report(out,robot.steps,robot.warnings)
        shutil.make_archive(str(out), "zip", root_dir=out)
        print("Report:",out/"report.html")
        print("Assertions:",len(robot.steps),"Warnings:",len(robot.warnings))
        print("Evidence ZIP:",str(out)+".zip")
    if any(x["status"]=="FAIL" for x in robot.steps) or robot.warnings:
        raise SystemExit(1)

if __name__=="__main__":
    main()
