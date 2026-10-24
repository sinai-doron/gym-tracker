# Gym Tracker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single-user native Android gym training app with A/B/C workout schedules, active session tracking, set logging with rest timers, and session history.

**Architecture:** MVVM — Compose UI layer over ViewModels, repository pattern over Room, Hilt for DI. Domain layer uses sealed classes for the two exercise types (rep-based, timed). Business logic lives in use cases. No network layer.

**Tech Stack:** Kotlin, Jetpack Compose, Room 2.6.x, Hilt 2.51.x, Navigation Compose, ViewModel + StateFlow, Coroutines, DataStore Preferences

**Spec:** `CONTEXT.md`, `docs/adr/0001-local-only-no-backend.md`, `docs/adr/0002-hardcoded-training-plan.md`, `docs/adr/0003-two-exercise-types.md`

## Global Constraints

- `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`
- Package: `com.doron.gymtracker`
- No network calls, no authentication, no external services
- All data stored locally via Room; no cloud sync
- Training plan is hardcoded Kotlin objects seeded into Room at first launch; no edit UI
- Exercise order within a session is never enforced; any exercise may be skipped
- Rest timer duration defined per `ExerciseDefinition`, not globally
- Weight/duration logged **after** set completion, not before
- Sessions must be explicitly finished by the user; no auto-close

## Review Focus

1. **Rotation cursor off-by-one**: after Workout C, next suggested must be A — add test for C→A in Task 5
2. **Timed exercise has no weight prompt**: `ExerciseDetailSheet` must branch exhaustively on `RepExerciseDefinition` / `TimedExerciseDefinition` — test in Task 8
3. **Rest timer early dismiss**: dismissing rest timer must not skip the next set or corrupt state — test in Task 8
4. **Session clock survives backgrounding**: ticker coroutine in `viewModelScope` ticks while app is foregrounded; add a note in Task 7 — process-death recovery is out of scope
5. **History empty state**: Last Session and Trends tabs must render gracefully with no completed sessions — test in Task 9

---

## File Map

```
app/src/main/java/com/doron/gymtracker/
  GymTrackerApp.kt                              — @HiltAndroidApp Application
  MainActivity.kt                               — @AndroidEntryPoint, single Activity
  domain/model/
    ExerciseDefinition.kt                       — sealed interface + RepExerciseDefinition + TimedExerciseDefinition
    Workout.kt                                  — Workout data class + WorkoutLabel enum
    Session.kt                                  — Session data class
    ExerciseEntry.kt                            — ExerciseEntry data class
    SetRecord.kt                                — sealed interface + RepSetRecord + TimedSetRecord
  domain/usecase/
    GetNextWorkoutUseCase.kt                    — A→B→C→A rotation
    StartSessionUseCase.kt
    LogSetUseCase.kt
    FinishSessionUseCase.kt
  data/db/
    GymDatabase.kt                              — Room @Database
    entities/
      WorkoutEntity.kt
      ExerciseDefinitionEntity.kt               — type discriminator column "REP"|"TIMED"
      SessionEntity.kt
      ExerciseEntryEntity.kt
      SetRecordEntity.kt                        — type discriminator column "REP"|"TIMED"
    dao/
      WorkoutDao.kt
      SessionDao.kt
      ExerciseEntryDao.kt
      SetRecordDao.kt
  data/repository/
    WorkoutRepository.kt                        — interface + Impl
    SessionRepository.kt                        — interface + Impl
  data/seed/
    TrainingPlanData.kt                         — hardcoded Workout objects (placeholder until user provides plan)
  data/prefs/
    RotationPrefs.kt                            — DataStore: last completed WorkoutLabel
  data/di/
    DatabaseModule.kt                           — Hilt: provides GymDatabase + DAOs
    RepositoryModule.kt                         — Hilt: binds Repository interfaces
    UseCaseModule.kt                            — Hilt: provides use cases
  ui/
    navigation/NavGraph.kt                      — NavHost: home, session/{sessionId}, history
    theme/Theme.kt, Color.kt, Type.kt           — Material3 dark-first theme
    screens/
      home/HomeViewModel.kt
      home/HomeScreen.kt                        — suggested workout + A/B/C picker + Start button
      session/SessionViewModel.kt
      session/SessionScreen.kt                  — running clock + exercise list + Finish button
      session/ExerciseDetailSheet.kt            — ModalBottomSheet for logging sets
      session/ExerciseDetailViewModel.kt
      history/HistoryViewModel.kt
      history/HistoryScreen.kt                  — TabRow: Last Session | Trends
    components/
      SessionClockDisplay.kt                    — formats elapsedSecs as MM:SS
      ExerciseCard.kt                           — card in session list (name, sets done/total)
      RestTimerOverlay.kt                       — dismissible countdown after set
      SetLogRow.kt                              — completed set row (set #, weight/duration)
```

---

### Task 1: Project scaffold

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `GymTrackerApp.kt`
- Create: `MainActivity.kt`
- Create: `ui/theme/Theme.kt`, `Color.kt`, `Type.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `GymTrackerTheme {}` composable; `@HiltAndroidApp` Application; single-Activity with `setContent {}`

- [ ] **Step 1: Create Android project** — package `com.doron.gymtracker`, empty Compose Activity template

- [ ] **Step 2: Add dependencies to `app/build.gradle.kts`**
  - Compose BOM (latest stable) — `compose.material3`, `compose.ui`, `compose.ui.tooling.preview`
  - `hilt-android`, `hilt-compiler`, `hilt-navigation-compose`
  - `room-runtime`, `room-ktx`, `room-compiler`
  - `navigation-compose`
  - `lifecycle-viewmodel-compose`, `lifecycle-runtime-ktx`
  - `kotlinx-coroutines-android`
  - `datastore-preferences`

- [ ] **Step 3: Implement `GymTrackerApp.kt`** — `@HiltAndroidApp class GymTrackerApp : Application()`

- [ ] **Step 4: Implement `MainActivity.kt`** — `@AndroidEntryPoint`; `setContent { GymTrackerTheme { NavGraph() } }`

- [ ] **Step 5: Implement `Theme.kt`, `Color.kt`, `Type.kt`** — Material3 dark-first theme; dark background `#121212`, accent `#B5FF4D` (lime), surface `#1E1E1E`

- [ ] **Step 6: Verify build** — `./gradlew assembleDebug` with no errors

- [ ] **Step 7: Commit**
  ```bash
  git add -A
  git commit -m "feat: scaffold Android project with Compose, Hilt, Room, Nav"
  ```

---

### Task 2: Domain models

**Files:**
- Create: `domain/model/ExerciseDefinition.kt`
- Create: `domain/model/Workout.kt`
- Create: `domain/model/Session.kt`
- Create: `domain/model/ExerciseEntry.kt`
- Create: `domain/model/SetRecord.kt`
- Test: `src/test/.../domain/model/DomainModelTest.kt`

**Interfaces:**
- Consumes: nothing
- Produces:
  ```kotlin
  sealed interface ExerciseDefinition {
      val id: String
      val name: String
      val targetSets: Int
      val restDurationSecs: Int
  }
  data class RepExerciseDefinition(
      override val id: String, override val name: String,
      override val targetSets: Int, override val restDurationSecs: Int,
      val targetReps: Int
  ) : ExerciseDefinition

  data class TimedExerciseDefinition(
      override val id: String, override val name: String,
      override val targetSets: Int, override val restDurationSecs: Int
  ) : ExerciseDefinition

  enum class WorkoutLabel { A, B, C }
  data class Workout(val id: String, val label: WorkoutLabel, val exercises: List<ExerciseDefinition>)

  data class Session(val id: Long, val workoutId: String, val startedAt: Long, val finishedAt: Long?)
  data class ExerciseEntry(val id: Long, val sessionId: Long, val exerciseId: String, val sets: List<SetRecord>)

  sealed interface SetRecord {
      val id: Long; val entryId: Long; val completedAt: Long
  }
  data class RepSetRecord(
      override val id: Long, override val entryId: Long, override val completedAt: Long,
      val reps: Int, val weightKg: Float
  ) : SetRecord
  data class TimedSetRecord(
      override val id: Long, override val entryId: Long, override val completedAt: Long,
      val durationSecs: Int
  ) : SetRecord
  ```

- [ ] **Step 1: Write failing tests** — `DomainModelTest.kt`
  ```kotlin
  @Test fun `RepExerciseDefinition has targetReps`() {
      val ex = RepExerciseDefinition("1", "Squat", targetSets=3, restDurationSecs=90, targetReps=8)
      assertEquals(8, ex.targetReps)
  }
  @Test fun `TimedExerciseDefinition has no targetReps field`() {
      val ex: ExerciseDefinition = TimedExerciseDefinition("2", "Plank", targetSets=2, restDurationSecs=60)
      assertIs<TimedExerciseDefinition>(ex)
  }
  @Test fun `SetRecord when is exhaustive`() {
      val set: SetRecord = RepSetRecord(1L, 1L, 0L, reps=8, weightKg=80f)
      val label = when (set) {
          is RepSetRecord -> "rep"
          is TimedSetRecord -> "timed"
      }
      assertEquals("rep", label)
  }
  ```

- [ ] **Step 2: Run test to verify it fails** — `./gradlew testDebugUnitTest --tests "*DomainModelTest*"` → FAIL: classes not found

- [ ] **Step 3: Implement all domain model files** per signatures in Interfaces above

- [ ] **Step 4: Run test to verify it passes**

- [ ] **Step 5: Commit**
  ```bash
  git add domain/
  git commit -m "feat: add domain model sealed classes"
  ```

---

### Task 3: Room database + entities + DAOs

**Files:**
- Create: `data/db/GymDatabase.kt`
- Create: `data/db/entities/WorkoutEntity.kt`
- Create: `data/db/entities/ExerciseDefinitionEntity.kt`
- Create: `data/db/entities/SessionEntity.kt`
- Create: `data/db/entities/ExerciseEntryEntity.kt`
- Create: `data/db/entities/SetRecordEntity.kt`
- Create: `data/db/dao/WorkoutDao.kt`
- Create: `data/db/dao/SessionDao.kt`
- Create: `data/db/dao/ExerciseEntryDao.kt`
- Create: `data/db/dao/SetRecordDao.kt`
- Create: `data/di/DatabaseModule.kt`
- Test: `src/androidTest/.../data/db/SessionDaoTest.kt`

**Interfaces:**
- Consumes: none (entities are separate from domain models; mappers live in repositories)
- Produces:
  ```kotlin
  // ExerciseDefinitionEntity columns: id TEXT PK, workoutId TEXT, name TEXT,
  //   type TEXT ("REP"|"TIMED"), targetSets INT, restDurationSecs INT, targetReps INT nullable

  // SetRecordEntity columns: id INTEGER PK, entryId INTEGER FK, completedAt INTEGER,
  //   type TEXT ("REP"|"TIMED"), reps INT nullable, weightKg REAL nullable, durationSecs INT nullable

  @Dao interface WorkoutDao {
      @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(workouts: List<WorkoutEntity>)
      @Query("SELECT * FROM workouts") suspend fun getAll(): List<WorkoutEntity>
  }
  @Dao interface SessionDao {
      @Insert suspend fun insert(session: SessionEntity): Long
      @Update suspend fun update(session: SessionEntity)
      @Query("SELECT * FROM sessions ORDER BY startedAt DESC") fun observeAll(): Flow<List<SessionEntity>>
      @Query("SELECT * FROM sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
      suspend fun getLastCompleted(): SessionEntity?
  }
  @Dao interface ExerciseEntryDao {
      @Insert suspend fun insert(entry: ExerciseEntryEntity): Long
      @Query("SELECT * FROM exercise_entries WHERE sessionId = :sessionId")
      suspend fun getForSession(sessionId: Long): List<ExerciseEntryEntity>
  }
  @Dao interface SetRecordDao {
      @Insert suspend fun insert(record: SetRecordEntity): Long
      @Query("SELECT * FROM set_records WHERE entryId = :entryId")
      suspend fun getForEntry(entryId: Long): List<SetRecordEntity>
      @Query("""SELECT sr.* FROM set_records sr
                JOIN exercise_entries ee ON sr.entryId = ee.id
                WHERE ee.sessionId = :sessionId""")
      suspend fun getForSession(sessionId: Long): List<SetRecordEntity>
  }
  // GymDatabase: version=1, entities=[WorkoutEntity, ExerciseDefinitionEntity,
  //   SessionEntity, ExerciseEntryEntity, SetRecordEntity]
  ```

- [ ] **Step 1: Write failing DAO test** — `SessionDaoTest.kt` (instrumented, in-memory Room)
  ```kotlin
  @Test fun insertAndFinishSession() = runTest {
      val id = dao.insert(SessionEntity(0, "workout_a", startedAt=1000L, finishedAt=null))
      assertNull(dao.getLastCompleted()) // not finished
      dao.update(SessionEntity(id, "workout_a", 1000L, finishedAt=2000L))
      val last = dao.getLastCompleted()
      assertNotNull(last)
      assertEquals(id, last!!.id)
  }
  ```

- [ ] **Step 2: Run test to verify it fails** — `./gradlew connectedAndroidTest --tests "*SessionDaoTest*"` → FAIL

- [ ] **Step 3: Implement all entity and DAO files** per Interfaces above; implement `GymDatabase` wiring all entities and DAOs

- [ ] **Step 4: Implement `DatabaseModule.kt`** — `@Provides @Singleton fun provideDb(@ApplicationContext ctx: Context): GymDatabase` + individual DAO providers

- [ ] **Step 5: Run test to verify it passes**

- [ ] **Step 6: Commit**
  ```bash
  git add data/db/ data/di/DatabaseModule.kt
  git commit -m "feat: add Room database, entities, and DAOs"
  ```

---

### Task 4: Repositories + training plan seed

**Files:**
- Create: `data/seed/TrainingPlanData.kt`
- Create: `data/repository/WorkoutRepository.kt`
- Create: `data/repository/WorkoutRepositoryImpl.kt`
- Create: `data/repository/SessionRepository.kt`
- Create: `data/repository/SessionRepositoryImpl.kt`
- Create: `data/di/RepositoryModule.kt`
- Test: `src/test/.../data/repository/WorkoutRepositoryTest.kt`

**Interfaces:**
- Consumes: DAOs from Task 3, domain models from Task 2
- Produces:
  ```kotlin
  // TrainingPlanData — placeholder; replace with real exercises when user provides plan
  object TrainingPlanData {
      val workoutA = Workout("workout_a", WorkoutLabel.A, listOf(
          RepExerciseDefinition("ex_a1", "Exercise A1 (TBD)", targetSets=3, restDurationSecs=90, targetReps=8),
          RepExerciseDefinition("ex_a2", "Exercise A2 (TBD)", targetSets=3, restDurationSecs=90, targetReps=10)
      ))
      val workoutB = Workout("workout_b", WorkoutLabel.B, listOf(...)) // same placeholder pattern
      val workoutC = Workout("workout_c", WorkoutLabel.C, listOf(...))
      val all = listOf(workoutA, workoutB, workoutC)
  }

  interface WorkoutRepository {
      suspend fun ensureSeeded()          // idempotent: no-op if DB already has rows
      suspend fun getAll(): List<Workout>
      suspend fun getById(id: String): Workout?
  }
  interface SessionRepository {
      suspend fun startSession(workoutId: String): Long
      suspend fun finishSession(sessionId: Long)
      fun observeAll(): Flow<List<Session>>
      suspend fun getLastCompleted(): Session?
      suspend fun getEntriesForSession(sessionId: Long): List<ExerciseEntry>
      suspend fun addSetRecord(sessionId: Long, exerciseId: String, record: SetRecord): Long
  }
  ```

- [ ] **Step 1: Write failing tests** — `WorkoutRepositoryTest.kt`
  ```kotlin
  @Test fun `ensureSeeded inserts exactly 3 workouts`() = runTest {
      repo.ensureSeeded()
      assertEquals(3, repo.getAll().size)
  }
  @Test fun `ensureSeeded is idempotent`() = runTest {
      repo.ensureSeeded()
      repo.ensureSeeded()
      assertEquals(3, repo.getAll().size)
  }
  @Test fun `getById returns null for unknown id`() = runTest {
      repo.ensureSeeded()
      assertNull(repo.getById("nonexistent"))
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement `TrainingPlanData.kt`** with placeholder `Workout` objects

- [ ] **Step 4: Implement `WorkoutRepositoryImpl`** — `ensureSeeded` inserts `TrainingPlanData.all` with `IGNORE` conflict strategy; map entities↔domain in this class

- [ ] **Step 5: Implement `SessionRepositoryImpl`** — `startSession` inserts `SessionEntity(finishedAt=null)`; `finishSession` updates `finishedAt = System.currentTimeMillis()`; `addSetRecord` upserts an `ExerciseEntryEntity` for the exercise if none exists, then inserts `SetRecordEntity`

- [ ] **Step 6: Implement `RepositoryModule.kt`** — Hilt binds interfaces to Impl classes; `@Singleton`

- [ ] **Step 7: Call `ensureSeeded()` on app start** — in `MainActivity.onCreate` via a startup ViewModel or a `RoomDatabase.Callback`

- [ ] **Step 8: Run test to verify it passes**

- [ ] **Step 9: Commit**
  ```bash
  git add data/repository/ data/seed/ data/di/RepositoryModule.kt
  git commit -m "feat: add repositories and training plan seed data"
  ```

---

### Task 5: Use cases — rotation + session lifecycle

**Files:**
- Create: `data/prefs/RotationPrefs.kt`
- Create: `domain/usecase/GetNextWorkoutUseCase.kt`
- Create: `domain/usecase/StartSessionUseCase.kt`
- Create: `domain/usecase/LogSetUseCase.kt`
- Create: `domain/usecase/FinishSessionUseCase.kt`
- Create: `data/di/UseCaseModule.kt`
- Test: `src/test/.../domain/usecase/GetNextWorkoutUseCaseTest.kt`

**Interfaces:**
- Consumes: `WorkoutRepository`, `SessionRepository` from Task 4
- Produces:
  ```kotlin
  class RotationPrefs(context: Context) {
      suspend fun getLastLabel(): WorkoutLabel?
      suspend fun setLastLabel(label: WorkoutLabel)
  }
  // DataStore key: "last_workout_label" (String)

  class GetNextWorkoutUseCase(prefs: RotationPrefs, repo: WorkoutRepository) {
      suspend operator fun invoke(): Workout
      // A→B→C→A; returns A when no prior label
  }
  class StartSessionUseCase(repo: SessionRepository) {
      suspend operator fun invoke(workoutId: String): Long  // returns sessionId
  }
  class LogSetUseCase(repo: SessionRepository) {
      suspend operator fun invoke(sessionId: Long, exerciseId: String, record: SetRecord): Long
  }
  class FinishSessionUseCase(repo: SessionRepository, prefs: RotationPrefs, workoutRepo: WorkoutRepository) {
      suspend operator fun invoke(sessionId: Long)
      // sets finishedAt via repo.finishSession; updates rotation cursor via prefs.setLastLabel
  }
  ```

- [ ] **Step 1: Write failing rotation tests** — `GetNextWorkoutUseCaseTest.kt`
  ```kotlin
  @Test fun `returns A when no prior session`() = runTest {
      // prefs.getLastLabel() returns null
      assertEquals(WorkoutLabel.A, useCase().label)
  }
  @Test fun `returns B after A`() = runTest {
      // prefs.getLastLabel() returns A
      assertEquals(WorkoutLabel.B, useCase().label)
  }
  @Test fun `returns C after B`() = runTest {
      assertEquals(WorkoutLabel.C, useCase().label)
  }
  @Test fun `returns A after C`() = runTest {
      // prefs.getLastLabel() returns C — wraps around
      assertEquals(WorkoutLabel.A, useCase().label)
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement `RotationPrefs`** using `dataStore<Preferences>` delegate on Application context

- [ ] **Step 4: Implement all four use case classes** per signatures above

- [ ] **Step 5: Implement `UseCaseModule.kt`** — Hilt `@Provides` for each use case

- [ ] **Step 6: Run test to verify it passes**

- [ ] **Step 7: Commit**
  ```bash
  git add domain/usecase/ data/prefs/ data/di/UseCaseModule.kt
  git commit -m "feat: add rotation and session lifecycle use cases"
  ```

---

### Task 6: Home screen

**Files:**
- Create: `ui/screens/home/HomeViewModel.kt`
- Create: `ui/screens/home/HomeScreen.kt`
- Create: `ui/navigation/NavGraph.kt`
- Test: `src/test/.../ui/screens/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: `GetNextWorkoutUseCase`, `StartSessionUseCase`, `WorkoutRepository` from Tasks 4–5
- Produces:
  ```kotlin
  data class HomeUiState(
      val suggestedWorkout: Workout?,
      val selectedWorkout: Workout?,
      val allWorkouts: List<Workout>,
      val isLoading: Boolean
  )
  class HomeViewModel @HiltViewModel constructor(...) : ViewModel() {
      val uiState: StateFlow<HomeUiState>
      fun selectWorkout(workout: Workout)
      suspend fun startSession(): Long   // returns new sessionId
  }
  // NavGraph routes: "home", "session/{sessionId}", "history"
  // HomeScreen: workout chip row (A / B / C), highlighted chip = selected,
  //   suggested chip has a subtle "suggested" label, Start Session button (disabled if nothing selected)
  ```

- [ ] **Step 1: Write failing ViewModel tests** — `HomeViewModelTest.kt`
  ```kotlin
  @Test fun `uiState initially suggests next workout`() = runTest {
      // mock GetNextWorkoutUseCase returns workoutB
      viewModel.uiState.test {
          assertEquals("workout_b", awaitItem().suggestedWorkout?.id)
      }
  }
  @Test fun `selectWorkout overrides suggestion`() = runTest {
      viewModel.selectWorkout(workoutA)
      assertEquals("workout_a", viewModel.uiState.value.selectedWorkout?.id)
  }
  @Test fun `selectedWorkout defaults to suggestedWorkout`() = runTest {
      viewModel.uiState.test {
          val state = awaitItem()
          assertEquals(state.suggestedWorkout?.id, state.selectedWorkout?.id)
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement `HomeViewModel`** — load suggestion in `init {}` via `viewModelScope.launch`; `selectWorkout` updates `selectedWorkout` in `MutableStateFlow`

- [ ] **Step 4: Implement `HomeScreen`** — three `FilterChip` for A/B/C; selected one highlighted; "Suggested" badge on auto-suggested chip; `Button("Start Session")` calls `viewModel.startSession()` then navigates to `session/{id}`

- [ ] **Step 5: Implement `NavGraph.kt`** — `NavHost(startDestination = "home")` with composable routes for `home`, `session/{sessionId}`, `history`; bottom nav or top bar links

- [ ] **Step 6: Run test to verify it passes**

- [ ] **Step 7: Commit**
  ```bash
  git add ui/screens/home/ ui/navigation/
  git commit -m "feat: add Home screen with workout suggestion and navigation"
  ```

---

### Task 7: Session screen + running clock

**Files:**
- Create: `ui/screens/session/SessionViewModel.kt`
- Create: `ui/screens/session/SessionScreen.kt`
- Create: `ui/components/SessionClockDisplay.kt`
- Create: `ui/components/ExerciseCard.kt`
- Test: `src/test/.../ui/screens/session/SessionViewModelTest.kt`

**Interfaces:**
- Consumes: `SessionRepository`, `WorkoutRepository`, `FinishSessionUseCase` from Tasks 4–5
- Produces:
  ```kotlin
  data class SessionUiState(
      val workout: Workout?,
      val elapsedSecs: Long,
      val entries: Map<String, ExerciseEntry>,  // exerciseId → ExerciseEntry
      val isFinished: Boolean
  )
  class SessionViewModel @HiltViewModel constructor(
      savedStateHandle: SavedStateHandle,  // reads "sessionId"
      ...
  ) : ViewModel() {
      val uiState: StateFlow<SessionUiState>
      fun finishSession()
  }
  // SessionScreen: top bar shows MM:SS clock + workout label; scrollable LazyColumn of ExerciseCards;
  //   floating Finish button; on finish navigates back to home
  // ExerciseCard: exercise name, "X / Y sets" subtitle, tap opens ExerciseDetailSheet
  // SessionClockDisplay(elapsedSecs: Long): Text(formatMMSS(elapsedSecs))
  // NOTE: clock ticker runs only while ViewModel is alive (foregrounded); process-death recovery out of scope
  ```

- [ ] **Step 1: Write failing ViewModel tests** — `SessionViewModelTest.kt`
  ```kotlin
  @Test fun `elapsedSecs increments each second`() = runTest {
      viewModel.uiState.test {
          val t0 = awaitItem().elapsedSecs
          advanceTimeBy(2_000)
          val t1 = awaitItem().elapsedSecs
          assertTrue(t1 >= t0 + 2)
      }
  }
  @Test fun `finishSession sets isFinished true`() = runTest {
      viewModel.finishSession()
      assertTrue(viewModel.uiState.value.isFinished)
  }
  @Test fun `finish session without logging any exercise succeeds`() = runTest {
      // no ExerciseEntry logged — finishing must still complete cleanly
      viewModel.finishSession()
      assertTrue(viewModel.uiState.value.isFinished)
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement `SessionViewModel`** — start a `ticker` flow (`flow { while(true) { emit(Unit); delay(1000) } }`) in `viewModelScope` updating `elapsedSecs`

- [ ] **Step 4: Implement `SessionScreen`**, `SessionClockDisplay`, `ExerciseCard`

- [ ] **Step 5: Run test to verify it passes**

- [ ] **Step 6: Commit**
  ```bash
  git add ui/screens/session/SessionViewModel.kt ui/screens/session/SessionScreen.kt ui/components/
  git commit -m "feat: add Session screen with running clock and exercise cards"
  ```

---

### Task 8: Set logging sheet + rest timer

**Files:**
- Create: `ui/screens/session/ExerciseDetailSheet.kt`
- Create: `ui/screens/session/ExerciseDetailViewModel.kt`
- Create: `ui/components/RestTimerOverlay.kt`
- Create: `ui/components/SetLogRow.kt`
- Test: `src/test/.../ui/screens/session/ExerciseDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `LogSetUseCase` from Task 5; `ExerciseDefinition` sealed types from Task 2
- Produces:
  ```kotlin
  data class ExerciseDetailUiState(
      val exercise: ExerciseDefinition,
      val completedSets: List<SetRecord>,
      val restCountdownSecs: Int?,    // null = no active rest timer
      val timerRunning: Boolean,      // true while a TimedExercise set is being timed
      val activeTimerStartMs: Long?   // epoch ms when current timed set started
  )
  class ExerciseDetailViewModel @HiltViewModel constructor(
      savedStateHandle: SavedStateHandle,  // reads "sessionId", "exerciseId"
      ...
  ) : ViewModel() {
      val uiState: StateFlow<ExerciseDetailUiState>
      fun logRepSet(reps: Int, weightKg: Float)   // saves RepSetRecord, starts rest countdown
      fun startTimedSet()                          // sets timerRunning=true, records start epoch
      fun stopTimedSet()                           // saves TimedSetRecord, starts rest countdown
      fun dismissRestTimer()                       // clears restCountdownSecs
  }
  // ExerciseDetailSheet: ModalBottomSheet
  //   — RepExerciseDefinition branch: reps TextField + weight TextField + "Log Set" Button
  //   — TimedExerciseDefinition branch: "Start" / "Stop" Button showing elapsed time
  //   — SetLogRow list of completed sets above input area
  //   — RestTimerOverlay sits on top of sheet content when restCountdownSecs != null
  ```

- [ ] **Step 1: Write failing ViewModel tests** — `ExerciseDetailViewModelTest.kt`
  ```kotlin
  @Test fun `logRepSet saves set and starts rest countdown`() = runTest {
      viewModel.logRepSet(8, 80f)
      val state = viewModel.uiState.value
      assertEquals(1, state.completedSets.size)
      assertNotNull(state.restCountdownSecs)
      assertEquals(exercise.restDurationSecs, state.restCountdownSecs)
  }
  @Test fun `rest countdown decrements to zero then clears`() = runTest {
      viewModel.logRepSet(8, 80f)
      advanceTimeBy((exercise.restDurationSecs * 1000 + 500).toLong())
      assertNull(viewModel.uiState.value.restCountdownSecs)
  }
  @Test fun `dismissRestTimer clears countdown immediately`() = runTest {
      viewModel.logRepSet(8, 80f)
      viewModel.dismissRestTimer()
      assertNull(viewModel.uiState.value.restCountdownSecs)
  }
  @Test fun `stopTimedSet records durationSecs and starts rest`() = runTest {
      viewModel.startTimedSet()
      advanceTimeBy(5_000)
      viewModel.stopTimedSet()
      val set = viewModel.uiState.value.completedSets.first()
      assertIs<TimedSetRecord>(set)
      assertTrue((set as TimedSetRecord).durationSecs >= 5)
      assertNotNull(viewModel.uiState.value.restCountdownSecs)
  }
  @Test fun `stopTimedSet is no-op if timer not running`() = runTest {
      viewModel.stopTimedSet()
      assertTrue(viewModel.uiState.value.completedSets.isEmpty())
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement `ExerciseDetailViewModel`** — rest timer as a decrement coroutine in `viewModelScope`; cancel on dismiss; branch on exercise type in `logRepSet` / `startTimedSet` / `stopTimedSet`

- [ ] **Step 4: Implement `ExerciseDetailSheet`** — `ModalBottomSheet`; `when (exercise)` branch for rep vs timed UI; `SetLogRow` per completed set; `RestTimerOverlay` on top when countdown active

- [ ] **Step 5: Wire sheet into `SessionScreen`** — `ExerciseCard` click sets `selectedExerciseId` in `SessionViewModel`; sheet shown via `if (selectedExerciseId != null) ExerciseDetailSheet(...)`

- [ ] **Step 6: Run test to verify it passes**

- [ ] **Step 7: Commit**
  ```bash
  git add ui/screens/session/ExerciseDetail* ui/components/RestTimerOverlay.kt ui/components/SetLogRow.kt
  git commit -m "feat: add set logging sheet and rest timer"
  ```

---

### Task 9: History screen

**Files:**
- Create: `ui/screens/history/HistoryViewModel.kt`
- Create: `ui/screens/history/HistoryScreen.kt`
- Test: `src/test/.../ui/screens/history/HistoryViewModelTest.kt`

**Interfaces:**
- Consumes: `SessionRepository` from Task 4; domain models from Task 2
- Produces:
  ```kotlin
  data class ExerciseSummary(val exerciseName: String, val sets: List<SetRecord>)
  data class TrendPoint(val dateLabel: String, val value: Float)  // weight (kg) or duration (secs)

  data class LastSessionUiState(
      val workoutLabel: WorkoutLabel?,
      val dateLabel: String?,
      val summaries: List<ExerciseSummary>   // empty if no completed session
  )
  data class TrendsUiState(
      val exerciseNames: List<String>,
      val selectedExercise: String?,
      val dataPoints: List<TrendPoint>       // ascending by date; empty if none
  )
  class HistoryViewModel @HiltViewModel constructor(...) : ViewModel() {
      val lastSession: StateFlow<LastSessionUiState>
      val trends: StateFlow<TrendsUiState>
      fun selectExerciseForTrend(name: String)
  }
  // HistoryScreen: TabRow with tabs "Last Session" and "Trends"
  // Last Session tab: if summaries empty → EmptyState("No sessions yet"); else LazyColumn of ExerciseSummary cards
  // Trends tab: exercise dropdown (ExposedDropdownMenuBox); Compose Canvas line chart of TrendPoints;
  //   x = date labels, y = value; empty state if no dataPoints
  ```

- [ ] **Step 1: Write failing ViewModel tests** — `HistoryViewModelTest.kt`
  ```kotlin
  @Test fun `lastSession empty state when no sessions`() = runTest {
      // repo.getLastCompleted() returns null
      val state = viewModel.lastSession.value
      assertNull(state.workoutLabel)
      assertTrue(state.summaries.isEmpty())
  }
  @Test fun `trends empty state when no sessions`() = runTest {
      assertTrue(viewModel.trends.value.dataPoints.isEmpty())
  }
  @Test fun `trends dataPoints ordered ascending by date`() = runTest {
      // seed two sessions at t=1000 and t=2000 with same exercise
      val points = viewModel.trends.value.dataPoints
      assertEquals(2, points.size)
      assertTrue(points[0].dateLabel <= points[1].dateLabel)
  }
  @Test fun `selectExerciseForTrend updates dataPoints`() = runTest {
      viewModel.selectExerciseForTrend("Squat")
      assertEquals("Squat", viewModel.trends.value.selectedExercise)
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement `HistoryViewModel`** — `lastSession` derived from `repo.getLastCompleted()` + entry + set queries; `trends` derived from all sessions filtered by `selectedExercise`; date formatted with `SimpleDateFormat("dd MMM", Locale.getDefault())`

- [ ] **Step 4: Implement `HistoryScreen`** — `TabRow` + `HorizontalPager`; Last Session tab with empty state; Trends tab with dropdown + `Canvas` line chart; handle `dataPoints.isEmpty()` gracefully

- [ ] **Step 5: Wire History into `NavGraph`** — add a history icon button to `SessionScreen` top bar or bottom nav; route navigates to `history`

- [ ] **Step 6: Run test to verify it passes**

- [ ] **Step 7: Commit**
  ```bash
  git add ui/screens/history/
  git commit -m "feat: add History screen with Last Session and Trends tabs"
  ```

---

## Self-Review Checklist

- [x] **Spec coverage**: A/B/C ✓, rotation ✓, hardcoded plan ✓, rep exercise ✓, timed exercise (Plank) ✓, free order / skip ✓, running clock ✓, set log after completion ✓, rest timer per exercise ✓, finish session ✓, history two tabs ✓
- [x] **Step scan**: each step has one action; no "TBD" steps; no bodies duplicating what signatures already determine
- [x] **Type consistency**: `Workout`, `WorkoutLabel`, `SetRecord`, `ExerciseEntry`, `SessionRepository`, `WorkoutRepository` names consistent across all tasks
- [x] **Review Focus covered**: rotation C→A test in Task 5 ✓; timed exercise no-weight branch in Task 8 ✓; rest dismiss test in Task 8 ✓; clock backgrounding noted in Task 7 ✓; history empty state tests in Task 9 ✓
- [x] **Proportion**: plan describes interfaces and tests; no implementation bodies except algorithm-critical code
