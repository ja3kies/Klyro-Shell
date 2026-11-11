#!/usr/bin/env python3
"""Render evidence-backed component UX/code reports. Never execute Kotlin source."""
import html
import os
import json
import re
from pathlib import Path
from urllib.parse import quote

ROOT = "app/src/main/java/com/oryn/terminal/"
MAIN = ROOT + "MainActivity.kt"
PLUGINS = ROOT + "ui/KlyroPluginsPage.kt"
SETTINGS = ROOT + "ui/KlyroSettingsSheet.kt"
# The bootstrap resolves one immutable revision so source excerpts remain
# traceable even when this development branch receives further commits.
SOURCE_REV = os.environ.get("KLYRO_QA_SOURCE_COMMIT", "p1/ux-cross-screen-final-pass-20261110")
BASE_URL = "https://github.com/ja3kies/Klyro-Shell/blob/" + quote(SOURCE_REV, safe="/") + "/"

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
    "Diagnostics: Recheck environment": (SETTINGS, "onClick = onRecheckEnvironment"),
    "Custom accent (+)": (SETTINGS, 'onClickLabel = "Choose custom accent"'),
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
    "Custom accent (+)": "Décrire explicitement le bouton + dans les sémantiques Compose et vérifier le lecteur d’écran.",
}

def source_for(action, from_stage, source_root):
    if action.startswith("Back to Settings: "):
        choice = (SETTINGS, "fun navigateBack()")
    elif action == "Navigate: Settings":
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
    if action.startswith("Back to Settings: "):
        result = ["Utiliser le BackHandler natif pour revenir au menu Paramètres depuis une section défilée ; ne pas ramener la feuille vers le bas par geste."]
    else:
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

def findings_from_accessibility(evidence, source_root):
    """Prioritized and deduplicated defects measured on unique screenshots."""
    issues, seen = [], set()
    for snapshot in evidence or []:
        for item in snapshot.get("small_details", []):
            label = item.get("label", "(unknown)")
            key = ("touch", snapshot.get("stage"), label)
            if key in seen:
                continue
            seen.add(key)
            source = (source_for("Diagnostics: Recheck environment", None, source_root)
                      if "Recheck environment" in label else
                      {"status": "UNMAPPED", "description": "Source action not mapped"})
            issues.append({
                "priority": "P1", "kind": "touch-target",
                "screen": snapshot.get("stage", "unknown"),
                "label": label, "bounds": item.get("bounds"),
                "observation": (f"{item.get('width_dp')} × {item.get('height_dp')} dp"
                                " — au moins une dimension sous 48 dp"),
                "recommendation": "Agrandir la zone accessible, corriger les insets si rognée.",
                "source": source,
            })
        for item in snapshot.get("clipped", []):
            label = item.get("label", "(unlabeled)")
            key = ("screen-clip", snapshot.get("stage"), label, item.get("bounds"))
            if key in seen:
                continue
            seen.add(key)
            issues.append({
                "priority": "P1", "kind": "clipped-touch-target",
                "screen": snapshot.get("stage", "unknown"),
                "label": label, "bounds": item.get("bounds"),
                "observation": "Zone interactive en dehors des limites de la capture",
                "recommendation": "Vérifier insets, contraintes et dimensions du composant, puis réévaluer sur Pixel.",
                "source": {"status": "UNMAPPED",
                           "description": "Composant non relié à un fichier Kotlin sans preuve de correspondance"},
            })
        for label in snapshot.get("unlabeled", []):
            # Compose can shift the same icon vertically by hundreds of pixels
            # while scrolling; two viewport captures must not generate two
            # independent issues for that same control.
            match = re.search(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", label)
            box = tuple(int(v) for v in match.groups()) if match else None
            stable_key = (re.sub(r"\s*\[.*", "", label),
                          box[0], box[2] - box[0], box[3] - box[1]) if box else label
            key = ("unlabeled", snapshot.get("stage"), stable_key)
            if key in seen:
                continue
            seen.add(key)
            # This association is grounded in the Appearance screenshots:
            # a single 126px circular '+' at the far-right of Accent.
            # Mark it as an inferred mapping, not runtime proof.
            accent_plus = (snapshot.get("stage") == "appearance"
                           and box is not None and box[0] >= 850
                           and box[2] - box[0] in range(110, 151)
                           and box[3] - box[1] in range(110, 151))
            source = (source_for("Custom accent (+)", None, source_root)
                      if accent_plus else {"status": "UNMAPPED"})
            issues.append({
                "priority": "P1", "kind": "accessibility",
                "screen": snapshot.get("stage"),
                "label": "Custom accent (+)" if accent_plus else label,
                "observed_bounds": label,
                "source_match": "inferred-from-position-and-code" if accent_plus else "unknown",
                "observation": "Contrôle cliquable sans libellé accessible",
                "recommendation": ("Rendre contentDescription explicite sur le bouton +."
                                   if accent_plus else
                                   "Ajouter une description ou un texte enfant lisible."),
                "source": source
            })
        for label in snapshot.get("missing", []):
            key = ("missing", snapshot.get("stage"), label)
            if key not in seen:
                seen.add(key)
                issues.append({
                    "priority": "P2", "kind": "missing-marker",
                    "screen": snapshot.get("stage"), "label": label,
                    "observation": "Marqueur attendu absent de la capture visible",
                    "recommendation": "Vérifier la navigation et la position de défilement.",
                    "source": {"status": "UNMAPPED"}
                })
    return issues

def esc(value):
    return html.escape(str(value if value is not None else ""), quote=True)

def make_report(out, steps, source_root=None, density=None, sheet_gallery=None,
                duplicate_count=0, display_height=None, display_width=None,
                accessibility_evidence=None):
    out = Path(out)
    sheet_gallery = sheet_gallery or {}
    records = []
    cards = []
    md = ["# Klyro — audit par composant", "",
          "Révision du code analysé : " + SOURCE_REV, "",
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
        bounds = target.get("bounds")
        ratio = (str(display_width) + "/" + str(display_height)
                 if display_width and display_height else "9/20")
        overlay = ""
        if bounds and display_width and display_height:
            x1,y1,x2,y2 = bounds
            css = ("left:" + str(round(x1 / display_width * 100, 3)) + "%;" +
                   "top:" + str(round(y1 / display_height * 100, 3)) + "%;" +
                   "width:" + str(round((x2-x1) / display_width * 100, 3)) + "%;" +
                   "height:" + str(round((y2-y1) / display_height * 100, 3)) + "%")
            overlay = '<span class="target-overlay" style="' + css + '"></span>'
        images_html = (
            '<div class="evidence"><figure><div class="screen" style="aspect-ratio:' +
            esc(ratio) + '"><img src="' + esc(step.get("before", "")) +
            '" alt="Écran avant le clic">' + overlay +
            '</div><figcaption>Avant : élément cliqué encadré</figcaption></figure>' +
            '<figure><div class="screen" style="aspect-ratio:' + esc(ratio) +
            '"><img src="' + esc(step.get("after", "")) +
            '" alt="Écran après le clic"></div><figcaption>Après : résultat observé</figcaption>'
            '</figure></div>')
        cards.append(
            '<article><h3>' + esc(str(i) + ". " + step["name"]) +
            ' <small>' + esc(step["status"]) + '</small></h3>' +
            '<p>Élément : ' + esc(target.get("label", "Non enregistré")) +
            ' · coordonnées : ' + esc(target.get("bounds", "")) + '</p>' +
            images_html +
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
    issues = findings_from_accessibility(accessibility_evidence, source_root)
    issues_html = []
    for item in issues:
        source = item.get("source", {})
        link = ('<a href="' + esc(source.get("url", "")) + '">' +
                esc(source.get("path", "Code Kotlin")) + '</a>'
                if source.get("url") else "Code non identifié")
        code = source.get("code")
        details = ('<details><summary>Voir le code Kotlin lié au défaut</summary><pre><code>' +
                   esc(code) + '</code></pre></details>') if code else ""
        issues_html.append(
            '<article><h3>' + esc(item["priority"]) + " · " + esc(item["label"]) +
            '</h3><p>Écran : ' + esc(item.get("screen")) +
            ' · ' + esc(item["observation"]) + '</p><p>Correction : ' +
            esc(item["recommendation"]) + '</p><p>' + link + '</p>' +
            details + '</article>')
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
    h1,h2,h3{line-height:1.3}.evidence{display:flex;flex-wrap:wrap;gap:18px}
    .evidence figure{margin:0}.screen{width:200px;position:relative;overflow:hidden;border:1px solid #dde3d8;border-radius:12px}
    .screen img{display:block;width:100%;height:100%;object-fit:fill}
    .target-overlay{position:absolute;box-sizing:border-box;border:3px solid #e45b24;background:#e45b2424;pointer-events:none}
    .gallery{display:flex;overflow-x:auto;gap:12px}.gallery figure{margin:0;flex:none}
    .gallery img{width:240px;max-height:560px;object-fit:contain}
    figcaption{font-size:12px}pre{background:#17221a;color:#ebf4eb;overflow:auto;padding:12px}
    a{color:#355c27}summary{cursor:pointer}
    </style></head><body>""" + (
        '<header><h1>Klyro — audit par composant et code Kotlin</h1><p>' +
        str(len(steps)) + ' assertions · ' + str(duplicate_count) +
        ' doublons de capture évités. Évaluation des zones tactiles et recommandations ciblées.</p>' +
        '<p>Révision Kotlin/robot : <code>' + esc(SOURCE_REV) + '</code></p>' +
        '<p>Les recommandations sont des règles explicites et doivent être validées sur appareil.</p></header>'
        '<h2>Problèmes prioritaires détectés (' + str(len(issues)) + ')</h2>' +
        ("".join(issues_html) if issues_html else
         "<p>Pas de défaut détecté par ces seules règles statiques.</p>") +
        '<h2>Feuilles complètes et défilement</h2>' + "".join(gallery_html) +
        '<h2>Composants testés</h2>' + "".join(cards) + '</body></html>')
    (out / "ux-review.html").write_text(page, encoding="utf-8")
    md += ["## Problèmes prioritaires", ""]
    md += [("- " + x["priority"] + " " + x["label"] + ": " +
            x["observation"] + " — " + x["recommendation"]) for x in issues]
    (out / "ux-review.md").write_text("\n".join(md), encoding="utf-8")
    (out / "components.json").write_text(
        json.dumps({"density": density, "source_revision": SOURCE_REV,
                    "duplicate_screens_avoided": duplicate_count,
                    "components": records, "sheets": sheet_gallery,
                    "findings": issues},
                   ensure_ascii=False, indent=2), encoding="utf-8")
    return records
