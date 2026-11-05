# ADR-0002: Training plan data is hardcoded, not user-editable

## Status
Superseded in part (2026-10); the start date, deloads and rotation became a programme row in v16 (ADR-0006).
Superseded (2026-11): users build their own programmes.

> **Update (2026-10).** The plan is still seeded from `TrainingPlanData` on first launch,
> but it is now editable in the app: Workouts → Edit changes an exercise's name, sets,
> target and rest, and adds, removes and reorders exercises. Removing an exercise is soft,
> so the sessions that used it keep their history.

> **Superseded (2026-11).** With the app public, there is no single plan to hardcode.
>
> - **No plan on a fresh install.** "Your training", the last step of the welcome screens,
>   starts a programme from a template (Full body, Upper / Lower, Push / Pull / Legs, A / B / C;
>   JSON in `assets/templates/`, checked against the exercise catalogue by
>   `ProgramTemplateTest`) or from an empty programme. It can be skipped and done later.
> - **Programmes are user data.** A user can keep several, one active, and archive the rest.
>   The builder adds, renames, reorders and removes workouts, gives them a day of the week, and
>   sets when week 1 starts; the plan editor changes each workout's exercises, picked from the
>   exercise library or created by the user.
> - **Rotation, in order or by weekday.** The next workout is read from history: the one after
>   the last finished, or, once any workout has a day, today's or the next day's
>   (`GetNextWorkoutUseCase`).
> - **`TrainingPlanData` stays only for the owner's plan.** It tops up the A/B/C plan that
>   database version 16 made from the owner's history, on installs that have it, and nothing
>   else. That plan is still the only one with deload weeks and A/B variants; programmes made
>   since have neither until those become settings.

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
