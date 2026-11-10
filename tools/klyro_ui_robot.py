#!/usr/bin/env python3
"""Bounded, non-destructive Android UI crawler with screenshot/HTML evidence."""
import argparse
import datetime as dt
import html
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "com.klyro.shell"
BLOCKED = re.compile(r"delete|remove|uninstall|clear|reset|wipe|force.stop|close session|terminate|kill|install|purchase|logout|sign.out|erase|supprimer|effacer|réinitialiser|désinstaller|fermer la session", re.I)
ALLOWED = re.compile(r"settings|preferences|plugins|appearance|theme|more actions|find|search|home|back|general|terminal|workspace|diagnostic|about|paramètres|apparence|recherche|accueil", re.I)

def adb(*args, binary=False, timeout=35):
    p = subprocess.run(["adb", *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
    if p.returncode:
        raise RuntimeError((p.stderr or p.stdout).decode(errors="replace")[:1200])
    return p.stdout if binary else p.stdout.decode(errors="replace")

def foreground():
    s = adb("shell", "dumpsys", "activity", "activities")
    return PACKAGE in "\n".join(x for x in s.splitlines() if "topResumedActivity=" in x)

def capture(out, index):
    remote = "/sdcard/klyro-robot.xml"
    adb("shell", "uiautomator", "dump", remote, timeout=55)
    xml = adb("exec-out", "cat", remote)
    (out / f"{index:03d}.xml").write_text(xml, encoding="utf-8")
    png = adb("exec-out", "screencap", "-p", binary=True)
    (out / f"{index:03d}.png").write_bytes(png)
    return ET.fromstring(xml)

def nodes(root):
    for node in root.iter("node"):
        if node.attrib.get("clickable") != "true" or node.attrib.get("enabled") != "true":
            continue
        bounds = re.findall(r"\d+", node.attrib.get("bounds", ""))
        if len(bounds) != 4:
            continue
        x1,y1,x2,y2 = map(int, bounds)
        if x2-x1 < 32 or y2-y1 < 32:
            continue
        descendants = list(node.iter("node"))
        label = " ".join(filter(None, [
            node.attrib.get("content-desc", ""),
            node.attrib.get("text", ""),
            *[n.attrib.get("content-desc", "") or n.attrib.get("text", "") for n in descendants[:6]]
        ])).strip()
        if not label:
            continue
        yield {"label": label[:160], "x": (x1+x2)//2, "y": (y1+y2)//2}

def write_report(out, results, warnings):
    (out / "results.json").write_text(json.dumps({"results":results,"warnings":warnings}, indent=2, ensure_ascii=False), encoding="utf-8")
    rows = []
    for r in results:
        label = html.escape(r["label"])
        shot = html.escape(r["screenshot"])
        rows.append(f'<tr><td>{r["step"]}</td><td>{label}</td><td>{r["status"]}</td><td><a href="{shot}"><img src="{shot}" width="240"></a></td></tr>')
    warn = "".join(f"<li>{html.escape(w)}</li>" for w in warnings)
    page = f"""<!doctype html><html lang="fr"><meta charset="utf-8"><title>Klyro UX Robot</title>
<style>body{{font:16px system-ui;max-width:1200px;margin:35px auto;padding:15px}}table{{border-collapse:collapse;width:100%}}td,th{{border:1px solid #bbb;padding:10px;text-align:left}}img{{border-radius:12px}}.note{{color:#8a5200}}</style>
<h1>Klyro-Shell — Rapport UX Robot</h1><p>Généré : {dt.datetime.now().isoformat(timespec="seconds")}</p>
<p class="note">Exploration limitée, non exhaustive. PASS signifie capture et retour au premier plan réussis, pas validation fonctionnelle complète.</p>
<p>Étapes : {len(results)} · Avertissements : {len(warnings)}</p><ul>{warn}</ul>
<table><tr><th>Étape</th><th>Interaction</th><th>Statut</th><th>Capture</th></tr>{''.join(rows)}</table></html>"""
    (out / "report.html").write_text(page, encoding="utf-8")

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--output", default="/sdcard/Klyro-Shell/qa-robot")
    p.add_argument("--max-actions", type=int, default=18)
    p.add_argument("--delay", type=float, default=1.2)
    args = p.parse_args()
    out = Path(args.output).expanduser()
    out.mkdir(parents=True, exist_ok=True)
    results, warnings, visited = [], [], set()
    try:
        adb("start-server")
        if not foreground():
            adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
            time.sleep(2)
        if not foreground():
            raise RuntimeError("Klyro not foreground; stop to avoid interacting with another app")
        for i in range(args.max_actions):
            if not foreground():
                warnings.append("Klyro left foreground; stopped safely")
                break
            root = capture(out, i)
            options = [n for n in nodes(root) if ALLOWED.search(n["label"]) and not BLOCKED.search(n["label"])]
            candidate = next((n for n in options if (n["label"],n["x"],n["y"]) not in visited), None)
            if candidate is None:
                warnings.append("No further allowlisted controls found; stopped")
                break
            visited.add((candidate["label"],candidate["x"],candidate["y"]))
            adb("shell", "input", "tap", str(candidate["x"]), str(candidate["y"]))
            time.sleep(args.delay)
            status = "PASS" if foreground() else "FAIL"
            results.append({"step":i+1,"label":candidate["label"],"status":status,"screenshot":f"{i:03d}.png"})
            if status == "FAIL":
                warnings.append("App no longer foreground after interaction")
                break
            # Return to the previous screen to keep each probe bounded.
            adb("shell", "input", "keyevent", "KEYCODE_BACK")
            time.sleep(0.5)
        if foreground():
            capture(out, len(results)+1)
    except Exception as e:
        warnings.append(f"{type(e).__name__}: {e}")
    finally:
        write_report(out, results, warnings)
        print(f"Report: {out / 'report.html'}")
        print(f"Actions: {len(results)}; warnings: {len(warnings)}")
    if warnings and not results:
        raise SystemExit(1)

if __name__ == "__main__":
    main()
