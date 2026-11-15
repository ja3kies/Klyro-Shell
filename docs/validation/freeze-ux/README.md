# Preuves freeze/UX

Source APK : d634b4d. CI : https://github.com/ja3kies/Klyro-Shell/actions/runs/43530165958

- artifacts.json : liens, hashes APK, certificat et comparaison de permissions.
- test-results.json : 326 + 34 JVM, 63 nouveaux cas app, XML CI associés.
- adversarial-matrix.json : préconditions, étapes, attendu, observé et statut.
- stress-1500-pty.json : exact script Java dans le guest jetable et un PTY hôte.
- recorder-benchmark.json : coût noyau Java hôte, pas UI/SAF Android.
- native-host.txt/core-host.txt : exécutions hôte réelles.
- adb-devices.txt/instrumentation-attempt.txt/device-attempt.txt : blocage appareil.

Les copies texte versionnées normalisent les espaces/retours chariot ; journaux
bruts, XML et harness de benchmark sont dans l'archive locale
.local/freeze/freeze-ux-evidence.zip. Les artefacts CI ont une rétention de 14 jours.
Aucun exemple synthétique n'est présenté comme un incident Pixel capturé.

Physique : NOT TESTED. Cause du freeze : UNVERIFIED. Statut : TEST BUILD.
