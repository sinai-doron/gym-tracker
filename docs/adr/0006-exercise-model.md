# ADR-0006: Exercises declare their metrics; a set is made of parts

## Status
Accepted (2026-11-02). Supersedes [ADR-0003](0003-two-exercise-types.md) once the sets migration lands
(amended 2026-11-02: the migration runs in steps, see the end).

## Context
To open the app to other people, it has to hold their exercises, not just the owner's 23.
Today an exercise is one of three Kotlin classes (`RepExerciseDefinition`,
`TimedExerciseDefinition`, `DistanceExerciseDefinition`), and a set stores `reps`,
`weightKg`, `durationSecs` or `distanceM`.

We reviewed 40 exercises across free weights, machines, bodyweight, calisthenics,
plyometrics, carries, cardio machines, mobility, swimming, climbing and combat sports. A
fixed list of nine tracking types (reps × weight, reps only, assisted, added weight, time,
time + weight, distance + weight, distance + time, calories + time) failed for 14 of them:

- **What the load means:** dumbbells per hand, cable stacks in plates rather than kg, bands.
- **Sides that differ:** a Bulgarian split squat with 10 reps left and 9 right.
- **One set, several segments:** drop sets, rest-pause, Olympic complexes.
- **Progression steps:** tuck → straddle → full front lever.
- **More numbers:** box height, treadmill speed and incline, machine level.
- **Categories and outcomes:** swim stroke, climbing grade, attempts, sent.

Every one of these could become another tracking type, but the list would keep growing
with each new kind of training. The review also found two problems in today's data: the
farmer's walk never saved its weight, and bodyweight sets are saved as 0 kg.

The full review is in the "Exercise model review" tab of the planning doc.

## Decision

### Metrics
A fixed catalogue of metrics, stored in metric units, shown in the Settings unit:

| Metric | Value | Stored as |
|---|---|---|
| reps | whole number | count |
| load | decimal | kg, or plate number (see load unit) |
| duration | whole number | seconds |
| distance | whole number | metres |
| calories | whole number | kcal |
| height | whole number | cm |
| speed | decimal | km/h |
| incline | decimal | % |
| level | whole number | machine level |
| band | category | light, medium, heavy, extra heavy |

Swim stroke, climbing grade, attempts and sent are added with swimming and climbing as
training types.

### What an exercise declares
- **Metrics:** which ones a set records, each required or optional.
- **Record metric and direction:** what counts as better (estimated 1RM for reps × load,
  longest hold, least assistance…).
- **Load convention:** total, per hand, added to bodyweight, assistance, or none.
- **Load unit:** kg, lb or stack plates, per exercise, because machines differ between gyms.
  Plates can carry an optional kg per plate so volume and records add up; without it,
  records compare plate numbers only. The default follows the Settings unit.
- **Sides:** both together, or per side.
- **Progression chain (optional):** the previous and next step. Each step is its own exercise
  with its own records.
- **Preset:** the name the plan builder shows ("Reps × weight", "Time", "Treadmill"…), which
  fills in the above.

### Sets and parts
```
sets       id, entry, order, kind (warm-up, working, failure, AMRAP), rpe, note, completedAt
set_parts  set, seq, side (left, right or none),
           reps, loadKg, durationS, distanceM, calories,
           extra (JSON: height, speed, incline, level, band, …)
```
A normal set has one part. A per-side set has one part by default ("each side"), and two
when the user splits it. A drop set or rest-pause set has one part per segment. The five
common metrics are columns because charts and records query them; rare ones live in
`extra`.

### Records and progress
Each preset names its record and its chart line. For reps × load it is estimated 1RM
(Epley, `load × (1 + reps / 30)`, only from sets of 12 reps or fewer), with heaviest weight
as an option on the chart. Volume and records use the effective load: per hand × 2, body
weight + added load, body weight − assistance. Body weight comes from the weigh-in nearest
the session, falling back to Settings.

### Scope of the first release
- Metrics: reps, load, duration, distance, calories, speed, incline, level, band, height.
- The schema holds drop sets, rest-pause and progression chains from the sets step (v15). Their logging
  screens come later (progression chains with calisthenics).

### Migrating the owner's history
- Each `set_records` row becomes one set with one part. Per-side history stays one part with
  no side.
- Dumbbell exercises migrate as **per hand**: the owner logs 20 kg for 20 kg in each hand.
- Cable machine history migrates as **kg**: the owner's gym marks its stacks in kg.
- Bodyweight sets saved with 0 kg get no load (null).
- Farmer's walk sets keep their distance; their weight was never saved and stays empty.
- Week-B variants become exercises of their own; the A/B alternation becomes the
  programme's variant rule.

## Consequences
- **Good:** a new exercise, or a new kind of training, is data: a preset or a metric list, not
  a new Kotlin class and a release.
- **Good:** one logging row, one chart and one records path for every exercise, driven by
  what the exercise declares.
- **Good:** estimated 1RM compares 10 × 60 kg with 5 × 70 kg on one line.
- **Bad:** the compiler no longer guarantees that a rep exercise has reps; validation moves
  to the exercise's metric list and needs its own tests.
- **Bad:** `extra` JSON can't be indexed or queried cheaply; any metric that needs charts
  or records must be promoted to a column.
- **Bad:** the sets migration (v15) rewrites every set in the owner's history. It ships only after
  migration tests pass against a copy of a real backup.

## Amendment (2026-11-02): the migration runs in three steps

One v13 → v14 migration would have changed every table and every synced item at once, and the
backend accepts only the item shapes it knows (`gym.entities.ts`). It runs in three steps instead,
each a database version of its own, each tested against a real backup, each leaving the app
working:

| Version | Step | Synced shapes |
|---|---|---|
| v14 | The exercise library: `exercises`, `exercise_names`, `exercise_muscles`, seeded from the catalogue at start; each plan exercise linked to its built-in (`exercise_definitions.libraryExerciseId`) | Unchanged |
| v15 | Sets and parts, with the history rules above (per hand, bodyweight with no load) | `set` takes a new shape with its parts; the backend takes both shapes first (doron-backend Task 31) |
| v16 | Programmes and workouts: week-B rows become variants, the rotation leaves prefs | New kinds; the backend learns them first |

A built-in's id is derived from its slug (`ExerciseCatalogue.builtinId`), so it is the same on
every phone and history can name it without uploading it. The v14 migration links the plan by a
map written into the migration, not read from the catalogue, so a later catalogue can't change
what it did.
