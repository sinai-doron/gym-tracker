# Gym Tracker

A personal Android app for training: strength workouts in the gym, and runs and walks
outside. Log every set with a rest timer between them, record runs with GPS, and follow
how your weights, distances and steps move over time.

It is built for one person and one training programme. There is no account and no
server: workouts are stored on the phone. The UI is Hebrew-first and right-to-left, with
English available in Settings.

## Features

### Home

- **Two equal cards: gym and cardio.**
  - The gym card shows the next workout, with its exercises collapsed.
  - The cardio card starts a run or a walk and opens saved routes.
- **Week strip.** The days you trained this week, colored purple for gym and green for
  cardio.
- **Steps this week.** A bar per day against your daily step goal, with the part taken
  on runs and walks in green. Steps come from Health Connect and the phone's step sensor.
- **Achievements.**
  - Distance badges: 1K, 5K and 10K.
  - Personal records: fastest 1K, 5K and 10K, longest run and best week. Tap a record to
    open its run.
- **Program status and weather.** Week number, A/B week, weeks to deload, and today's
  weather.

### Gym workouts

- **A → B → C rotation.** Home suggests the next workout in the cycle; you can pick
  another before starting.
- **Session clock.** A session runs from when you start it until you end it. If the app
  is killed mid-workout, home offers to resume it.
- **Three kinds of exercise.**
  - Rep-based: reps × weight.
  - Timed, such as a plank, with a start/stop timer.
  - Distance, such as a farmer's walk.
- **Weight carries forward.** Each set is pre-filled from the previous one, and the
  first set from your last session.
- **Edit or delete a logged set**, even during rest.
- **Workout summary** when you end a session.
- **Rest timer.**
  - Starts after each set with the exercise's rest time.
  - Adjust it with +/- or dismiss it early.
  - Ends with a 3-2-1 beep countdown and vibration. It only beeps; voice is used only on
    runs.
  - Follow it from the lock screen in a foreground notification.
- **Plan editor** (Workouts → Edit).
  - Change an exercise's name, sets, target and rest.
  - Reorder, add and remove exercises.
  - Removal is soft, so history stays.
- **Programme logic.**
  - Weekly exercise variants that alternate on A/B weeks.
  - A deload week every sixth week: 60% of the last weight, 2 sets.
  - Progression hints from your last three sessions: *increase*, *stalled* or *on
    track*.
- **Exercise progress.** For each exercise: its personal record, a chart of the best set
  per workout, and every session's sets.

### Runs and walks

- **GPS recording that survives the screen being off.** A foreground service records
  the run. If the app is killed, the run is picked back up.
- **Live screen.**
  - A map with the route so far.
  - Distance, moving time, current and average pace.
  - Progress towards a target.
- **Targets.** None, a distance, a duration, or a saved route to follow. Following a
  route warns you when you're off it and counts shortcuts.
- **Laps** of a length set in Settings. A final partial lap is shown when the rest is
  10 m or more.
- **Pausing.**
  - **Auto-pause.** The run pauses after 10 seconds without moving.
  - A gap in GPS that still covered ground at walking speed or more isn't a pause.
  - **Manual pause** from the screen or the notification.
- **Voice feedback** (optional, in Hebrew): laps, the target, pause and resume.
- **Run report.**
  - The map colored by slope.
  - Elevation and pace charts.
  - Laps, climb and descent, calories and steps/cadence.
  - New personal records are announced when you finish.
- **Share.**
  - A square image of the route colored by pace. You choose which numbers it shows.
  - A GPX file of the route.
- **Routes.**
  - Save a run's route, or import one from GPX.
  - Name routes automatically from the street.
  - Each route has a details page you can start or edit it from.
- **Health Connect.** Finished runs and walks are written with their route, distance,
  calories and elevation. Steps are read from it.

### History

- **Monthly calendar.** Gym days are purple and cardio days green; a day with both is
  half and half.
- **Session details.** Tap a day to open the gym session or the full run report.

### Settings and extras

- **Settings.**
  - Language: Hebrew or English.
  - Body weight, used for calories.
  - Daily step goal (10,000 by default).
  - Lap length and voice feedback.
- **Backup and restore.**
  - Save the database and settings as one zip, anywhere the system file picker reaches
    (e.g. Google Drive).
  - Restore replaces all data and restarts the app.
- **Now playing bar.** The music playing in any app, with play/pause and skip, so you
  don't leave the session screen. This needs notification access.

## The training programme

The programme is seeded into the database on first launch from
[`TrainingPlanData.kt`](app/src/main/java/com/doron/gymtracker/data/seed/TrainingPlanData.kt).
After that, edit it in the app's plan editor; see
[ADR-0002](docs/adr/0002-hardcoded-training-plan.md).

| Workout | Focus |
|---|---|
| A | Chest and back |
| B | Legs and core |
| C | Shoulders and arms |

The programme start date, which drives the A/B weeks and deload weeks, is set in
[`ProgramPrefs.kt`](app/src/main/java/com/doron/gymtracker/data/prefs/ProgramPrefs.kt).

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM with a use-case layer (`ui` → `domain` → `data`) |
| Dependency injection | Hilt |
| Database | Room (schema version 10, with migrations) |
| Maps | MapLibre, with OpenFreeMap tiles |
| Health data | Health Connect |
| Settings | DataStore |
| Navigation | Navigation Compose |
| Async | Kotlin coroutines and Flow |
| Tests | JUnit 4, MockK, Turbine, coroutines-test; Room and Compose instrumented tests |
| Min / target SDK | 26 (Android 8.0) / 35 |

## Project structure

```
app/src/main/java/com/doron/gymtracker/
├── data/
│   ├── db/            Room database, entities, DAOs and migrations
│   ├── di/            Hilt modules
│   ├── prefs/         DataStore: rotation, programme start, settings
│   ├── repository/    Session, workout and route repositories
│   ├── seed/          The initial training programme
│   ├── CardioRecorder, LocationTracker   Recording a run from GPS fixes
│   ├── ActiveClock, RouteDistance, Laps  Moving time, distance and laps
│   ├── RunStats, RouteFollower           Elevation, pace, best efforts, route following
│   ├── Gpx, RouteNamer                   GPX import/export, street names
│   ├── HealthConnectManager, StepCounterManager
│   └── BackupManager, WeatherRepository
├── domain/
│   ├── model/         Workout, Session, SetRecord, RunDetails, RunTarget, PersonalRecord, …
│   └── usecase/       Rotation, sessions, sets, variants, deload, progression, records
├── media/             Now playing, rest-timer beeps, run voice
├── service/           Foreground services for the gym session and the run, pause receiver
└── ui/
    ├── components/    Shared pieces: run map, charts, activity icons, rest timer, …
    ├── navigation/
    ├── screens/       home, session, workouts, plan, progress, cardio, routes,
    │                  history, settings
    └── theme/
assets/map_style_run.json   The map style
docs/
├── adr/               Architecture decision records
└── superpowers/plans/ Implementation plans
CONTEXT.md             Domain glossary
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

### Signing

Debug builds are signed with a fixed key named in `local.properties`, so installing a
new build updates the app in place instead of wiping its data. The key files are
gitignored and must stay out of this public repository.

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
./gradlew testDebugUnitTest          # unit tests: use cases, view models, run maths (clock, laps, pace, GPX)
./gradlew connectedDebugAndroidTest  # instrumented tests: Room DAOs (needs a device)
```

## Permissions

| Permission | Why |
|---|---|
| Notifications | The foreground notification with the session, rest or run timer |
| Foreground service (special use, location) | Keeps timers and run recording going with the screen off |
| Location (precise) | Recording runs and walks; today's weather |
| Physical activity | The step counter, and steps on runs |
| Vibrate | The end-of-rest countdown |
| Internet | Map tiles, weather and street names |
| Health Connect | Write runs (exercise, route, distance, calories, elevation); read steps, exercise and heart rate |
| Notification access *(optional)* | Lets the now-playing bar read and control music in other apps. The app asks once and remembers if you dismiss it. |

## Data and privacy

Workouts, runs, routes and settings are stored on the device in a local Room database.
Back them up from Settings: uninstalling the app or wiping the phone deletes them
otherwise. See [ADR-0001](docs/adr/0001-local-only-no-backend.md).

The app talks to a few services, and only for these things:

- **OpenFreeMap:** map tiles.
- **Open-Meteo:** weather for your location.
- **OpenStreetMap Nominatim:** street names for new routes.
- **Health Connect,** on the phone itself: runs you finish, steps, and heart rate.

## Known limits

- **Heart rate.** The CMF Watch Pro 2, through Nothing X, writes heart rate to Health
  Connect only once every 5 minutes, about 15 minutes late. That isn't enough for a
  heart-rate chart or zones on a run. It is still to be checked whether a workout started
  on the watch writes more often.
- **Samsung Health** doesn't share exercise routes through Health Connect, so its runs
  can't be imported. A run can be added by hand from a GPX export.

## Design decisions

The reasoning behind the main choices is recorded in [`docs/adr`](docs/adr):

- [0001 — Local-only storage, no backend, no authentication](docs/adr/0001-local-only-no-backend.md)
- [0002 — The training plan is hardcoded, not user-editable](docs/adr/0002-hardcoded-training-plan.md)
- [0003 — Distinct exercise types (rep-based and timed)](docs/adr/0003-two-exercise-types.md)
- [0004 — Hebrew UI with right-to-left forced at the app root](docs/adr/0004-hebrew-rtl-single-language.md)
- [0005 — Runs: moving time, pauses and GPS dropouts](docs/adr/0005-run-moving-time.md)

The domain vocabulary used throughout the code is defined in [`CONTEXT.md`](CONTEXT.md).
