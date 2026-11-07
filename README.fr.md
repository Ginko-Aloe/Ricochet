# Ricochet

*[English version](README.md)*

Des rappels pour les tâches récurrentes, qui **rebondissent sur le moment où on les
fait**.

Arroser les plantes tous les 3 jours. Si on le fait avec 2 jours de retard, le
prochain arrosage est dû 3 jours *après l'arrosage* — pas demain. Chaque fois qu'on
marque une tâche faite, son échéance *ricoche* une période plus loin. Pas
d'arriéré d'occasions manquées, pas de culpabilité : une tâche oubliée un mois est
simplement une tâche à faire.

Pour ce qui relève vraiment du calendrier — le loyer le 1er, les poubelles un
dimanche sur deux — Ricochet fait aussi des **rendez-vous**.

## Fonctions

- **Deux sortes de rappels**
  - **Ricochet** : dû une période après la dernière fois. Des périodes d'une heure à
    plusieurs semaines : une pilule toutes les 8 heures comme un appel tous les
    quinze jours.
  - **Rendez-vous** : à dates fixes — jours de la semaine, jours du mois (le 31 tombe
    sur le dernier jour des mois plus courts), le premier lundi ou le dernier
    vendredi, ou une date annuelle ; toutes les N semaines, mois ou années ; à
    l'heure choisie. Le faire un peu en avance compte pour la date qui vient. Les
    dates manquées sont pardonnées par défaut, ou comptées si on préfère.
- **Une liste triée par urgence**, chaque carte montrant sa photo sous un voile
  adaptatif. **Balayer une carte la marque faite**, avec *Annuler*.
- **Un écran de détail** avec la photo en plein écran, le rythme, la prochaine
  échéance et un grand bouton *Fait*.
- **L'historique** de chaque accomplissement, son avance ou son retard, et ses
  corrections : rattraper un oubli, changer une date, retirer une erreur.
- **Un calendrier du mois** montrant ce qui vient — ricochets projetés compris — et
  ce qui a été fait.
- **Des notifications à l'heure exacte** avec une action *Fait* et un report :
  on ouvre rarement l'application. Le report dure un huitième de la période, entre
  15 minutes et 12 heures.
- **Sauvegarde** dans une archive zip des tâches et de leurs photos, rangée où l'on
  veut, et **export de l'historique en CSV**.
- **Français et anglais**, selon la langue du téléphone — ou choisie pour Ricochet
  seul dans les réglages d'Android.

## Vie privée

Ricochet n'a **aucun accès au réseau** : ni compte, ni pistage, ni publicité, ni
mesure d'audience. Les tâches et les photos restent dans l'espace privé de
l'application, et n'en sortent que lorsqu'on les exporte soi-même.

Elle demande trois permissions : les **notifications**, les **alarmes exactes**
(pour rappeler à la minute où une tâche est due) et le **démarrage avec le
téléphone** (pour reposer ces alarmes après un redémarrage). Les photos viennent du
sélecteur de photos d'Android ou de l'appareil photo : Ricochet n'a besoin d'accéder
ni au stockage ni à la caméra.

## Installation

Nécessite **Android 14** ou plus récent.

Télécharger l'APK dans les [versions publiées](../../releases) de ce dépôt, ou
laisser [Obtainium](https://github.com/ImranR98/Obtainium) l'installer et le tenir
à jour : ajouter `https://github.com/Ginko-Aloe/Ricochet` comme source.

Les versions sont signées d'un certificat dont l'empreinte SHA-256 est
`3d34cbf829ce12e0524fa111d9cb5188176ee94a60ebe5a655a400b2a800bdec`.
La construction est reproductible : construit depuis le tag d'une version, l'APK est
identique à celui publié, une fois la signature recopiée.

## Construire

```sh
./gradlew test            # tests unitaires, sans appareil
./gradlew assembleDebug   # app/build/outputs/apk/debug/
```

Gradle tourne sur le JDK 25, qu'il télécharge lui-même au besoin ; celui fourni avec
un Android Studio récent convient aussi.

## Traductions

Les textes sont dans `app/src/main/res/values/strings.xml` (anglais) et
`values-fr/strings.xml` (français). D'autres langues sont bienvenues : copier le
fichier anglais dans `values-<langue>/` et le traduire. Un test vérifie que chaque
texte a son équivalent français.

## Remerciements

Ricochet doit beaucoup de son allure à
[Water Me](https://f-droid.org/packages/ch.abertschi.waterme.water_me/)
([sources](https://github.com/abertschi/water-me)), une application de rappel
d'arrosage d'Andrin Bertschi. De Water Me viennent les idées qui donnent son
caractère à Ricochet : **la photo qui occupe le fond de chaque carte, sous un voile
dont l'opacité s'adapte** à la présence d'une image ; **le balayage d'une carte pour
la marquer faite, avec annulation** ; et **l'écran de détail où la photo occupe tout
l'écran** sous le nom de la tâche. Ricochet les étend à toutes sortes de tâches et y
ajoute les rendez-vous, mais l'idée visuelle est celle de Water Me — merci.

Aucun code n'a été repris de Water Me ; les deux applications sont publiées sous
GPL.

## Comment elle a été faite

Le code de Ricochet a été écrit avec un modèle d'IA, Claude (Anthropic). Son auteur
a décidé de ce que fait l'application et de son comportement, règle par règle, et a
testé chaque fonction sur un appareil ; le code lui-même a été produit par le modèle,
et vérifié par des tests unitaires et des revues de code. Les commits faits ainsi le
signalent par une ligne `Co-Authored-By: Claude`.

## Licence

[Licence publique générale GNU v3.0 ou ultérieure](LICENSE).
