# Gym Tracker — Domain Context

## Glossary

**TrainingPlan**
The full set of three workouts (A, B, C) that make up the user's programme. Seeded on first launch and editable in the plan editor. There is exactly one TrainingPlan.

**Workout**
One of the three labelled variants of the TrainingPlan: Workout A, Workout B, or Workout C. Contains an ordered list of ExerciseDefinitions. Order within a Workout is a default, not a constraint — the user is free to perform exercises in any order during a Session.

**ExerciseDefinition**
A template describing one exercise as it appears in a Workout: its name, type (rep-based, timed or distance), target number of sets, target reps or distance, and rest duration between sets.

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
The persistent log of all completed Sessions, gym and cardio. In the app it is the monthly History calendar, where gym days are purple and cardio days green. Per exercise, it also feeds Exercise Progress: the personal record, the best set per workout, and every session's sets.

**DistanceExercise**
An ExerciseDefinition measured in distance (e.g. Farmer's Walk).

**DeloadWeek**
Every sixth week since the programme start. Sessions suggest 60% of the last weight for 2 sets.

**ExerciseVariant**
An exercise that alternates between two versions on alternate (A/B) weeks, counted from the programme start date.

---

## Cardio

**CardioSession** (run or walk)
A Session whose type is RUNNING or WALKING, recorded from GPS by a foreground service. Its RunDetails hold distance, steps, calories, target and lap length; its route is a list of LocationPoints.

**Move**
A GPS fix that added distance (a `DistanceMark`: when, and the distance by then). Drift while standing still isn't a move.

**MovingTime**
Time from start to finish without pauses, as computed by `ActiveClock`. Pace, laps and records all use it. See ADR-0005.

**AutoPause**
A pause that starts 10 s after the last move and ends with the next one. A gap that covered ground at ≥ 1 m/s is a GPS dropout, not a pause.

**ManualPause**
A pause the runner asks for, from the screen or the notification. It is stored in `run_pauses`. Fixes during it are ignored.

**Lap**
A stretch of the lap length set in Settings, with its moving time and pace. The final Lap may be partial (≥ 10 m).

**RunTarget**
What a run aims at: none, a distance, a duration, or a Route to follow.

**Route**
A saved path, from a run or imported from GPX, with a name. It can be followed as a RunTarget, and the runner is warned when off it.

**Badge**
Earned by the first run that reaches 1K, 5K or 10K.

**PersonalRecord**
The best so far: fastest 1K, 5K and 10K (the fastest stretch of that distance inside any run), longest run, and best week.

**StepGoal**
The daily step target, 10,000 by default. The home steps card measures each day against it.
