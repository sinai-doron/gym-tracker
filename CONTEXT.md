# Gym Tracker — Domain Context

## Glossary

**TrainingPlan**
The full set of three workouts (A, B, C) that make up the user's programme. Hardcoded in the app. There is exactly one TrainingPlan.

**Workout**
One of the three labelled variants of the TrainingPlan: Workout A, Workout B, or Workout C. Contains an ordered list of ExerciseDefinitions. Order within a Workout is a default, not a constraint — the user is free to perform exercises in any order during a Session.

**ExerciseDefinition**
A template describing one exercise as it appears in a Workout: its name, type (rep-based or timed), target number of sets, target reps (rep-based only), and rest duration between sets.

**RepExercise**
An ExerciseDefinition whose performance is measured in repetitions and load (weight). Each completed set records reps and weight.

**TimedExercise**
An ExerciseDefinition whose performance is measured in elapsed time (e.g. Plank). Each completed set records duration via a start/stop timer. No weight is recorded.

**Session**
A single instance of the user performing a Workout on a specific date. A Session has a start time, an optional end time, and a collection of ExerciseEntries. The user explicitly starts and finishes a Session; the app never auto-closes it.

**ExerciseEntry**
The record of one ExerciseDefinition attempted within a Session. Contains one or more SetRecords. An exercise may be skipped (no ExerciseEntry created) or abandoned mid-set.

**SetRecord**
One completed set within an ExerciseEntry. For a RepExercise: reps performed + weight lifted. For a TimedExercise: duration held. Weight and duration are logged *after* the set is completed.

**RestPeriod**
A countdown timer that starts automatically after a SetRecord is saved. Duration is defined on the ExerciseDefinition, not globally. The user may dismiss it early.

**RotationCursor**
The internal record of which Workout was last completed, used to derive the next suggested Workout (A→B→C→A). The user may override the suggestion at Session start.

**SessionHistory**
The persistent log of all completed Sessions. Surfaced in the app as two views: Last Session (most recent weights per exercise) and Trends (a time-series of weights/durations per exercise across all sessions).
