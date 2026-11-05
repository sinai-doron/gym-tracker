# Neonsheep Fitness — Domain Context

The app was first called Gym Tracker, and the code still is (`com.doron.gymtracker`). Each
install belongs to one person; many people use the app, each on their own phone.

## Glossary

**Programme** (`Program`)
What the user follows: a name, a start date from which its weeks count, and its Workouts. A user can keep several; exactly one is active, and the rest can be archived. A fresh install has none: the user starts one from a ProgramTemplate or from an empty programme. A programme with history is archived, never deleted. Its deload and variant rules (`deloadEveryWeeks`, `deloadLoadPercent`, `deloadSets`, `variantRule`, `variantEveryWeeks`) are set by the user from the programme's rules line.

**ProgramTemplate**
A ready-made Programme the user can start from: Full body, Upper / Lower, Push / Pull / Legs, A / B / C. Kept as JSON in `assets/templates/`, naming LibraryExercises by slug, with its names in English and Hebrew.

**TrainingPlan**
The owner's original A/B/C plan, from before Programmes: `TrainingPlanData` in code. Database version 16 turned it into a Programme on the owner's phone; it means nothing on any other install.

**Workout**
One of a Programme's workouts, with a name, a short code for its chip (A, B, Push…), its place in the Rotation and, optionally, a day of the week. Contains an ordered list of ExerciseDefinitions. Order within a Workout is a default, not a constraint — the user is free to perform exercises in any order during a Session. A workout with history is hidden, never deleted.

**Rotation**
How the next Workout is chosen, read from history (`GetNextWorkoutUseCase`), never stored:
- **In order** (no Workout has a day): the one after the last finished, the first when none has been.
- **By weekday** (any Workout has a day): today's, unless already done today, else the next day that has one. A missed day isn't made up, and a Workout without a day is never suggested.
The user may pick another Workout at Session start.

**LibraryExercise**
An exercise in the exercise library: a built-in one from the catalogue (`catalogue/`, built into `assets/exercises/`, names in English and Hebrew), or a CustomExercise. It declares the metrics a set records, its muscles and equipment (ADR-0006).

**CustomExercise**
A LibraryExercise the user created because the library lacked it. It keeps the name they typed.

**ExerciseDefinition**
One exercise as it appears in a Workout: the LibraryExercise it is, its targets (sets, reps, time or distance) and rest duration between sets.

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

**SessionHistory**
The persistent log of all completed Sessions, gym and cardio. In the app it is the monthly History calendar, where gym days are purple and cardio days green. Per exercise, it also feeds Exercise Progress: the personal record, the best set per workout, and every session's sets.

**DistanceExercise**
An ExerciseDefinition measured in distance (e.g. Farmer's Walk).

**DeloadWeek**
Every Nth week since the Programme's start, when the Programme has deloads (the owner's: every sixth), never before week 1. The Programme sets the load (a share of the usual weight) and the sets per exercise. In the session, each exercise's sets are capped and the weight pre-fills at that share, rounded down to a plate; the plan keeps its own sets, and later weeks' last weight and ProgressionHint pass over deload sessions.

**ExerciseVariant**
An exercise that alternates between two versions in A/B weeks, counted from the Programme's start date, when the Programme alternates: the exercise for N weeks, then its variant for N (N from 1 to 4, a Programme setting).

**TrainingType**
What a Session is: Strength, Run and Walk are built in (the built-ins can be hidden), and the user can add their own (Pilates, football…). Each names the screen that records it: the gym session, the GPS run screen, or a timer.

**LoggedSession**
A Session added afterwards rather than recorded live (`loggedManually`). It counts for streaks and totals, not for speed or distance records.

**Units**
Kilometres or miles, kilograms or pounds: the user's choice in About you, preset from the phone's region. Storage is always metres and kilograms; units apply only to what is shown and typed.

**Achievement**
The distance Badges, plus bronze, silver and gold in four families (climb in one run or walk, distance in all, tonnes lifted, weeks in a row) and three one-offs.

---

## Account and sync

**Account**
A Google sign-in, optional, and switched off for the public release (`SignInFeature`). Without it the app works fully and no workout data leaves the phone.

**DataOwner**
The account whose data this phone holds: the first to sign in. Another account is refused.

**SyncConsent**
What the signed-in user agreed to on the consent screen, with the version of its text. Nothing syncs without it; run routes and the music log are separate choices.

**Sync**
Changes logged by database triggers in `sync_changes` and sent to the owner's server, then pulled back on other phones. Health Connect data (daily steps, heart rate) and a run's steps over time never sync.

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
