# Ricochet — repères pour travailler sur ce dépôt

Rappels de tâches récurrentes. Deux sortes : le **Ricochet**, dont l'échéance repart
du dernier accomplissement, et le **Rendez-vous**, qui suit un calendrier. Ce que fait
l'application est dans [README.md](README.md).

## Langue

Le code, les commentaires et les messages de commit sont en **français**. Les
**identifiants restent sans accents** (`Tache`, `echeance`, `creee`) pour ne pas
gêner l'outillage ; les accents vivent dans les chaînes affichées et les commentaires.

**L'interface, elle, est traduite.** Aucune chaîne affichée dans le code : elles sont
dans `res/values/strings.xml` (**anglais, langue par défaut**) et
`res/values-fr/strings.xml`. Les libellés calculés passent par l'interface `Textes`
(`ui/Textes.kt`) pour rester du Kotlin pur ; leurs tests lisent les **vrais** fichiers
de traduction (`TextesDeTest`), et `TraductionsTest` refuse une chaîne sans son
pendant français. Ajouter une chaîne, c'est donc l'ajouter aux deux fichiers.

## Commandes

```sh
./gradlew test            # tests unitaires, aucun n'a besoin d'un appareil
./gradlew lintDebug
./gradlew installDebug    # déploie sur l'appareil connecté
```

Gradle tourne sur le JDK 25 (`gradle/gradle-daemon-jvm.properties`), qu'il télécharge
lui-même au besoin.

**Toujours vérifier le rapport, pas seulement « BUILD SUCCESSFUL »** :
`app/build/test-results/testDebugUnitTest/*.xml` donne le compte réel.

## Sur un appareil

Pour inspecter les données de l'application (version de débogage) :

```sh
adb shell run-as io.github.ginkoaloe.ricochet cat files/taches.json
adb shell run-as io.github.ginkoaloe.ricochet ls files/images/
```

Pour y **écrire** un fichier, passer par l'entrée standard : une redirection `>` dans
une commande `adb shell` est exécutée par le shell `shell`, qui n'a pas les droits.

```sh
printf '%s' "$JSON" | adb shell "run-as $PKG sh -c 'cat > /data/user/0/$PKG/files/taches.json'"
```

Et **arrêter l'application avant** de réécrire ses fichiers, sinon elle les réécrit.

Pour vérifier une structure d'écran, préférer `uiautomator dump`, qui ne rend que le
texte, à une capture : celle-ci peut montrer des données personnelles.

## Pièges déjà payés

- **`kotlinx-serialization` doit rester accordée au Kotlin du projet.** La 1.9.0 est
  compilée contre `kotlin-stdlib` 2.2.0, comme nous. Monter en 1.10+ casse toute la
  compilation avec des erreurs de métadonnées trompeuses. C'est écrit dans
  `gradle/libs.versions.toml`, ne pas « mettre à jour » sans vérifier.
- **`SwipeToDismissBox` indexe son `LaunchedEffect` sur `onDismiss`.** Une lambda
  recréée à chaque recomposition relance l'effet en boucle : un seul balayage avait
  ainsi enregistré plus de cinq mille accomplissements. La lambda doit être
  `remember`ée (voir `LigneBalayable` dans `ui/EcranListe.kt`).
- **Un Rendez-vous ne compte qu'à partir de sa référence**, pas de la création de la
  tâche : sans quoi un vieux Ricochet converti, ou un *Fait* antidaté, ferait surgir
  des dizaines d'oublis. Voir `domaine/Suivi.kt`.
- **Construction reproductible** : F-Droid reconstruit l'application et compare avec
  l'APK publié. Ne rien introduire qui dépende de la machine ou de l'heure de
  construction ; voir les réglages commentés dans `app/build.gradle.kts`.

## Organisation du code

| Paquet | Rôle |
|---|---|
| `domaine` | La règle métier, **sans aucune dépendance Android**, donc testable seule : tâches, rythmes, calendriers, ce que solde un *Fait*, l'agenda. |
| `donnees` | Stockage JSON, images, archive zip d'export, CSV. Testable sur JVM. |
| `ui` | Compose : écrans, libellés (à travers `Textes`), chargement d'images. |
| `rappels` | Alarmes exactes, notifications, récepteurs. |

La règle qui justifie le projet tient en une ligne, dans `domaine/Tache.kt` :

```kotlin
is Rythme.Ricochet -> depuis.plus(rythme.periode.toJavaDuration())
```

`depuis` vaut le dernier accomplissement, ou à défaut la date de création. L'échéance
d'un Ricochet repart donc toujours de ce qui a été **fait**, jamais de l'échéance
précédente.

## Agent skills

### Issue tracker

Les issues GitHub de ce dépôt, rédigées en français, via `gh`. Voir
`docs/agents/issue-tracker.md`.

### Triage labels

Les cinq étiquettes par défaut (`needs-triage`, `needs-info`, `ready-for-agent`,
`ready-for-human`, `wontfix`). Voir `docs/agents/triage-labels.md`.

### Domain docs

Contexte unique : `CONTEXT.md` et `docs/adr/` à la racine. Voir `docs/agents/domain.md`.
