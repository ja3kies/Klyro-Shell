# Campagne P0 — progression

Source distante : `022d758`, branche `fix/p0-reliability-closure`.
Le dépôt initial était propre. Les validations de novembre 12 sont conservées
comme antérieures, pas comptées comme actions de cette campagne.

Flux actif : Bash → PRoot → slave/master PTY (JNI termux.c) → lecteur ByteQueue
→ Handler/TerminalEmulator (thread principal) → OrynTerminalSessionClient
→ rafraîchissement TerminalView + notifications Compose regroupées à 120 ms.
Le regroupement Compose ne ralentit ni le lecteur PTY ni ses octets.

## Cycle 1 — implémentation et tests hôte terminés

Défauts CONFIRMED par code et cas ciblés : la suppression du dernier identifiant
pending permettait de programmer un second timer pendant que le premier restait
actif ; SELECT allouait toujours le transcript entier avant de le borner ; une
cellule acceptait une quantité illimitée de caractères combinants.

Correctifs : ownership explicite du timer, suffixe de transcript borné avant
allocation, plafond 64 unités UTF-16 par cellule pour le texte combinant.
Preuves comportementales : dix scénarios OutputUpdateBatchTest (horloge virtuelle
120 ms, rafale 10 000 chunks, dernière notification, fermeture, réentrance,
exception), cinq OutputBoundsTest (scrollback 50 000 lignes, UTF-16 et flood
100 000 caractères combinants). 15 nouveaux scénarios comportementaux : PASS dans les deux tâches JVM Gradle. La CI finale doit encore confirmer le lot.
Ces tests ne constituent pas une mesure de latence Settings ou un test Android.

P0-06 à P0-10 : UNVERIFIED, réservés aux cycles 4–6. ANR utilisateur original :
UNVERIFIED sans trace. Appareil ADB : aucun connecté au début de campagne.
