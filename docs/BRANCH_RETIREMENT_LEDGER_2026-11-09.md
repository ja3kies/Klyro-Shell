# Klyro — branches de travail et statut de retrait (9 novembre 2026)

Le registre ci-dessous emploie le suffixe visuel **X** pour signaler une branche
qui n'est plus la référence de développement. Ce suffixe est **documentaire** :
le connecteur GitHub ne dispose pas actuellement de l'action de renommage ou de
suppression des refs. Ne pas confondre le nom affiché ci-dessous avec le nom
réel de la branche Git.

- `fix/home-navigation-runtime-state-20261109` → **X** : intégrée dans le
  chantier `p1/home-ux-cycle-15-20261109`, conserver jusqu'aux tests et à
  l'approbation de nettoyage.
- `p1/home-ux-audit-20261109` → **X** : base du cycle de 15 commits,
  conserver jusqu'à validation de l'APK et rollback.
- `p0/select-paste-adversarial-audit-20261109` → **CONSERVER** :
  audit P0 antérieur, QA physique encore attendue.
- `p1/home-ux-cycle-15-20261109` → **ACTIVE** : candidate de validation.

Politique : aucune suppression, aucun renommage simulé et aucune fusion dans
`main` sans contrôle de l'ascendance, du CI, de la QA Pixel et accord préalable.
Les branches d'un dépôt public ne sont pas privées individuellement.
