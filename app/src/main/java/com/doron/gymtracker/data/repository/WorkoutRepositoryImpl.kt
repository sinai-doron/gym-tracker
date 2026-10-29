package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.WorkoutDao
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.model.WorkoutLabel
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WorkoutRepositoryImpl @Inject constructor(
    private val dao: WorkoutDao
) : WorkoutRepository {

    private val seedLock = Mutex()
    @Volatile private var seeded = false

    // Inserts whatever part of the plan is missing, in one transaction (IGNORE keeps existing
    // rows, so edited set counts survive). Covers a fresh install, an install a crash left
    // half-seeded, and exercises added to the plan later, like the week-B variant rows.
    override suspend fun ensureSeeded() {
        if (seeded) return
        seedLock.withLock {
            if (seeded) return
            val workoutEntities = TrainingPlanData.all.map { WorkoutEntity(it.id, it.label.name) }
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

    override suspend fun getAll(): List<Workout> {
        ensureSeeded()
        val workoutEntities = dao.getAll()
        return workoutEntities.map { entity ->
            val rows = dao.getExercisesForWorkout(entity.id)
            val variantsByBase = rows.filter { it.variantOfId != null }.associateBy { it.variantOfId }
            val exercises = rows.filter { it.variantOfId == null }.map { base ->
                base.toDomain(variantB = variantsByBase[base.id]?.toDomain())
            }
            Workout(entity.id, WorkoutLabel.valueOf(entity.label), exercises)
        }
    }

    override suspend fun getById(id: String): Workout? =
        getAll().firstOrNull { it.id == id }

    override suspend fun updateTargetSets(exerciseId: String, sets: Int) =
        dao.updateTargetSets(exerciseId, sets)

    override suspend fun updateExercise(exercise: ExerciseDefinition) {
        val existing = dao.getExercise(exercise.id) ?: return
        val edited = exercise.toEntity(existing.workoutId, existing.variantOfId, existing.sortOrder)
        dao.updateExercise(edited.copy(type = existing.type))
    }

    override suspend fun addExercise(workoutId: String, exercise: ExerciseDefinition) {
        dao.insertExercise(exercise.toEntity(workoutId, sortOrder = dao.maxSortOrder(workoutId) + 1))
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
            variantOfId = variantOfId, sortOrder = sortOrder
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

    private fun ExerciseDefinitionEntity.toDomain(variantB: ExerciseDefinition? = null): ExerciseDefinition = when (type) {
        "REP" -> RepExerciseDefinition(
            id, name, targetSets, restDurationSecs, targetReps!!, variantB = variantB,
            isOptional = isOptional, isPerSide = isPerSide, isBodyweight = isBodyweight
        )
        "DISTANCE" -> DistanceExerciseDefinition(
            id, name, targetSets, restDurationSecs, targetDistanceM!!, variantB = variantB,
            isOptional = isOptional, isPerSide = isPerSide
        )
        else -> TimedExerciseDefinition(
            id, name, targetSets, restDurationSecs, variantB = variantB,
            isOptional = isOptional, isPerSide = isPerSide
        )
    }
}
