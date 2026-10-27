# Gym Tracker

A personal Android app for logging strength workouts: pick today's workout, log every set,
rest between sets on a timer, and see how your weights move over time.

It is built for one person and one training programme. Everything is stored on the phone —
there is no account, no server and no network access — and the UI is Hebrew-first and
right-to-left, with English available in Settings.

## Features

**Workouts and sessions**
- **A → B → C rotation.** The home screen suggests the next workout in the cycle; you can
  pick a different one before starting.
- **Session clock.** A session runs from when you start it until you end it. The app never
  closes one on its own.
- **Three kinds of exercise**: rep-based (reps × weight), timed (e.g. plank, with a
  start/stop timer) and distance (e.g. farmer's walk).
- **Weight carries forward.** Each set is pre-filled from the previous set, and the first
  set from your last session, so you rarely retype a number.
- **Edit a logged set** by tapping it.
- **Workout summary** when you end a session.

**Rest timer**
- Starts automatically after each set, using the rest time defined for that exercise.
- Adjust it with +/- or dismiss it early.
- A 3-2-1 beep countdown with vibration before it ends.
- A foreground notification shows the rest countdown, so you can follow it from the lock
  screen.

**Programme logic**
- **Weekly exercise variants.** Some exercises alternate between two versions (e.g.
  dumbbell vs. barbell incline press) on alternate weeks, counted from the programme start
  date.
- **Deload week.** Every sixth week the session shows a deload banner (60% of last weight,
  2 sets).
- **Progression hints** per exercise, based on your last three sessions:
  - *Increase*: you hit the target reps three sessions running. Add 2.5 kg (target ≤ 12
    reps) or 5 kg (higher-rep exercises).
  - *Stalled*: same weight and same reps three sessions running.
  - *On track*: anything else.
- Exercises can be marked **optional** or **per side**.

**History**
- A monthly calendar marking the days you trained. Tap a day to see that session.

**Extras**
- **Now playing bar.** Shows the music playing in any app, with play/pause and skip, so
  you don't leave the session screen. This needs notification access (see
  [Permissions](#permissions)).
- **Daily step counter** on the home screen, from the phone's step sensor.

## The training programme

The programme is hardcoded in
[`TrainingPlanData.kt`](app/src/main/java/com/doron/gymtracker/data/seed/TrainingPlanData.kt)
and seeded into the database on first launch. It isn't editable in the app — see
[ADR-0002](docs/adr/0002-hardcoded-training-plan.md).

| Workout | Focus | Exercises |
|---|---|---|
| A | Chest and back | 7, ending with plank |
| B | Legs and core | 8 |
| C | Shoulders and arms | 8, ending with farmer's walk |

The programme start date, which drives the weekly variants and deload weeks, is set in
[`ProgramPrefs.kt`](app/src/main/java/com/doron/gymtracker/data/prefs/ProgramPrefs.kt).

To change the programme, edit those files and reinstall. If sessions have already been
recorded, keep the exercise `id`s stable — history is linked to them.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM with a use-case layer (`ui` → `domain` → `data`) |
| Dependency injection | Hilt |
| Database | Room |
| Settings | DataStore |
| Navigation | Navigation Compose |
| Async | Kotlin coroutines and Flow |
| Tests | JUnit 4, MockK, Turbine, coroutines-test; Room and Compose instrumented tests |
| Min / target SDK | 26 (Android 8.0) / 35 |

## Project structure

```
app/src/main/java/com/doron/gymtracker/
├── data/
│   ├── db/            Room database, entities and DAOs
│   ├── di/            Hilt modules
│   ├── prefs/         DataStore: workout rotation, programme start, settings
│   ├── repository/    Session and workout repositories
│   ├── seed/          The hardcoded training programme
│   └── StepCounterManager.kt
├── domain/
│   ├── model/         Workout, ExerciseDefinition, Session, SetRecord, ProgressionHint
│   └── usecase/       Rotation, session lifecycle, set logging, variants, deload, progression
├── media/             Now-playing integration and rest-timer beeps
├── service/           Foreground service for the session and rest timer
└── ui/
    ├── components/    Exercise card, rest timer, now-playing bar, set row
    ├── navigation/
    ├── screens/       home, session, history, settings
    └── theme/
docs/
├── adr/               Architecture decision records
└── superpowers/plans/ Implementation plans
CONTEXT.md             Domain glossary (Session, ExerciseEntry, SetRecord, …)
```

## Getting started

### Requirements

- **Android Studio** (recent stable). Its bundled JDK is enough to build.
- **Android SDK Platform 35**, which Android Studio installs on first sync.
- **Gradle 9.x on your `PATH`.** See the note below.

> **About `gradlew`:** the `gradlew` script in this repo is not the standard Gradle
> wrapper. It just runs `exec gradle "$@"`, so you need Gradle installed system-wide
> (`brew install gradle` on macOS). Running `gradle wrapper` once would replace it with a
> real wrapper pinned to the version in `gradle/wrapper/gradle-wrapper.properties`.

### Build and run

In Android Studio: **File → Open**, choose this folder, wait for the Gradle sync, then
**Run ▶**.

From the command line:

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"  # macOS

./gradlew assembleDebug        # → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # build and install on a connected device
```

### Running on a phone

1. **Settings → About phone** — tap **Build number** seven times to enable developer
   options.
2. **Settings → System → Developer options** — turn on **USB debugging**.
3. Connect the phone and accept the "Allow USB debugging?" prompt.
4. Check it's visible with `adb devices -l`, then run `./gradlew installDebug`.

For wireless debugging, turn on **Wireless debugging** in the same menu, choose **Pair
device with pairing code**, then run `adb pair <ip>:<port>` and `adb connect <ip>:<port>`.

### Tests

```sh
./gradlew testDebugUnitTest          # unit tests: use cases, view models, repository
./gradlew connectedDebugAndroidTest  # instrumented tests: Room DAOs (needs a device)
```

## Permissions

| Permission | Why |
|---|---|
| Notifications | The foreground notification with the session and rest timer |
| Foreground service | Keeps the timer running while the screen is off or you're in another app |
| Physical activity | The daily step counter |
| Vibrate | The end-of-rest countdown |
| Notification access *(optional)* | Lets the now-playing bar read and control music in other apps. Granted in system settings; the app asks once and remembers if you dismiss it. |

## Data and privacy

All data stays on the device in a local Room database: sessions, sets, weights and
settings. Nothing is sent anywhere. The flip side is that uninstalling the app or wiping
the phone deletes your history. See [ADR-0001](docs/adr/0001-local-only-no-backend.md).

## Design decisions

The reasoning behind the main choices is recorded in [`docs/adr`](docs/adr):

- [0001 — Local-only storage, no backend, no authentication](docs/adr/0001-local-only-no-backend.md)
- [0002 — The training plan is hardcoded, not user-editable](docs/adr/0002-hardcoded-training-plan.md)
- [0003 — Distinct exercise types (rep-based and timed)](docs/adr/0003-two-exercise-types.md)
- [0004 — Hebrew UI with right-to-left forced at the app root](docs/adr/0004-hebrew-rtl-single-language.md)

The domain vocabulary used throughout the code is defined in [`CONTEXT.md`](CONTEXT.md).
