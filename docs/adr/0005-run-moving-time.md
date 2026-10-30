# ADR-0005: Runs: moving time, pauses and GPS dropouts

## Status
Accepted

## Context
A run's time, pace, laps and records all depend on how long the runner was *moving*,
not on the time from start to finish. The phone's GPS is noisy:

- Standing at a light, fixes drift a few metres.
- Under bridges or between tall buildings, fixes stop for tens of seconds while the
  runner keeps going.

Options considered:
- **Elapsed time:** simple, but it counts every stop, so pace and records come out wrong.
- **Speed threshold per fix:** pause whenever the current speed is low. GPS drift makes
  the clock flicker.
- **Moves plus a timeout:** pause when no fix has added distance for a while.

## Decision
`ActiveClock` derives moving time from *moves*: the fixes that counted towards the
distance in `RouteDistance.marks`.

- **Auto-pause.** The run pauses once there has been no move for 10 s, and resumes with
  the next move. The 10 s before a pause kicks in still count.
- **GPS dropouts.** A gap between moves that covered ground at 1 m/s or more was lost
  GPS, not a stop, so it counts as moving.
- **Manual pauses.** These are stored as `Pause` rows (`run_pauses`) and merged with the
  automatic ones. Fixes inside a manual pause are ignored, and distance doesn't bridge
  across one.

Laps, the pace chart, best efforts and personal records all use the same clock. As a
result, the live screen, the report and history always agree.

## Consequences
- **Good:** time and pace match what a watch shows. A real run came to 41:54 here, and
  Samsung Health showed 41:49.
- **Good:** a single definition of moving time, which unit tests cover.
- **Bad:** a long walk break that still covers 1 m/s counts as moving.
- **Bad:** while GPS is lost, the live screen can show "paused" until the fixes return.
  The time is corrected afterwards.
- Personal records ignore any stretch faster than 2:30 /km, as that is GPS error.
