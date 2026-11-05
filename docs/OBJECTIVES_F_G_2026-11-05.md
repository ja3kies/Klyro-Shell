# CAVEMAN — prochaine paire F + G (travail en cours)

**Base** : v0.4.1-dev / code 8, objectifs D/E compilés. **Statut** : plan et premier correctif, pas 20 corrections terminées. Main Git intacte.

## F — Terminal réellement opérationnel (minimum 10 contrôles/corrections)

1. Vérification Bash par marqueurs de lignes exactes, sans faux positifs — **code modifié, CI à confirmer**.
2. Contrat argv[0] JNI et copie défensive — **tests ajoutés**.
3. Vérifier UID réel du shell et absence de repli vers Termux.
4. Vérifier HOME, PREFIX, TMPDIR et isolation des chemins.
5. Vérifier dimensions PTY et SIGWINCH après clavier/rotation.
6. Vérifier signaux Ctrl+C, Ctrl+D et gestion de la sortie du shell.
7. Vérifier Unicode/UTF-8 et séquences ANSI.
8. Vérifier sessions parallèles, cwd et historique distincts.
9. Vérifier erreurs apt/pkg sans présenter READY prématurément.
10. Vérifier intégrité et récupération des actifs ARM64 après interruption.
11. Vérifier reprise et feedback lors d'une installation lente.
12. Vérifier absence d'accès aux données privées Termux.

## G — Résilience, observabilité et UX des échecs (minimum 10 contrôles/corrections)

1. Tests de messages d'erreur orientés action, sans stack trace visible.
2. Réduction et masquage des diagnostics sensibles.
3. Gestion d'annulation sans destruction de session.
4. Timeout borné des vérifications d'environnement.
5. États distincts READY, FAILED, CHECKING, MISSING.
6. Prévention des doubles clics et vérifications concurrentes.
7. Tests d'arrière-plan/retour sans duplication PTY.
8. Contrôles de fermeture de session et confirmation.
9. Audit des chemins SAF et des import/export interrompus.
10. Contrôles d'accessibilité des messages et actions.
11. Tests orientation, IME et navigation Home.
12. Journal QA appareil avec SHA exact, version et résultat reproductible.

## Règles de livraison

- Ne jamais annoncer un contrôle comme PASS sans test correspondant.
- Exécuter CI pour chaque lot ; aucun merge dans main sans validation.
- Tests Android physiques séparés de CI ; ne pas tuer de processus ni effacer de données sans accord.
- Le seuil **10 corrections réelles par objectif** reste à satisfaire avant de déclarer la paire terminée.
