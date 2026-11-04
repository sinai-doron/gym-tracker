# Gym Tracker

A personal Android app for training: strength workouts in the gym, and runs and walks
outside. Log every set with a rest timer between them, record runs with GPS, and follow
how your weights, distances and steps move over time.

It is built for one person and one training programme. There is no account and no
server: workouts are stored on the phone. The UI is Hebrew-first and right-to-left, with
English available in Settings.

## Features

### First launch

- **Permissions wizard.** A fresh install opens on a welcome page and then one page per
  permission: notifications, step counting, location, Health Connect and music controls.
  Each has an illustration, a short reason, an enable button and Skip; dots at the bottom
  show where you are. Pages that don't apply to the phone are left out.
- **Run it again** from Settings → Permissions.

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

- **A → B → C rotation.** Home suggests the workout after the last one you finished;
  you can pick another before starting.
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
[ADR-0002](docs/adr/0002-hardcoded-training-plan.md). Each of its exercises is linked to the
built-in exercise it is in the exercise library, which the app fills from the catalogue on start
([ADR-0006](docs/adr/0006-exercise-model.md)).

| Workout | Focus |
|---|---|
| A | Chest and back |
| B | Legs and core |
| C | Shoulders and arms |

The plan belongs to a programme, a row in the database: its start date drives the A/B weeks
and the deload weeks (every sixth). The owner's started on 2026-10-26; a fresh install's starts
the day it is first opened.

Every session has a training type, also a row: Strength, Run and Walk are built in.

A user can keep several programmes, one active. The programme chip on home opens the list:
make one active, archive or restore one, or start a new one. A programme's builder adds,
renames, reorders and removes its workouts, and sets when week 1 starts. Tapping a workout opens its
exercises. A workout or programme with history is hidden or archived, never deleted.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM with a use-case layer (`ui` → `domain` → `data`) |
| Dependency injection | Hilt |
| Database | Room (schema version 17, with migrations) |
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
│   ├── catalogue/     Reads the exercise catalogue from assets/exercises/
│   ├── db/            Room database, entities, DAOs and migrations
│   ├── di/            Hilt modules
│   ├── prefs/         DataStore: settings
│   ├── repository/    Session, workout and route repositories; the exercise library
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
assets/exercises/           The exercise catalogue the app ships (built, don't edit)
assets/templates/           Programmes a new user can start from (Full body, Upper / Lower,
                            Push / Pull / Legs, A / B / C): library exercises by slug;
                            ProgramTemplateTest checks them against the catalogue
assets/body_map/            The picker's body figures and their MIT licence, copied from
                            react-native-body-highlighter by scripts/build_body_map.py
catalogue/            The built-in exercise catalogue: written by hand, built into
                      assets/exercises/ by build_catalogue.py (see its README)
catalogue/wger/       Pinned snapshot of wger's exercise data, the upstream of our
                      exercise catalogue; how to refresh it and what we owe in its README
scripts/              build_body_map.py (refreshes assets/body_map/; how is in the
                      script), check-hebrew-literals.sh
docs/
├── adr/               Architecture decision records
└── superpowers/plans/ Implementation plans
CONTEXT.md             Domain glossary
```

## Getting started

### Requirements

- **Android Studio** (recent stable). Its bundled JDK is enough to build.
- **Android SDK Platform 37**, which Android Studio installs on first sync.

`./gradlew` is the standard Gradle wrapper: it downloads the Gradle version pinned in
`gradle/wrapper/gradle-wrapper.properties` (9.8.1, checked against its SHA-256), so no
system-wide Gradle is needed.

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

### Release builds for Play

Play takes an Android App Bundle (`.aab`) signed with an **upload key**. With Play App Signing,
Google keeps the key that signs what phones install. The upload key only proves an upload is
yours, and Google can replace it if it is lost.

1. Make the upload key once, outside the repository, and keep it and its password in your
   password manager (keytool asks for the password, so it never lands in your shell history):

   ```sh
   mkdir -p ~/keys
   keytool -genkeypair -keystore ~/keys/gym-tracker-upload.jks -storetype PKCS12 \
     -alias upload -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=neonsheep"
   ```

2. Point `local.properties` at it:

   ```properties
   release.storeFile=/Users/<you>/keys/gym-tracker-upload.jks
   release.storePassword=<the password>
   ```

   (`release.keyAlias` defaults to `upload`, `release.keyPassword` to the store password.) CI can
   take the same values as `RELEASE_STOREFILE`, `RELEASE_STOREPASSWORD`, `RELEASE_KEYALIAS` and
   `RELEASE_KEYPASSWORD`.

3. Raise `versionCode` in `app/build.gradle.kts` (Play refuses a code it has seen), then build:

   ```sh
   ./gradlew bundleRelease   # app/build/outputs/bundle/release/app-release.aab
   ```

Without the key the bundle is built unsigned, which is what CI does to prove a release still
builds; Play refuses an unsigned upload. The release build isn't shrunk yet (no R8): Room, Hilt,
serialization and MapLibre each need keep rules first.

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
./gradlew verifyComposeVersions      # fails if a library pulls Compose past the BOM at run time (CI runs it)
./gradlew connectedDebugAndroidTest  # instrumented tests: Room, sync, migrations (an emulator, never your phone)
```

`connectedDebugAndroidTest` uninstalls the app when it finishes, which deletes its data: run
it on an emulator. See [docs/testing/database-migrations.md](docs/testing/database-migrations.md).

### CI

GitHub Actions ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) runs on every push to
`main` and every pull request:

- **Build and unit tests:** `assembleDebug`, `testDebugUnitTest` and an unsigned `bundleRelease`. It fails if the build
  changed a Room schema in `app/schemas/` that wasn't committed; the generated schemas are
  attached to the run as the `room-schemas` artifact. It also fails when the exercise
  catalogue breaks a rule or wasn't rebuilt after a change
  (`python3 -I catalogue/build_catalogue.py --check`).
- **Instrumented tests:** `connectedDebugAndroidTest` on an API 34 emulator.

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
- [0006 — Exercises declare their metrics; a set is made of parts](docs/adr/0006-exercise-model.md)

The domain vocabulary used throughout the code is defined in [`CONTEXT.md`](CONTEXT.md).
