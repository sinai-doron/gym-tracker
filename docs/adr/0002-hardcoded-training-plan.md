# ADR-0002: Training plan data is hardcoded, not user-editable

## Status
Superseded in part (2026-10)

> **Update (2026-10).** The plan is still seeded from `TrainingPlanData` on first launch,
> but it is now editable in the app: Workouts → Edit changes an exercise's name, sets,
> target and rest, and adds, removes and reorders exercises. Removing an exercise is soft,
> so the sessions that used it keep their history.

## Context
The app needs a source of truth for the three Workouts (A, B, C) and their ExerciseDefinitions. Options considered:
- **Hardcoded Kotlin data objects** — plan defined in source, seeded into Room at first launch
- **User-editable through the UI** — full CRUD for exercises and sets
- **Remote config / CMS** — plan fetched from a server or config file

## Decision
The training plan is hardcoded as Kotlin data objects and seeded into the Room database on first launch. The user does not edit the plan through the UI. The plan owner (the developer) updates it by changing source code and releasing a new build.

## Consequences
- **Good:** no plan-editing UI needed, significantly reduces scope
- **Good:** data model is simpler — no "is this user-modified or original?" complexity
- **Good:** plan correctness is enforced at compile time
- **Bad:** changing the plan requires a code change and a new install
- **Bad:** on-device data migration is needed if the plan structure changes after sessions have been recorded
- If the user later wants to edit their own plan, this decision must be revisited and a migration path defined
