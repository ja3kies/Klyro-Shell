#!/usr/bin/env python3
"""Render evidence-backed component UX/code reports. Never execute Kotlin source."""
import html
import json
import re
from pathlib import Path
from urllib.parse import quote

ROOT = "app/src/main/java/com/oryn/terminal/"
MAIN = ROOT + "MainActivity.kt"
PLUGINS = ROOT + "ui/KlyroPluginsPage.kt"
SETTINGS = ROOT + "ui/KlyroSettingsSheet.kt"
BASE_URL = "https://github.com/ja3kies/Klyro-Shell/blob/p1/ux-cross-screen-final-pass-20261110/"

SOURCE_ANCHORS = {
    "Initial Home": (MAIN, 'Text("KLYRO"'),
    "Navigate: Plugins": (MAIN, 'onClickLabel = "Open Klyro plugins"'),
    "Navigate: Home": (PLUGINS, 'Text("Home")'),
    "Navigate: Appearance": (SETTINGS, 'SettingsBentoTile("Appearance"'),
    "Navigate: Keyboard": (SETTINGS, 'SettingsBentoTile("Keyboard"'),
    "Navigate: Storage": (SETTINGS, 'SettingsBentoTile("Storage"'),
    "Navigate: Privacy": (SETTINGS, 'SettingsBentoTile("Privacy"'),
    "Navigate: Diagnostics": (SETTINGS, 'SettingsRowLink("Diagnostics"'),
    "Navigate: About": (SETTINGS, 'SettingsRowLink("About"'),
    "Close settings": (SETTINGS, 'Text("Close")'),
    "Plugin tab switch": (PLUGINS, 'listOf("installed" to "Installed'),
    "Plugin tab restoration": (PLUGINS, 'listOf("installed" to "Installed'),
    "Impl: SettingsBentoTile": (SETTINGS, "private fun SettingsBentoTile("),
    "Impl: SettingsRowLink": (SETTINGS, "fun SettingsRowLink("),
    "Impl: KlyroSheet": (ROOT + "ui/KlyroUiComponents.kt", "internal fun KlyroSheet("),
    "Impl: KlyroActionRow": (ROOT + "ui/KlyroUiComponents.kt", "internal fun KlyroActionRow("),
}
SUGGESTIONS = {
    "Initial Home": "Tester aussi le premier démarrage, le runtime absent et la reprise d'une session.",
    "Navigate: Plugins": "Clarifier la différence Installed / Explore et vérifier les confirmations sans installer de plugin.",
    "Navigate: Home": "Conserver l'état du terminal et la position de navigation au retour.",
    "Navigate: Settings": "Déployer la feuille quand des composants ne sont pas entièrement visibles et vérifier les espacements Bento.",
    "Navigate: Appearance": "Comparer les vignettes clair/sombre et capturer tous les réglages en scrollant.",
    "Navigate: Keyboard": "Distinguer les modificateurs actifs par plus que la couleur seule.",
    "Navigate: Storage": "Différencier lecture, import, export et demandes d'autorisation.",
    "Navigate: Privacy": "Vérifier la lisibilité avec grandes polices et TalkBack.",
    "Navigate: Diagnostics": "Vérifier la hauteur tactile des boutons et les longs journaux.",
    "Navigate: About": "Vérifier licences, contraste et numéros de version en thème sombre.",
    "Close settings": "Préserver l'état de l'accueil et des sessions à la fermeture.",
    "Plugin tab switch": "Vérifier l'état sélectionné par la sémantique et la mise en forme.",
    "Plugin tab restoration": "Rétablir l'onglet initial sans altérer le registre local.",
}

def source_for(action, from_stage, source_root):
    if action == "Navigate: Settings":
        choice = ((MAIN, 'onClickLabel = "Open Klyro settings"')
                  if from_stage in (None, "Home", "home")
                  else (SETTINGS, 'onClick = ::navigateBack'))
    else:
        choice = SOURCE_ANCHORS.get(action)
    if not choice:
        return {"status": "UNMAPPED", "description": "No mapped source component"}
    path, anchor = choice
    url = BASE_URL + "/".join(quote(x) for x in path.split("/"))
    if not source_root or not (Path(source_root) / path).is_file():
        return {"status": "UNAVAILABLE", "path": path, "url": url,
                "description": "Source Kotlin non inclus ; aucun code inventé"}
    lines = (Path(source_root) / path).read_text(encoding="utf-8").splitlines()
    index = next((i for i, line in enumerate(lines) if anchor in line), None)
    if index is None:
        return {"status": "ANCHOR_MISSING", "path": path, "url": url,
                "description": "Ancre source introuvable : " + anchor}
    lo, hi = max(0, index-16), min(len(lines), index+65)
    return {"status": "FOUND", "path": path, "url": url + "#L" + str(index+1),
            "line_start": lo+1, "line_end": hi,
            "code": "\n".join(f"{i+1:4d}  {lines[i]}" for i in range(lo, hi))}

def related_code_for(action, source_root):
    if action in ("Navigate: Appearance", "Navigate: Keyboard",
                  "Navigate: Storage", "Navigate: Privacy"):
        names = ("Impl: SettingsBentoTile", "Impl: KlyroSheet")
    elif action in ("Navigate: Diagnostics", "Navigate: About"):
        names = ("Impl: SettingsRowLink", "Impl: KlyroActionRow", "Impl: KlyroSheet")
    elif action in ("Navigate: Settings", "Close settings"):
        names = ("Impl: KlyroSheet",)
    else:
        names = ()
    return [{"name": name, **source_for(name, None, source_root)} for name in names]

def style_tokens(code):
    """Tokens actually named in the Kotlin excerpt, not visual guesses."""
    return sorted(set(re.findall(
        r"(?:KlyroUiMetrics|colors|ui)\.[A-Za-z][A-Za-z0-9_]*", code or "")))[:24]

def examine_target(target, density, display_height):
    if not target:
        return {"status": "NOT_RECORDED", "checks": [],
                "description": "Aucun composant cliqué dans cette assertion"}
    checks = []
    bounds = target.get("bounds")
    if target.get("label"):
        checks.append({"result": "PASS", "check": "Libellé accessible", "value": target["label"]})
    else:
        checks.append({"result": "FAIL", "check": "Libellé accessible", "value": "Absent"})
    if bounds and len(bounds) == 4 and density:
        w = round((bounds[2]-bounds[0])/density, 1)
        h = round((bounds[3]-bounds[1])/density, 1)
        checks.append({"result": "FAIL" if min(w, h) < 48 else "PASS",
                       "check": "Zone tactile 48 dp", "value": f"{w} × {h} dp"})
    else:
        checks.append({"result": "UNKNOWN", "check": "Zone tactile 48 dp",
                       "value": "Densité ou coordonnées non disponibles"})
    if bounds and display_height and bounds[3] >= display_height-12:
        checks.append({"result": "REVIEW", "check": "Bord inférieur",
                       "value": "Contrôler le rognage et les insets système"})
    return {"status": "OBSERVED", "label": target.get("label", ""),
            "bounds": bounds, "aliases": target.get("aliases", []),
            "class": target.get("class", ""), "checks": checks}

def improvements(action, target_review, sheet=None):
    result = [SUGGESTIONS.get(action,
               "Vérifier les états actif, inactif, chargement et navigation arrière.")]
    if any(c["result"] == "FAIL" and "48 dp" in c["check"]
           for c in target_review["checks"]):
        result.insert(0, "Agrandi la zone interactive à au moins 48 dp, sans forcément grossir le glyphe.")
    if any(c["result"] == "REVIEW" for c in target_review["checks"]):
        result.insert(0, "Ajouter une marge de sécurité sous le contrôle pour la barre de navigation Android.")
    if sheet and sheet.get("top_fraction", 0) > .45:
        result.insert(0, "La feuille occupe moins de 55 % de l'écran : tester l'expansion pour le contenu long.")
    if sheet and sheet.get("scroll_segments", 0) > 1:
        result.append("Vérifier chacun des segments du contenu défilant, pas uniquement le premier.")
    return result

def esc(value):
    return html.escape(str(value if value is not None else ""), quote=True)

def make_report(out, steps, source_root=None, density=None, sheet_gallery=None,
                duplicate_count=0, display_height=None):
    out = Path(out)
    sheet_gallery = sheet_gallery or {}
    records = []
    cards = []
    md = ["# Klyro — audit par composant", "",
          "Le code est extrait du vrai dépôt. Les conseils UI/UX sont des heuristiques, pas une expertise visuelle automatique.",
          f"Captures répétées économisées : {duplicate_count}.", ""]
    for i, step in enumerate(steps, 1):
        target = examine_target(step.get("target"), density, display_height)
        source = source_for(step["name"], step.get("from_stage"), source_root)
        related = related_code_for(step["name"], source_root)
        tokens = style_tokens(source.get("code"))
        gallery = sheet_gallery.get(step.get("destination", ""))
        ideas = improvements(step["name"], target, gallery)
        if tokens:
            ideas.append("Comparer les jetons d'apparence de l'extrait avec les composants voisins : " +
                         ", ".join(tokens[:5]) + ".")
        records.append({**step, "component": target, "source": source,
                        "related_code": related, "style_tokens": tokens,
                        "recommendations": ideas})
        tests_html = "".join("<li><strong>" + esc(t["result"]) + "</strong> " +
                             esc(t["check"]) + " — " + esc(t["value"]) + "</li>"
                             for t in target["checks"])
        ideas_html = "".join("<li>" + esc(idea) + "</li>" for idea in ideas)
        source_html = ('<a href="' + esc(source["url"]) + '">' + esc(source["path"]) + '</a>'
                       if source.get("url") else esc(source.get("description", "")))
        snippet = source.get("code", source.get("description", ""))
        related_html = "".join(
            '<details><summary>' + esc(item["name"]) + ' · ' + esc(item["status"]) +
            '</summary><p><a href="' + esc(item.get("url", "")) + '">' +
            esc(item.get("path", "")) + '</a></p><pre><code>' +
            esc(item.get("code", item.get("description", ""))) +
            '</code></pre></details>' for item in related)
        tokens_html = ('<p>Jetons Kotlin : ' + esc(", ".join(tokens)) + '</p>') if tokens else ""
        cards.append(
            '<article><h3>' + esc(str(i) + ". " + step["name"]) +
            ' <small>' + esc(step["status"]) + '</small></h3>' +
            '<p>Élément : ' + esc(target.get("label", "Non enregistré")) +
            ' · coordonnées : ' + esc(target.get("bounds", "")) + '</p>' +
            '<img class="sample" src="' + esc(step.get("after", "")) +
            '" alt="Capture liée à cette action">' +
            '<h4>Vérifications développeur / accessibilité</h4><ul>' + tests_html +
            '</ul><h4>Améliorations UI / UX</h4><ul>' + ideas_html +
            '</ul><p>Code réel : ' + source_html + ' (' + esc(source["status"]) + ')</p>' +
            tokens_html +
            '<details><summary>Afficher l’extrait Kotlin et ses numéros de ligne</summary><pre><code>' +
            esc(snippet) + '</code></pre></details>' + related_html + '</article>'
        )
        md.extend(["## " + step["name"] + " — " + step["status"],
                   "Composant : " + target.get("label", "non relevé"), ""])
        md.extend("- " + t["result"] + " " + t["check"] + ": " + t["value"] for t in target["checks"])
        md.extend(["", "### Suggestions"])
        md.extend("- " + v for v in ideas)
        md.extend(["", "### Kotlin : " + source.get("path", "inconnu"),
                   source.get("url", ""), "~~~kotlin", snippet, "~~~", ""])
    gallery_html = []
    for page, record in sheet_gallery.items():
        pictures = record.get("images", [])
        if not pictures:
            continue
        figures = "".join('<figure><img src="' + esc(p) + '" loading="lazy">' +
                          '<figcaption>' + esc(p) + '</figcaption></figure>' for p in pictures)
        gallery_html.append('<section><h3>' + esc(page) + ' · ' +
                            str(len(pictures)) + ' vue(s)</h3><p>Expansion : ' +
                            esc(record.get("expanded", "non vérifiée")) +
                            ' · segments scroll : ' + str(record.get("scroll_segments", 0)) +
                            '</p><div class="gallery">' + figures + '</div></section>')
    page = """<!doctype html><html lang="fr"><head><meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Klyro — code et revue UX</title>
    <style>
    body{max-width:1160px;margin:auto;padding:24px;font:15px/1.55 system-ui;background:#f4f6f3;color:#1d261d}
    header,article,section{background:#fff;border-radius:14px;border:1px solid #dce3d9;padding:18px;margin-bottom:16px}
    h1,h2,h3{line-height:1.3}.sample{max-height:430px;max-width:230px;object-fit:contain}
    .gallery{display:flex;overflow-x:auto;gap:12px}.gallery figure{margin:0;flex:none}
    .gallery img{width:240px;max-height:560px;object-fit:contain}
    figcaption{font-size:12px}pre{background:#17221a;color:#ebf4eb;overflow:auto;padding:12px}
    a{color:#355c27}summary{cursor:pointer}
    </style></head><body>""" + (
        '<header><h1>Klyro — audit par composant et code Kotlin</h1><p>' +
        str(len(steps)) + ' assertions · ' + str(duplicate_count) +
        ' doublons de capture évités. Évaluation des zones tactiles et recommandations ciblées.</p>' +
        '<p>Les recommandations sont des règles explicites et doivent être validées sur appareil.</p></header>'
        '<h2>Feuilles complètes et défilement</h2>' + "".join(gallery_html) +
        '<h2>Composants testés</h2>' + "".join(cards) + '</body></html>')
    (out / "ux-review.html").write_text(page, encoding="utf-8")
    (out / "ux-review.md").write_text("\n".join(md), encoding="utf-8")
    (out / "components.json").write_text(
        json.dumps({"density": density, "duplicate_screens_avoided": duplicate_count,
                    "components": records, "sheets": sheet_gallery},
                   ensure_ascii=False, indent=2), encoding="utf-8")
    return records
