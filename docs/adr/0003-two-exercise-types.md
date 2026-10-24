# ADR-0003: Two distinct exercise types — RepExercise and TimedExercise

## Status
Accepted

## Context
Most exercises are measured in repetitions and load (weight). At least one exercise (Plank) is measured in elapsed time with no weight component. Options considered:
- **Sealed class hierarchy** — `RepExercise` and `TimedExercise` as distinct subtypes of `ExerciseDefinition`
- **Single type with nullable fields** — one `ExerciseDefinition` with optional `targetReps` and optional `restDurationSecs`; type inferred at runtime
- **Behaviour flags** — a single type with an `isTimed: Boolean` flag

## Decision
Use a sealed class hierarchy: `ExerciseDefinition` is a sealed interface with two implementations — `RepExerciseDefinition` and `TimedExerciseDefinition`. SetRecords are similarly typed: `RepSetRecord` (reps + weight) and `TimedSetRecord` (duration). Room stores both via a type discriminator column.

## Consequences
- **Good:** the compiler enforces that rep-based exercises always have reps/weight and timed exercises always have duration — no null-checks in UI or business logic
- **Good:** adding a third type later (e.g. distance-based) is additive, not a rewrite
- **Bad:** slightly more boilerplate in Room entity mapping and in `when` expressions in the UI
- **Bad:** DB schema has a discriminator column and some nullable columns per type; acceptable at this scale
