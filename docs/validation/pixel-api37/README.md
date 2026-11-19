# Pixel 9, Android 17

La branche corrective part de `da9cfac`. La version de départ installée était `0.4.24-dev.1285.1`. Le Pixel a reproduit les 80 échecs signalés sur 98 tests. Le registre [baseline.tsv](baseline.tsv) contient les 80 méthodes et leurs erreurs, relevées sur le profil de test secondaire.

## Causes et corrections

- 78 tests échouaient pendant l’initialisation d’Espresso. Compose UI Test dépendait transitivement d’une version qui appelait par réflexion une méthode privée absente sur ce Pixel. L’APK instrumenté utilise maintenant Espresso 3.7.0, Runner 1.7.0 et AndroidX JUnit 1.3.0. Le BOM de l’application reste identique. [Correctif AndroidX](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0)
- Une sortie Bash avec le code 0 appelait directement `closeTab(id)`. La sortie conserve désormais l’émulateur, son texte et son identifiant jusqu’à la fermeture explicite. Une sortie réussie conserve un message d’échec nul même après une nouvelle sélection.
- Un callback dupliqué pour la même session remplaçait le premier code de sortie. Le modèle vérifie l’identité de la session et accepte uniquement la première transition. Le service reçoit le changement d’inventaire une seule fois. Les deux assertions instrumentées originales sont conservées. Deux anciens contrats Python exigeaient la fermeture automatique et ont été alignés sur la rétention demandée.

## Compilation et contrôles

Commit testé `1a098177f7f499ce425fbf6abb415a0d79fc2eac`. Version `0.4.24-dev.1286.1`, versionCode 128632. La CI a exécuté 440 tests JVM sans échec ni omission, lint, les tests Python et JNI, et le contrôle ELF 16 KiB. Sur l’hôte local, 655 tests Python réussis. Le test PTY a transmis 20 000 lignes, conservé quatre descripteurs avant et après et vérifié UTF-8 via JNI. [GitHub Actions](https://github.com/ja3kies/Klyro-Shell/actions/runs/44505649058)

Les deux APK ont passé la vérification locale de signature et ont été installés avec remplacement sur le profil de test existant, sans effacement. Certificat SHA-256 commun `01a7e05326237fdfa4cda54e75fb8164cbe12bbc964d36e2d9c2d1a8ef7e5c14`.

| Artefact | SHA-256 |
|---|---|
| [APK ARM64](https://github.com/ja3kies/Klyro-Shell/actions/runs/44505649058/artifacts/14519756579) | `c612d5e32f7be6a63eb6825107634f4aacf6cc7c300a3d653c61cb5e732f2b5e` |
| [APK instrumenté](https://github.com/ja3kies/Klyro-Shell/actions/runs/44505649058/artifacts/14518229550) | `efa2344e7b87bd59a25af3c42f00202db7978911f9bebe762d0760cbe4c8238f` |

## Exécution sur appareil après correction

Le runner a renvoyé 18 résultats individuels de réussite. Les cinq tests de rétention passent, dont les deux échecs initiaux. Les quatre tests PTY natifs, cinq tests de reprise du terminal et quatre tests de diagnostic passent également. [Résultats individuels](after.tsv)

Le profil dédié `KlyroTest` (utilisateur Android 10) était déverrouillé en arrière-plan. Pour le test Compose de taille de police, le runner a signalé le début du test. WindowManager indiquait `INITIALIZING` et `mVisibleRequested=false` pour sa ComponentActivity. Le profil principal occupait l’écran et Android a signalé que ce Pixel ne prend pas en charge les utilisateurs secondaires visibles en arrière-plan. Le processus instrumenté de test a été arrêté à ce stade, avant tout résultat pour cette méthode. La suite complète corrigée et les interactions tactiles restent à valider. Aucun effacement des données applicatives.

Pour reprendre, rendre le profil KlyroTest visible et déverrouillé. Son dossier privé ne contenait pas de runtime installé lors de l’inspection. Effectuer la première installation de Klyro dans ce profil pour les tests exigeant Bash, puis lancer la suite avec les tests d’acceptation activés. Conserver les statuts individuels, y compris les préconditions non satisfaites.

```sh
adb shell am instrument --user 10 -w -r -e klyroRuntimeAcceptance true com.klyro.shell.test/androidx.test.runner.AndroidJUnitRunner
```
