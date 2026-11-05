# QA Pixel 9 — paire A + B — 0.4.2-dev (code 9)

**État initial : NOT RUN.** Une CI verte ne valide pas le rendu réel.

## A — Launcher / identité visuelle (12 contrôles)
- [ ] A01 — Après `adb install -r`, icône Klyro visible sur Pixel Launcher.
- [ ] A02 — Icône standard affichée ~48dp, ressources natives restant en 108dp.
- [ ] A03 — K et capsule terminal clairement séparés à taille launcher.
- [ ] A04 — Ruban diagonal et pied vertical conformes à la référence fournie.
- [ ] A05 — Pas de contour fluo autour de la capsule.
- [ ] A06 — `>_` lisible dans la capsule à 48dp.
- [ ] A07 — Icône à 24px dans les surfaces Android secondaires encore identifiable.
- [ ] A08 — Masque rond : rien coupé.
- [ ] A09 — Masque squircle : rien coupé.
- [ ] A10 — Thème dynamique activé : monogramme monochrome sans artefact.
- [ ] A11 — Sur fond clair et sombre : contraste monochrome suffisant.
- [ ] A12 — Écran d'accueil de Klyro reprend la même silhouette.

## B — Écrans et interactions (18 contrôles)
- [ ] B01 — Bien distinguer Home (navigation) et Close session (destructif).
- [ ] B02 — Depuis une erreur PTY, Home conserve le processus si possible.
- [ ] B03 — Alertes d'installation et d'erreur lisibles en clair/sombre.
- [ ] B04 — Alertes d'avertissement lisibles pour les 7 accents.
- [ ] B05 — Indicateur de progression Material 3 suit le pourcentage annoncé.
- [ ] B06 — Settings hub compact sans texte tronqué à taille normale.
- [ ] B07 — Settings hub à police 1.5×, toutes les cartes accessibles.
- [ ] B08 — Bento expérimental compact en debug seulement.
- [ ] B09 — Mode release conserve Settings hub non expérimental.
- [ ] B10 — Picker HEX : valeur invalide expliquée, Apply inaccessible.
- [ ] B11 — Modifier HEX ou HSV après un preset annule la sélection provisoire.
- [ ] B12 — Taper Wine/Citron/Berry ne change pas l'accent avant Apply.
- [ ] B13 — Cancel après choix d'un preset conserve l'accent précédent.
- [ ] B14 — Appliquer un preset le persiste en changeant aussi les surfaces.
- [ ] B15 — Après une favorite Preview, retour au picker puis Apply explicite.
- [ ] B16 — Fermer la sous-fenêtre avancée ne valide pas la couleur.
- [ ] B17 — Les modales et CTA partagent leur géométrie et couleur d'action.
- [ ] B18 — Longue recherche, sélection, renommage, END et menu overflow utilisables avec IME.

**REMARQUE:** l'inline FIND n'est pas autorisé ; cette paire ne change pas
l'architecture FIND. L'APK doit rester signé par la clé Klyro, sans effacement.

## Résultat
SHA appareil : `________` ; RUN : `________`.
A = ___/12, B = ___/18. Bloqueur : `________`.
Un échec P0/P1 empêche la fusion. Vérifier ces éléments en plus de la
checklist de 60 cas `QA_PIXEL9_0_4_1_DEV.md`.
