# Ricochet

*[Version française](README.fr.md)*

Reminders for recurring chores that **bounce off the moment you do them**.

Water the plants every 3 days. If you do it 2 days late, the next watering is due
3 days *after you watered* — not tomorrow. Every time you mark a task done, its due
date *ricochets* one period further. No backlog of missed occurrences, no guilt: a
task forgotten for a month is simply one task that is due.

For things that do belong to the calendar — rent on the 1st, bins every other
Sunday — Ricochet also does **appointments**.

## Features

- **Two kinds of reminders**
  - **Ricochet**: due one period after it was last done. Periods from one hour to
    many weeks, so it fits a pill every 8 hours as well as a call every fortnight.
  - **Appointment**: on fixed dates — weekdays, days of the month (the 31st falls on
    the last day of shorter months), the first Monday or last Friday, or a yearly
    date; every N weeks, months or years; at a time of your choice. Doing it a bit
    early counts for the coming date. Missed dates are forgiven by default, or kept
    count of if you prefer.
- **A list sorted by urgency**, each card showing its photo under an adaptive veil.
  **Swipe a card to mark it done**, with *Undo*.
- **A detail screen** with the photo full-screen, the rhythm, the next due date and
  a large *Done* button.
- **History** of every completion, how early or late it was, and corrections:
  add a forgotten one, change a date, remove a mistake.
- **A month calendar** showing what is coming — projected ricochets included — and
  what was done.
- **Exact-time notifications** with a *Done* action and a snooze, so you rarely need
  to open the app. The snooze lasts an eighth of the period, between 15 minutes and
  12 hours.
- **Backup** as a zip archive of tasks and photos, saved wherever you choose, and an
  **export of the history as CSV**.
- **English and French**, following your phone's language — or chosen for Ricochet
  alone in Android's settings.

## Privacy

Ricochet has **no network access**: no account, no tracking, no ads, no analytics.
Your tasks and photos stay in the app's private storage, and leave it only when you
export them yourself.

It asks for three permissions: **notifications**, **exact alarms** (to remind you at
the minute a task is due) and **start at boot** (to set those alarms again after a
restart). Photos come from Android's photo picker or the camera app, so Ricochet
needs no access to your storage or camera.

## Install

Requires **Android 14** or later.

Ricochet will be available on [IzzyOnDroid](https://apt.izzysoft.de/fdroid/) and
[F-Droid](https://f-droid.org/), and as an APK in this repository's
[releases](../../releases).

## Build

```sh
./gradlew test            # unit tests, no device needed
./gradlew assembleDebug   # app/build/outputs/apk/debug/
```

Gradle runs on JDK 25, which it downloads by itself if needed; the JDK bundled with
a recent Android Studio works too.

## Translations

Strings live in `app/src/main/res/values/strings.xml` (English) and
`values-fr/strings.xml` (French). Other languages are welcome: copy the English file
into `values-<language>/` and translate. A test checks that every string has its
French counterpart.

## Acknowledgements

Ricochet owes much of its look and feel to
[Water Me](https://f-droid.org/packages/ch.abertschi.waterme.water_me/)
([source](https://github.com/abertschi/water-me)), a plant watering app by
Andrin Bertschi. From Water Me come the ideas that give Ricochet its
character: **the photo filling the background of each card, under a veil whose
opacity adapts** to whether there is a picture; **swiping a card to mark it done,
with undo**; and **the detail screen where the photo fills the whole screen** under
the task's name. Ricochet generalises them to any chore and adds fixed-date
appointments, but the visual idea is Water Me’s — thank you.

No code was copied from Water Me; both apps are released under the GPL.

## License

[GNU General Public License v3.0 or later](LICENSE).
