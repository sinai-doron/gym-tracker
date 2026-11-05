# Neonsheep Fitness

Published as **Neonsheep Fitness** (package `dev.neonsheep.gymtracker`); this repository keeps its
first name, gym-tracker.

A personal Android app for training: strength workouts in the gym, and runs and walks
outside. Log every set with a rest timer between them, record runs with GPS, and follow
how your weights, distances and steps move over time.

It is built for one person and one training programme. There is no account and no
server: workouts are stored on the phone. The UI is in English or Hebrew
(right to left): a fresh install follows the phone's language, Hebrew on a Hebrew phone and
English on any other, and Settings can change it.

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

- **Rotation, in order or by weekday.** Home suggests the workout after the last one you
  finished. Give a programme's workouts a day (edit a workout in the programme) and it follows
  the week instead: home shows today's workout, or the next day's ("Next workout · Monday").
  A missed day isn't made up, and a workout without a day is only started by hand. Either
  way, you can pick another before starting.
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
  - Set per programme (tap the rules line in the programme): a deload every 3 to 12 weeks,
    at a share of the usual weight (40–90%) and with at most so many sets; and week B, when
    exercises switch to their variant, every week or every 2 to 4 weeks.
  - In a deload week the session caps each exercise's sets and pre-fills the weight at that
    share, rounded down to a plate (2.5 kg or 5 lb), with no hint to add weight. The plan keeps
    its own sets, and later weeks start again from the usual weight.
  - Progression hints per exercise. Once every set at the top weight reached the top of the
    rep range last time, the hint says to add the exercise's step, which comes from its
    equipment: 2.5 kg on a barbell, 2 kg a dumbbell, 5 kg a machine (2.5 kg when unknown;
    shown as 5 lb in pounds). Three sessions at the same weight and reps short of the top is
    *stalled*. Deload weeks are left out.
- **Exercise progress.** For each exercise: its personal record, a chart of the best set
  per workout, and every session's sets.

### Runs and walks

- **GPS recording that survives the screen being off.** A foreground service records
  the run. If the app is killed, the run is picked back up.
- **Live screen.**
  - A map with the route so far.
  - Distance, moving time, current and average pace.
  - Progress towards a target.
- **Kilometres or miles**, as chosen in About you (Settings): distance, pace, speed, laps,
  targets, the notification and the voice. Climb and elevation go with them, in metres or
  feet. Everything is still stored in metres. The fastest 1, 5 and 10 km records and the
  badges stay metric, as do GPS accuracy and gym exercises measured in metres.
- **Kilograms or pounds**, as chosen in About you: sets are typed and shown in the chosen
  unit, and so are volume, the progression hint (2.5 kg becomes 5 lb) and body weight.
  Weights are still stored in kg. The tonnes badges stay metric.
- **Targets.** None, a distance, a duration, or a saved route to follow. Following a
  route warns you when you're off it and counts shortcuts.
- **Laps** of a length set in Settings (from 200 m to 5 km, or ⅛ to 5 mi; changing units
  moves 1 km to 1 mi and back). A past run keeps the lap unit it was recorded in. A final partial lap is shown when the rest is
  10 m or more.
- **Pausing.**
  - **Auto-pause.** The run pauses after 10 seconds without moving.
  - A gap in GPS that still covered ground at walking speed or more isn't a pause.
  - **Manual pause** from the screen or the notification.
- **Voice feedback** (optional, in the app's language: Hebrew, or English for any other): laps, the target, pause and resume. It needs the phone's speech engine to have that language; without it the run beeps instead.
- **Run report.**
  - The map colored by slope.
  - Elevation, pace and cadence charts.
  - Laps with their time, pace, climb and cadence; climb and descent, calories and steps.
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
- **Heart rate from a watch.** A run's summary shows the average and highest heart rate,
  and a chart along the distance, from whatever a watch wrote to Health Connect during the
  run. It is read each time the run is shown and never stored or synced, and read again on
  coming back to the screen, since watches often sync late. Without the permission, the
  summary offers to connect it.

### History

- **Monthly calendar.** Gym days are purple and cardio days green; a day with both is
  half and half.
- **Session details.** Tap a day to open the gym session or the full run report.

### Settings and extras

- **Settings.**
  - Language: English or Hebrew; until one is picked, the phone's.
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
and the deload weeks (every sixth in the owner's plan, which keeps 60% and 2 sets). The owner's started on 2026-10-26; a fresh install's starts
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
| Database | Room (schema version 18, with migrations) |
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
Google keeps the key that signs what phones install; the upload key only proves an upload is
yours, and Google can replace it if it is lost.

The upload key lives in two places and nowhere else: two secrets in GCP Secret Manager in
`symbio-shared` (`gym-tracker-upload-key`, the key file, and `gym-tracker-upload-key-password`),
and an entry in the owner's Strongbox database with the file attached.

- **Make it, once:** `scripts/make-upload-key.sh`, in a terminal. It asks for a password (typed
  at a prompt, so it is in neither the shell history nor the process list), makes the key in a
  private temporary folder, saves both secrets, then waits while you add the Strongbox entry and
  deletes the file. It refuses to run if the secrets already exist.
- **Build an upload:** raise `versionCode` in `app/build.gradle.kts` (Play refuses a code it has
  seen), then `scripts/bundle-release.sh`. It fetches the key into a private temporary folder,
  runs `bundleRelease`, checks the bundle is signed and deletes the key, whatever happens. The
  bundle is `app/build/outputs/bundle/release/app-release.aab`.

The build itself reads the key from `RELEASE_STOREFILE` and `RELEASE_STOREPASSWORD` (or
`release.*` in `local.properties`); without them the bundle is built unsigned, which is what CI
does to prove a release still builds, and what Play refuses. The release build isn't shrunk yet
(no R8): Room, Hilt, serialization and MapLibre each need keep rules first.

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
| Health Connect | Write runs (exercise, route, distance, calories, elevation); read steps, and heart rate for runs |
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
  Connect only once every 5 minutes, about 15 minutes late. A run then shows its heart
  rate a while after it ends, as a coarse chart; zones would need more. It is still to be
  checked whether a workout started on the watch writes more often.
- **Older runs.** Health Connect lets an app read only 30 days back from when it was first
  given a permission, so runs older than that may show no heart rate.
- **Samsung Health** doesn't share exercise routes through Health Connect, so its runs
  can't be imported. A run can be added by hand from a GPX export.
- **Cadence over a run** needs the steps saved as the run goes, which started with database
  version 18. They stay on the phone and don't sync, so older runs, and runs that reach a phone
  through sign-in, show only the average.

## Design decisions

The reasoning behind the main choices is recorded in [`docs/adr`](docs/adr):

- [0001 — Local-only storage, no backend, no authentication](docs/adr/0001-local-only-no-backend.md)
- [0002 — The training plan is hardcoded, not user-editable](docs/adr/0002-hardcoded-training-plan.md)
- [0003 — Distinct exercise types (rep-based and timed)](docs/adr/0003-two-exercise-types.md)
- [0004 — Hebrew UI with right-to-left forced at the app root](docs/adr/0004-hebrew-rtl-single-language.md)
- [0005 — Runs: moving time, pauses and GPS dropouts](docs/adr/0005-run-moving-time.md)
- [0006 — Exercises declare their metrics; a set is made of parts](docs/adr/0006-exercise-model.md)

The domain vocabulary used throughout the code is defined in [`CONTEXT.md`](CONTEXT.md).
