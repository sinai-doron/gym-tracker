# Phase 2 — Real Program Data & Advanced Features

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans.

**Goal:** Implement the user's full 3-day strength programme with per-exercise A/B week variants, deload logic, progression hints, distance exercise type, and Week A/B rotation based on start date.

**Architecture:** Extend existing domain models with new sealed types and flags. Add a `ProgramScheduler` domain service. All new logic is pure functions (no network, no new dependencies).

**Program start date:** 2026-10-26

**Spec:** `docs/adr/0002-hardcoded-training-plan.md`, user-provided JSON (2026-10-24 conversation)

## Global Constraints
- All user-facing text in Hebrew
- RTL enforced via GymTrackerTheme (ADR-0004)
- No new external dependencies
- Hardcoded programme data; no edit UI
- `minSdk = 26`, `compileSdk = 35`

---

### Task 10: Per-exercise A/B variant rotation by week number

**Files:**
- Modify: `domain/model/ExerciseDefinition.kt` — add `variantB: ExerciseDefinition?` to `RepExerciseDefinition` and `TimedExerciseDefinition`
- Create: `domain/usecase/ResolveExerciseVariantUseCase.kt`
- Create: `data/prefs/ProgramPrefs.kt` — stores programme start date
- Modify: `data/seed/TrainingPlanData.kt` — add `variantB` to all A/B exercises
- Test: `domain/usecase/ResolveExerciseVariantUseCaseTest.kt`

**Interfaces:**
```kotlin
// ExerciseDefinition gains:
val variantB: ExerciseDefinition?  // null = fixed (same every week)

// ResolveExerciseVariantUseCase
class ResolveExerciseVariantUseCase(prefs: ProgramPrefs) {
    operator fun invoke(exercise: ExerciseDefinition, sessionDate: Long = System.currentTimeMillis()): ExerciseDefinition
    // week number = (sessionDate - startDate) / 7 days; odd week → exercise; even week → variantB ?? exercise
}
```

- [ ] Write failing test — week 1 (odd) returns A, week 2 (even) returns B, no variantB always returns fixed
- [ ] Run test → FAIL
- [ ] Implement `ResolveExerciseVariantUseCase`
- [ ] Add `variantB` fields to domain models
- [ ] Update `TrainingPlanData` with all Week B variants from programme JSON
- [ ] Wire use case into `SessionViewModel` — resolve each exercise variant before displaying
- [ ] Run test → PASS
- [ ] Commit: `feat: per-exercise A/B week rotation`

---

### Task 11: Distance exercise type (הליכת איכר)

**Files:**
- Modify: `domain/model/ExerciseDefinition.kt` — add `DistanceExerciseDefinition(targetDistanceM: IntRange)`
- Modify: `domain/model/SetRecord.kt` — add `DistanceSetRecord(distanceM: Int)`
- Modify: `data/db/entities/SetRecordEntity.kt` — add `distanceM INT nullable`; DB migration version → 2
- Modify: `data/seed/TrainingPlanData.kt` — change הליכת איכר from `TimedExerciseDefinition` to `DistanceExerciseDefinition`
- Modify: `ui/screens/session/ExerciseDetailSheet.kt` — add `DistanceSetInput` branch
- Modify: `ui/screens/session/ExerciseDetailViewModel.kt` — add `logDistanceSet(distanceM: Int)`
- Modify: `ui/components/SetLogRow.kt` — render `DistanceSetRecord` as "X מ'"
- Test: `domain/model/DomainModelTest.kt` — add `DistanceSetRecord when is exhaustive`

- [ ] Write failing test
- [ ] Run test → FAIL
- [ ] Implement domain model additions
- [ ] Add Room migration (version 1 → 2)
- [ ] Update sheet, ViewModel, SetLogRow
- [ ] Run test → PASS
- [ ] Commit: `feat: distance exercise type for הליכת איכר`

---

### Task 12: Deload week (every 6th week)

**Files:**
- Create: `domain/usecase/IsDeloadWeekUseCase.kt`
- Modify: `ui/screens/session/SessionScreen.kt` — show deload banner when active
- Modify: `ui/screens/session/ExerciseDetailSheet.kt` — pre-fill weight at 60% of last logged, cap sets at 2
- Test: `domain/usecase/IsDeloadWeekUseCaseTest.kt`

**Interfaces:**
```kotlin
class IsDeloadWeekUseCase(prefs: ProgramPrefs) {
    operator fun invoke(sessionDate: Long = System.currentTimeMillis()): Boolean
    // week number from start date; isDeload = weekNumber % 6 == 0
}
```

- [ ] Write failing test — week 6 is deload, week 7 is not, week 12 is deload
- [ ] Run test → FAIL
- [ ] Implement use case
- [ ] Show "שבוע דילוד" banner in SessionScreen when active
- [ ] ExerciseDetailSheet: if deload, pre-fill weight field with 60% of last session's weight for this exercise (query from SessionRepository), cap `targetSets` display at 2
- [ ] Run test → PASS
- [ ] Commit: `feat: deload week detection and UI`

---

### Task 13: Progression hints

**Files:**
- Create: `domain/usecase/ProgressionHintUseCase.kt`
- Create: `domain/model/ProgressionHint.kt`
- Modify: `ui/screens/session/ExerciseDetailSheet.kt` — show hint below last set row

**Interfaces:**
```kotlin
sealed interface ProgressionHint {
    data class Increase(val addKg: Float) : ProgressionHint  // all sets hit top of range 3× in a row
    object Stalled : ProgressionHint                          // no progress in 3 sessions
    object OnTrack : ProgressionHint                          // default
}
class ProgressionHintUseCase(sessionRepo: SessionRepository) {
    suspend operator fun invoke(exerciseId: String): ProgressionHint
}
```

- Increase rule: last 3 sessions for this exercise all had every set at `>= targetReps` (top of range)
- Stalled rule: last 3 sessions all at the same weight with no rep improvement
- Hint copy (Hebrew): Increase → "הגדל משקל ב-X ק״ג", Stalled → "הפחת 10% ונבנה מחדש"

- [ ] Write failing tests for Increase, Stalled, OnTrack
- [ ] Run tests → FAIL
- [ ] Implement `ProgressionHintUseCase`
- [ ] Show hint in ExerciseDetailSheet above the input area
- [ ] Run tests → PASS
- [ ] Commit: `feat: progression hints`

---

### Task 14: Optional exercises + per-side display

**Files:**
- Modify: `domain/model/ExerciseDefinition.kt` — add `isOptional: Boolean`, `isPerSide: Boolean`
- Modify: `data/seed/TrainingPlanData.kt` — set flags on קירוב ירכיים, פשיטה מאחורי הראש, סקוואט בולגרי, לאנג'ים
- Modify: `ui/components/ExerciseCard.kt` — dim optional exercises, show "צד אחד" label for per-side
- Modify: `ui/screens/session/ExerciseDetailSheet.kt` — show "לכל צד" note when `isPerSide`

- [ ] Update domain models + seed data
- [ ] Update ExerciseCard + ExerciseDetailSheet
- [ ] Build verifies + no test regressions
- [ ] Commit: `feat: optional and per-side exercise flags`
