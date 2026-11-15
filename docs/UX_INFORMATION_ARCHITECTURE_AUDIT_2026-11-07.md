# Klyro UX / architecture de l'information — audit adversarial (07-11-2026)

## Périmètre et preuves
Branche de travail : fix/klyro-critical-session-20261107.
Base installée validée par l'utilisatrice : 0.4.5-dev.
Cet audit est **statique** : il examine le code et les tests. Il ne prétend pas
avoir observé les nouveaux écrans sur un Pixel 9 ou exécuté des parcours TalkBack.

## Inventaire des actions et source unique

| Action / fonctionnalité | Point d'accès retenu | Logique partagée / source |
|---|---|---|
| Esc, Tab, Ctrl, Alt, Fn, navigation, CLEAR | Clavier terminal | TerminalPane, TerminalNavigationPolicy, TerminalDisplayActions |
| FIND / recherche du transcript | Touche FIND | TranscriptSearch, modal de recherche existante |
| Copier output | Touche ALL / sélection native | Termux TerminalView + sélection explicite |
| Collage | Touche PASTE / collage Android | ClipboardPastePolicy, TerminalSessionClient |
| Éditer, basculer, fermer une session | Onglets et session sheet | TerminalSessionViewModel |
| Grouper et nommer des projets | More > Workspaces | KlyroWorkspaceCatalog / Store / Sheet |
| Réglages et accès fichiers Android | More > Settings, section Files & storage | KlyroSettingsSheet, Android SAF |
| Diagnostic et partage du rapport | Settings > Diagnostics uniquement | KlyroHealthReport, KlyroSettingsSheet |
| Découvrir / insérer les commandes shell | More > Command Palette | KlyroCommandCatalog |
| Lire le message d'accueil | First interactive Bash session + guide dans la palette | KlyroWelcomeGuide |

Un raccourci natif Ctrl+C ou un bouton de copie ne constitue pas nécessairement
un doublon métier, mais deux boutons « copier » dans la palette et le clavier
n'avaient pas de justification contextuelle.

## Constats et décisions

| N° | Problème confirmé sur 0.4.5 | Correction/choix | État de code |
|---|---|---|---|
| UX-01 | Palette composée d'actions d'application déjà présentes ailleurs | Catalogue de commandes shell, insertion sans Enter | Modifié |
| UX-02 | Faux diagnostic de disponibilité des outils | Présence des binaires vérifiée dans le préfixe privé; built-ins identifiés | Modifié |
| UX-03 | termux-setup-storage affichable comme commande alors que l'intégration n'est pas prouvée | Entrée informative, pas de code injecté; renvoi vers SAF | Modifié |
| UX-04 | Diagnostic accessible depuis More en plus des réglages | Section Diagnostics de Settings exclusivement | Modifié |
| UX-05 | Un sheet de diagnostics supplémentaire dupliquait Settings | Suppression de son composant autonome | Modifié |
| UX-06 | Files & storage présent dans More et Settings | Accès principal par Settings | Modifié |
| UX-07 | Copie de sortie présente dans le clavier, Privacy et la palette | Copie conservée au clavier / sélection; Privacy devient informative | Modifié |
| UX-08 | Gestion des sessions dans Settings et dans les onglets | Uniquement dans les onglets et leur sheet contextuel | Modifié |
| UX-09 | Nom du projet aligné à gauche | En-tête centré et gras; noms de projets centrés et gras | Modifié |
| UX-10 | Édition projet par modal empilée sur bottom sheet | Éditeur inline dans le sheet | Modifié |
| UX-11 | Alerte de suppression projet | Dialogue de confirmation conservé car destructif | Conservé |
| UX-12 | Aperçu factice du terminal dans Settings expérimental | Supprimé; le terminal réel existe sous le sheet | Modifié |
| UX-13 | Aperçu de police redondant dans Settings | Supprimé; application directe visible sur terminal réel | Modifié |
| UX-14 | Rôles de couleurs M3 partiellement hérités des valeurs par défaut | surfaceVariant/onSurfaceVariant/outline/error liés aux tokens | Modifié |
| UX-15 | Personnalisation thème et gestes peuvent affecter session | Garder la logique de session hors des sheets | Préservé |
| UX-16 | Aucun guide de première session | Message Klyro/Bash dans .bashrc généré, une seule fois | Modifié |
| UX-17 | Aucune méthode de revoir le guide | Onglet guide à l'intérieur de la palette, sans nouvelle couche modale | Modifié |
| UX-18 | Collage et exécution involontaires possibles si la palette injecte Enter | Insertion littérale sans Enter, interdite en écran alternatif | Modifié |
| UX-19 | Exemples pkg pouvant être interprétés comme compatibles universellement | Avertissements et labels conditionnels, pas de vérification distante automatique | Modifié |
| UX-20 | Aucune défense automatisée contre la réapparition des doublons | Tests de contrats de placement et design system | Ajouté |

## Choix des composants et principes

- **Clavier** : touches contextuelles pour les caractères de contrôle et la navigation, sans les dupliquer dans les menus.
- **Command Palette** : modal bottom sheet avec recherche et liste simple, car il s'agit d'une découverte de commandes à insérer, pas d'un second écran Settings. Guide ouvrable dans la même surface.
- **Workspaces** : modal bottom sheet pour l'ensemble des projets, champs d'édition intégrés. Dialog uniquement pour confirmer une suppression.
- **Settings** : un seul sheet avec sections; diagnostic intégré en contenu documentaire compact, sans deuxième sheet.
- **Preview** : réservée aux décisions qui en ont besoin (ex. une couleur avant application), pas comme décoration de l'interface.
- **Couleurs** : KlyroColors, KlyroUiMetrics et KlyroModalTheme demeurent les seules sources de tokens. Aucun thème Material par défaut ajouté.

## Compatibilité et limites honnêtes

- La vérification des binaires contrôle leur existence dans le préfixe privé Klyro,
  **pas** le succès de leur exécution dans proot. Les built-ins Bash sont accessibles
  seulement lorsqu'une session Bash existe.
- La commande termux-setup-storage n'est pas revendiquée fonctionnelle dans Klyro.
  Utiliser Settings > Files & storage pour le contrat SAF existant.
- Le message d'accueil est injecté uniquement dans les .bashrc générés par Klyro;
  un profil utilisateur entièrement personnalisé sans marqueur Klyro n'est pas
  écrasé ni modifié automatiquement.
- Une sentinelle privée rend l'accueil unique, y compris pour les premiers
  onglets ouverts simultanément. Un nouvel onglet ne doit pas le réimprimer.
- Les commandes proposées par la palette ne doivent jamais être exécutées
  automatiquement. Un programme de plein écran prend priorité sur l'insertion.
- La version debug conserve une mise en page Settings expérimentale; les
  deux variantes utilisent les mêmes tokens. Leur unification est à statuer
  à partir de captures d'écran et de tests appareil, pas d'une assertion CI.

## QA appareil requise

1. Mise à jour sans désinstallation, préservation des données et des onglets.
2. Thèmes clair/sombre + accents Lime/Cyan/Coral/Orchid et contraste des champs.
3. Portrait/paysage, IME ouvert et fermé, défilement des sheets et taille de police 1.5x.
4. Palette : pwd, cd, exit, pkg search, termux-setup-storage (info), absence d'exécution avant Enter.
5. Aide depuis la palette, retour aux commandes dans le même sheet.
6. Settings > Diagnostics, copier/partager, non-exposition de chemins/erreurs.
7. Projet centré et gras, création/renommage/suppression, onglets inchangés.
8. Premier Bash après installation / profil auto-généré, une seule impression du guide.
9. CLEAR, FIND, presse-papiers et persistance de session sans régression.

## Retour arrière
Conserver l'APK 0.4.5-dev validé. Ne pas désinstaller pour contourner un
versionCode Android; si nécessaire, restaurer le SHA précédent dans une
branche de rollback et compiler avec un code de version supérieur, après
sauvegarde. Ne pas fusionner dans main sans validation.
