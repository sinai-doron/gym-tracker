# Database migrations: tests

Every database version the app has shipped must open in the current app with its history intact.
These instrumented tests check that before a new migration ships. They matter most for the v14
migration ([ADR-0006](../adr/0006-exercise-model.md)), which rewrites every set.

| Test | What it checks | Needs |
|---|---|---|
| `MigrationFrom10Test` | A version 10 database with a small sample history migrates to the current version: same history, every synced row has a uuid and one change-log entry, the sync triggers match a fresh install, and the app's own writes work afterwards | An emulator |
| `BackupMigrationTest` | A real backup zip migrates to the current version with the same history, uuids and triggers | An emulator and a backup zip; skipped without one |
| `SyncTriggersThroughRoomTest` | The change-log triggers on a fresh install, written through Room's DAOs | An emulator |

`HistoryFingerprint` is what "the same history" means: session and set counts, total reps and
volume, the best set per exercise, run distances, GPS points, pauses, routes, songs, and the plan.

## Run on an emulator, never on your phone

`connectedDebugAndroidTest` uninstalls the app when it finishes. On a phone that deletes all
your workouts. Use an emulator (Android Studio → Device Manager), and check `adb devices -l`
lists only the emulator before running.

## Schema files

Room writes each version's schema to `app/schemas/` when the app builds. Commit them: the next
migration's tests check against them. After the first build on this branch:

```sh
./gradlew assembleDebug
git add app/schemas
```

Versions 1–12 were never exported. `MigrationFrom10Test` writes version 10 out by hand instead.

## The sample-data tests

```sh
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.doron.gymtracker.data.db.MigrationFrom10Test
```

## With a real backup

1. On the phone: **Settings → Backup**, and save the zip somewhere you can reach from the
   computer.
2. Install the app on the emulator once, so its files folder exists: `./gradlew installDebug`.
3. Copy the backup there:
   ```sh
   adb push gym-backup.zip /sdcard/Android/data/dev.neonsheep.gymtracker/files/gym-backup.zip
   ```
4. Run the test:
   ```sh
   ./gradlew connectedDebugAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=com.doron.gymtracker.data.db.BackupMigrationTest
   ```
5. The numbers it compared are in logcat under the tag `BackupMigrationTest`; the report is in
   `app/build/reports/androidTests/connected/`.

The test run uninstalls the app, which deletes the copied zip too: push it again before each run.

The backup is your training history. Keep it out of the repository (`.gitignore` covers
`gym-backup*.zip` and the app's own `gym-tracker-backup-*.zip` names).

Step 3 can push the app's file as it is, renamed on the way:
`adb push gym-tracker-backup-2026-10-31.zip /sdcard/Android/data/dev.neonsheep.gymtracker/files/gym-backup.zip`.
