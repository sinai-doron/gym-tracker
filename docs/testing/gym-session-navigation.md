# Gym session navigation: test plan

Manual test plan, run on the emulator, for moving around during a gym session.
It covers starting, logging, rest, progress, going back, leaving the app and
finishing. Each step says what the screen must show and what the database
must hold. Check both: the screen can look right while the database is wrong,
and the reverse.

**Reported problems this plan covers (2026-11-01):**
- Opening an exercise's progress mid-session and coming back loses data that
  was entered.
- Back takes you through every screen opened before, instead of leaving.

## Setup

- A build of the branch under test, installed with `adb install -r` over
  existing data, so there is history for progress charts and pre-filled
  weights.
- No gym session open: home shows no resume card, and
  `select count(*) from sessions where finishedAt is null and type='GYM'`
  returns 0.
- Database checks: copy `gym_tracker.db` with its `-wal` and `-shm` files out
  through `run-as`, then query them with `sqlite3`.
- Each session's sets:
  `select e.exerciseId, s.reps, s.weightKg, s.durationSecs from set_records s join exercise_entries e on e.id = s.entryId where e.sessionId = ? order by s.id`.

## A. Start and log

| # | Steps | Screen | Database |
|---|---|---|---|
| A1 | Home → start the suggested workout | Session screen, clock running from 00:00, notification shows | New `sessions` row, `finishedAt` null |
| A2 | Open exercise 1, log a set (reps × weight) | The set appears in the sheet; rest starts with the exercise's rest time | 1 `set_records` row for exercise 1 |
| A3 | Close the sheet | Exercise 1's card shows 1 completed set | — |

## B. Progress round trips (the reported bug)

| # | Steps | Screen | Database |
|---|---|---|---|
| B1 | During rest: open exercise 1 → Progress → Back | Back on the session. Exercise 1 still shows 1 set. Rest still counting, not reset. Session clock not reset | unchanged |
| B2 | Log a 2nd set on exercise 1, close, then Progress → Back | Exercise 1 shows **2** sets, the one logged after the first round trip included | 2 rows |
| B3 | Log a set on exercise 2 → Progress for exercise 2 → Back | Exercise 2 shows its set, exercise 1 still 2 | 3 rows |
| B4 | Edit set 1 of exercise 1 (change the weight) → Progress → Back | The edited weight is shown, not the old one | row updated |
| B5 | Delete set 2 of exercise 1 → Progress → Back | The deleted set doesn't come back | row gone |
| B6 | Change exercise 1's target sets (+1) → Progress → Back | The new target is shown | `exercise_definitions.targetSets` updated |
| B7 | Progress screen for exercise 1 | Shows history (and, if it lists the current session, today's sets) | — |
| B8 | Pre-fill: open exercise 1 after a round trip | Weight field pre-filled with the last weight logged in this session | — |
| B9 | Five Progress → Back round trips in a row | Nothing lost or duplicated, no screen stacking | unchanged |

## C. Back stack

| # | Steps | Expected |
|---|---|---|
| C1 | From a session after B9, press Back once | The "finish workout?" dialog, not an older screen |
| C2 | Dialog → Continue | Stays in the session, nothing lost |
| C3 | Finish → summary → Home | Home. Press Back once: the app closes. No session, progress or summary screen in between |
| C4 | Home → Workouts → start a workout → finish → Home → Back | One Back press closes the app (Workouts isn't left underneath) |
| C5 | In a session: open the notification → app comes to front | The same session, a single activity (`dumpsys activity activities` shows one MainActivity) |
| C6 | Leave with the Home key, return from the launcher icon, repeat 3× | Same session each time; Back still behaves as C1 |

## D. Leaving and coming back

| # | Steps | Expected |
|---|---|---|
| D1 | Rotate the screen in a session with sets and rest running | Sets, rest and clock all kept |
| D2 | Home key, then kill the process (`am kill`), reopen | Home offers to resume. Resume → all logged sets shown, clock continues from the original start |
| D3 | After D2, Progress → Back | Sets still shown |

## E. Finishing

| # | Steps | Expected |
|---|---|---|
| E1 | Finish | The summary lists every set from the database, including those logged before and after progress round trips. Duration matches the clock |
| E2 | Database after finishing | `finishedAt` set; set count equals what the summary shows |
| E3 | Home after finishing | No resume card; the next workout in the rotation is suggested; the week strip marks today |
| E4 | Start a session → Back → Cancel workout | The session and its sets are deleted; home shows no resume card |

## Results: 2026-11-01, emulator (API 37), branch fix/session-navigation

**Before the fix**, on main at 3f2fa21:
- **B1 failed.** Back from progress, the exercise showed 0 sets while the database had 1.
- **B2 failed.** After the round trip, a second set showed as "set 1" alone; the database had 2.
  The summary is built from the same state, so it would have undercounted.
- **C6 failed.** When the task was first opened by a non-launcher intent (for example the
  notification after the app was killed), the launcher icon stacked a second MainActivity.
  Each copy had its own session screen with different sets and rest. Back walked through
  every copy.
- **B8 failed.** A resumed session didn't pre-fill the weight from its own last set.
- Small: the finish dialog closed on rotation.

**Causes and fixes:**
- **Lost sets.** Exercise sheets were held in a `remember` map in `SessionScreen`. That map
  went away when progress replaced the screen, and was rebuilt from the sets loaded when the
  session opened. `SessionViewModel` now owns the sheets (`detailFor`), and they run in its
  scope. The open sheet is kept in the state too, so coming back from progress reopens it.
  Half-typed reps, weight and distance use `rememberSaveable`.
- **Stacked activities.** `MainActivity` now has `launchMode="singleTask"`.
- **Pre-fill.** `ExerciseDetailViewModel` starts from the last logged set, and past
  sessions only fill in when there is none.

**After the fix, all pass:** A1–A3, B1–B9, C1–C6, D1–D3 and E1–E4, plus a timed set (plank)
that keeps running through a progress round trip and logs its full duration. Each step was
checked against the database.
