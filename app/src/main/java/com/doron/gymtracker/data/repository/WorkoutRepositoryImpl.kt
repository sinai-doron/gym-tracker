package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.ExerciseLibraryDao
import com.doron.gymtracker.data.db.dao.ProgramDao
import com.doron.gymtracker.data.db.dao.WorkoutDao
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.Program
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.domain.model.Workout
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WorkoutRepositoryImpl @Inject constructor(
    private val dao: WorkoutDao,
    private val programDao: ProgramDao,
    private val libraryDao: ExerciseLibraryDao
) : WorkoutRepository {

    private val seedLock = Mutex()
    @Volatile private var seeded = false

    // Tops up the owner's A/B/C plan, on installs that have it (the v16 migration made it from
    // their history): inserts whatever part is missing, in one transaction (IGNORE keeps existing
    // rows, so edited set counts survive), like the week-B variant rows added later. A fresh
    // install starts with no programme, and gets one from a template or the builder.
    override suspend fun ensureSeeded() {
        if (seeded) return
        seedLock.withLock {
            if (seeded) return
            if (programDao.get(TrainingPlanData.PROGRAM_ID) == null) {
                seeded = true
                return
            }
            val workoutEntities = TrainingPlanData.all.mapIndexed { i, w ->
                WorkoutEntity(w.id, w.code, programId = TrainingPlanData.PROGRAM_ID, sortOrder = i + 1)
            }
            val exerciseEntities = TrainingPlanData.all.flatMap { workout ->
                workout.exercises.mapIndexed { i, ex -> ex.toEntity(workout.id, sortOrder = i + 1) } +
                    workout.exercises.mapNotNull { base ->
                        base.variantB?.toEntity(workout.id, variantOfId = base.id)
                    }
            }
            dao.seed(workoutEntities, exerciseEntities)
            seeded = true
        }
    }

    override suspend fun getActiveProgram(): Program? {
        ensureSeeded()
        return programDao.getActive()?.toDomain()
    }

    override suspend fun getAll(): List<Workout> = getActiveProgram()?.let { getForProgram(it.id) }.orEmpty()

    override suspend fun getForProgram(programId: String): List<Workout> {
        ensureSeeded()
        return dao.getForProgram(programId).map { it.toDomain() }
    }

    override suspend fun getById(id: String): Workout? {
        ensureSeeded()
        return dao.getWorkout(id)?.toDomain()
    }

    override suspend fun getExercise(id: String): ExerciseDefinition? {
        val row = dao.getExercise(id) ?: return null
        return row.toDomain(names = libraryNames(listOf(row))[row.id].orEmpty(), step = weightSteps(listOf(row))[row.libraryExerciseId])
    }

    private suspend fun WorkoutEntity.toDomain(): Workout {
        val rows = dao.getExercisesForWorkout(id)
        val names = libraryNames(rows)
        val steps = weightSteps(rows)
        val variantsByBase = rows.filter { it.variantOfId != null }.associateBy { it.variantOfId }
        val exercises = rows.filter { it.variantOfId == null }.map { base ->
            val variant = variantsByBase[base.id]?.let { it.toDomain(names = names[it.id].orEmpty(), step = steps[it.libraryExerciseId]) }
            base.toDomain(variantB = variant, names = names[base.id].orEmpty(), step = steps[base.libraryExerciseId])
        }
        return Workout(id, label, exercises, name = name, sortOrder = sortOrder, weekday = weekday?.let(DayOfWeek::of))
    }

    override suspend fun installProgram(program: Program, workouts: List<NewWorkout>) {
        val rows = workouts.mapIndexed { i, w ->
            WorkoutEntity(UUID.randomUUID().toString(), w.code, programId = program.id, sortOrder = i + 1, name = w.name) to w
        }
        programDao.install(
            program.toEntity(isActive = true),
            rows.map { it.first },
            rows.flatMap { (row, w) ->
                w.exercises.mapIndexed { i, (exercise, libraryId) ->
                    exercise.toEntity(row.id, sortOrder = i + 1).copy(libraryExerciseId = libraryId)
                }
            }
        )
    }

    override suspend fun addWorkout(programId: String, code: String, name: String?, weekday: DayOfWeek?): String {
        val id = UUID.randomUUID().toString()
        dao.insertWorkout(
            WorkoutEntity(
                id, code, programId = programId, sortOrder = dao.maxWorkoutSortOrder(programId) + 1, name = name,
                weekday = weekday?.value
            )
        )
        return id
    }

    override suspend fun editWorkout(id: String, code: String, name: String?, weekday: DayOfWeek?) =
        dao.editWorkout(id, code, name, weekday?.value)

    override suspend fun moveWorkout(programId: String, workoutId: String, direction: Int) {
        val workouts = dao.getForProgram(programId)
        val from = workouts.indexOfFirst { it.id == workoutId }
        val to = from + direction
        if (from < 0 || to !in workouts.indices) return
        workouts.toMutableList().apply { add(to, removeAt(from)) }
            .forEachIndexed { i, w -> dao.setWorkoutSortOrder(w.id, i + 1) }
    }

    // A workout with history is hidden so its sessions still find it; so is one of the seeded
    // plan, which ensureSeeded would otherwise put back. Any other goes, with its exercises.
    override suspend fun removeWorkout(id: String) {
        val seeded = TrainingPlanData.all.any { it.id == id }
        if (seeded || dao.sessionCount(id) > 0) dao.hideWorkout(id, System.currentTimeMillis())
        else dao.deleteWorkout(id)
    }

    override suspend fun updateTargetSets(exerciseId: String, sets: Int) =
        dao.updateTargetSets(exerciseId, sets)

    override suspend fun updateExercise(exercise: ExerciseDefinition) {
        val existing = dao.getExercise(exercise.id) ?: return
        val edited = exercise.toEntity(existing.workoutId, existing.variantOfId, existing.sortOrder)
        dao.updateExercise(edited.copy(type = existing.type, libraryExerciseId = existing.libraryExerciseId))
    }

    override suspend fun addExercise(workoutId: String, exercise: ExerciseDefinition, libraryExerciseId: String?) {
        dao.insertExercise(
            exercise.toEntity(workoutId, sortOrder = dao.maxSortOrder(workoutId) + 1).copy(libraryExerciseId = libraryExerciseId)
        )
    }

    override suspend fun removeExercise(exerciseId: String) = dao.markRemoved(exerciseId)

    override suspend fun moveExercise(workoutId: String, exerciseId: String, direction: Int) {
        val bases = dao.getExercisesForWorkout(workoutId).filter { it.variantOfId == null }
        val from = bases.indexOfFirst { it.id == exerciseId }
        val to = from + direction
        if (from < 0 || to !in bases.indices) return
        // Renumber the whole list so ties from older rows can't make the swap a no-op
        val reordered = bases.toMutableList().apply { add(to, removeAt(from)) }
        reordered.forEachIndexed { i, row -> dao.setSortOrder(row.id, i + 1) }
    }

    private fun ExerciseDefinition.toEntity(workoutId: String, variantOfId: String? = null, sortOrder: Int = 0) = when (this) {
        is RepExerciseDefinition -> ExerciseDefinitionEntity(
            id, workoutId, name, "REP", targetSets, restDurationSecs,
            targetReps = targetReps, targetDistanceM = null,
            isOptional = isOptional, isPerSide = isPerSide, isBodyweight = isBodyweight,
            variantOfId = variantOfId, sortOrder = sortOrder, targetRepsMin = targetRepsMin
        )
        is TimedExerciseDefinition -> ExerciseDefinitionEntity(
            id, workoutId, name, "TIMED", targetSets, restDurationSecs,
            targetReps = null, targetDistanceM = null,
            isOptional = isOptional, isPerSide = isPerSide, variantOfId = variantOfId, sortOrder = sortOrder
        )
        is DistanceExerciseDefinition -> ExerciseDefinitionEntity(
            id, workoutId, name, "DISTANCE", targetSets, restDurationSecs,
            targetReps = null, targetDistanceM = targetDistanceM,
            isOptional = isOptional, isPerSide = isPerSide, variantOfId = variantOfId, sortOrder = sortOrder
        )
    }

    /**
     * Each linked row's library names by language, when its saved name is one of that exercise's
     * names or aliases in any language: a plan added in Hebrew then shows in English, while a name
     * the user typed stays theirs. By row id.
     */
    private suspend fun libraryNames(rows: List<ExerciseDefinitionEntity>): Map<String, Map<String, String>> {
        val linked = rows.filter { it.libraryExerciseId != null }
        if (linked.isEmpty()) return emptyMap()
        val byExercise = libraryDao.namesFor(linked.mapNotNull { it.libraryExerciseId }.distinct()).groupBy { it.exerciseId }
        return linked.mapNotNull { row ->
            val names = byExercise[row.libraryExerciseId].orEmpty()
            if (names.none { it.name == row.name }) return@mapNotNull null
            row.id to names.filter { !it.isAlias }.associate { it.lang to it.name }
        }.toMap()
    }

    /** The weight step of each library exercise the rows are, from its equipment; by library id. */
    private suspend fun weightSteps(rows: List<ExerciseDefinitionEntity>): Map<String, Double> {
        val ids = rows.mapNotNull { it.libraryExerciseId }.distinct()
        if (ids.isEmpty()) return emptyMap()
        return libraryDao.getAll(ids).mapNotNull { e -> e.weightStepKg?.let { e.id to it } }.toMap()
    }

    private fun ExerciseDefinitionEntity.toDomain(
        variantB: ExerciseDefinition? = null,
        names: Map<String, String> = emptyMap(),
        step: Double? = null
    ): ExerciseDefinition = when (type) {
        "REP" -> RepExerciseDefinition(
            id, name, targetSets, restDurationSecs, targetReps!!, variantB = variantB,
            isOptional = isOptional, isPerSide = isPerSide, names = names, isBodyweight = isBodyweight, targetRepsMin = targetRepsMin,
            weightStepKg = step
        )
        "DISTANCE" -> DistanceExerciseDefinition(
            id, name, targetSets, restDurationSecs, targetDistanceM!!, variantB = variantB,
            isOptional = isOptional, isPerSide = isPerSide, names = names
        )
        else -> TimedExerciseDefinition(
            id, name, targetSets, restDurationSecs, variantB = variantB,
            isOptional = isOptional, isPerSide = isPerSide, names = names
        )
    }
}
