package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.ProgramDao
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.Program
import com.doron.gymtracker.domain.model.ProgramSummary
import com.doron.gymtracker.domain.model.VariantRule
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** The user's programmes: one active, the rest kept or archived. History is never touched. */
interface ProgramRepository {
    suspend fun getAll(): List<ProgramSummary>
    suspend fun get(id: String): Program?
    /** A new, empty programme, not active yet; returns its id. */
    suspend fun create(name: String, startDate: LocalDate): String
    suspend fun rename(id: String, name: String)
    suspend fun setStartDate(id: String, date: LocalDate)
    /** The next workout comes from this programme from now on. */
    suspend fun makeActive(id: String)
    /** Only a programme that isn't active can be archived. */
    suspend fun archive(id: String)
    suspend fun restore(id: String)
    /** Deletes a programme nothing was done in; false, and nothing deleted, otherwise. */
    suspend fun delete(id: String): Boolean
}

class ProgramRepositoryImpl @Inject constructor(
    private val dao: ProgramDao,
    private val workouts: WorkoutRepository
) : ProgramRepository {

    override suspend fun getAll(): List<ProgramSummary> {
        workouts.ensureSeeded()
        return dao.getAll().map { p ->
            ProgramSummary(
                program = p.toDomain(),
                isActive = p.isActive,
                archivedAt = p.archivedAt,
                workoutCount = dao.workoutCount(p.id),
                sessionCount = dao.sessionCount(p.id),
                isSeeded = p.id == TrainingPlanData.PROGRAM_ID
            )
        }
    }

    override suspend fun get(id: String): Program? = dao.get(id)?.toDomain()

    override suspend fun create(name: String, startDate: LocalDate): String {
        val id = UUID.randomUUID().toString()
        dao.insert(Program(id, name, startDate, deloadEveryWeeks = null, variantRule = VariantRule.NONE).toEntity(isActive = false))
        return id
    }

    override suspend fun rename(id: String, name: String) = dao.rename(id, name)

    override suspend fun setStartDate(id: String, date: LocalDate) = dao.setStart(id, date.toEpochDay())

    override suspend fun makeActive(id: String) = dao.makeActive(id)

    override suspend fun archive(id: String) = dao.setArchived(id, System.currentTimeMillis())

    override suspend fun restore(id: String) = dao.setArchived(id, null)

    override suspend fun delete(id: String): Boolean {
        val summary = getAll().firstOrNull { it.program.id == id } ?: return false
        if (!summary.canDelete) return false
        workouts.getForProgram(id).forEach { workouts.removeWorkout(it.id) }
        dao.delete(id)
        return true
    }
}
