#!/usr/bin/env python3
"""Klyro UI state-based smoke tests. Read-only by default; captures evidence."""
import argparse
import datetime as dt
import html
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG = "com.klyro.shell"
SAFE = re.compile(r"^(More actions|Settings|Preferences|Plugins|Appearance|Theme|Find|Search|Home|General|Workspace|Diagnostics|About|Paramètres|Apparence|Recherche|Accueil)$", re.I)
DANGER = re.compile(r"end session|end bash|delete|remove|uninstall|clear|reset|wipe|kill|install|logout|erase|supprimer|effacer|réinitialiser|désinstaller", re.I)

def adb(*a, binary=False, timeout=45):
    p = subprocess.run(["adb", *a], capture_output=True, timeout=timeout)
    if p.returncode:
        raise RuntimeError((p.stderr or p.stdout).decode(errors="replace")[:1000])
    return p.stdout if binary else p.stdout.decode(errors="replace")

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
        title = next((v for v in labels if v), "")
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
        return next((c for c in s["controls"] if re.search(pattern,c["label"],re.I)),None)
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
        seen = set()
        current = self.snap("initial")
        current = self.safe_cancel_dialog(current)
        for _ in range(max_actions):
            current = self.safe_cancel_dialog(current)
            signature = (tuple(current["sessions"]), current["dialog"], tuple(sorted(set(current["labels"]))))
            candidates = [c for c in current["controls"] if SAFE.fullmatch(c["label"]) and not DANGER.search(c["label"])]
            next_action = next((c for c in candidates if (signature,c["label"]) not in seen),None)
            if next_action is None:
                break
            seen.add((signature,next_action["label"]))
            before = current
            self.tap(next_action)
            after = self.snap("after-navigation")
            self.record("Navigation: "+next_action["label"],after["labels"] != before["labels"] or after["dialog"] != before["dialog"],before,after,"Visible UI state should change")
            current = self.safe_cancel_dialog(after)
            if not active():
                break
            adb("shell","input","keyevent","KEYCODE_BACK")
            time.sleep(self.delay)
            current = self.snap("after-back")
            current = self.safe_cancel_dialog(current)
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
    args=p.parse_args()
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
        robot.explore(max(0,min(args.max_actions,100)))
        if args.create_session and active():
            robot.session_smoke()
    except Exception as exc:
        robot.warnings.append(f"{type(exc).__name__}: {exc}")
    finally:
        report(out,robot.steps,robot.warnings)
        print("Report:",out/"report.html")
        print("Assertions:",len(robot.steps),"Warnings:",len(robot.warnings))
    if any(x["status"]=="FAIL" for x in robot.steps) or (robot.warnings and not robot.steps):
        raise SystemExit(1)

if __name__=="__main__":
    main()
