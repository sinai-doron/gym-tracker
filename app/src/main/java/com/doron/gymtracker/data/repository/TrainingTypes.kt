package com.doron.gymtracker.data.repository

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.doron.gymtracker.data.db.ActivityTypeMigration
import com.doron.gymtracker.data.db.dao.ActivityTypeDao
import com.doron.gymtracker.data.db.entities.ActivityTypeEntity
import com.doron.gymtracker.domain.model.Recorder
import com.doron.gymtracker.domain.model.TrainingType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The training types: the three the app always had (strength, run, walk), the plan's first-release
 * list as built-ins that start hidden, so a user shows only what they do, and the user's own.
 * Built-ins are hidden, never deleted; a user's own is deleted while nothing was done in it.
 */
@Singleton
class TrainingTypes @Inject constructor(
    private val dao: ActivityTypeDao
) {
    private val seedLock = Mutex()
    @Volatile private var seeded = false

    /** Adds the hidden built-ins a phone doesn't have yet, hidden; ones it has keep their state. */
    suspend fun ensureBuiltins(now: Long = System.currentTimeMillis()) {
        if (seeded) return
        seedLock.withLock {
            if (seeded) return
            dao.insertMissing(HIDDEN_BUILTINS.map { it.copy(hiddenAt = now) })
            seeded = true
        }
    }

    /** Every type, hidden ones too, in order. */
    suspend fun all(): List<TrainingType> {
        ensureBuiltins()
        return dao.getAll().map { it.toDomain() }
    }

    /** The types to offer: shown, in order. */
    suspend fun shown(): List<TrainingType> = all().filter { !it.hidden }

    suspend fun get(id: String): TrainingType? {
        ensureBuiltins()
        return dao.get(id)?.toDomain()
    }

    suspend fun setShown(id: String, shown: Boolean) {
        // Strength is what the programme is: it stays
        if (id == ActivityTypeMigration.STRENGTH) return
        dao.setHidden(id, if (shown) null else System.currentTimeMillis())
    }

    /** A type of the user's own, recorded with a timer; returns its id. */
    suspend fun create(name: String, icon: String): String {
        val id = UUID.randomUUID().toString()
        val order = dao.maxSortOrder() + 1
        dao.insert(
            ActivityTypeEntity(
                id = id,
                name = name.trim(),
                icon = icon,
                color = OWN_COLORS[order % OWN_COLORS.size],
                recorder = Recorder.DURATION.name,
                healthConnectType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
                metValue = OWN_MET,
                builtin = false,
                sortOrder = order
            )
        )
        return id
    }

    suspend fun edit(id: String, name: String, icon: String) = dao.edit(id, name.trim(), icon)

    /** Deletes a type of the user's own nothing was done in; hides it otherwise. Built-ins are only hidden. */
    suspend fun remove(id: String) {
        val type = dao.get(id) ?: return
        if (!type.builtin && dao.sessionCount(id) == 0) dao.deleteOwn(id) else setShown(id, false)
    }

    companion object {
        /** MET for a type of the user's own: moderate effort, as the Compendium's "general" entries. */
        const val OWN_MET = 5.0

        private val OWN_COLORS = listOf("#E17055", "#0984E3", "#E84393", "#FDCB6E", "#00CEC9", "#D63031", "#A29BFE", "#636E72")

        /**
         * The plan's first-release types beyond strength, run and walk, all timed for now. MET values
         * are the 2011 Compendium's general entries; Health Connect types the nearest it has.
         */
        val HIDDEN_BUILTINS: List<ActivityTypeEntity> = listOf(
            builtin("pilates", "pilates", "#E17055", ExerciseSessionRecord.EXERCISE_TYPE_PILATES, 3.0, 10),
            builtin("yoga", "yoga", "#A29BFE", ExerciseSessionRecord.EXERCISE_TYPE_YOGA, 2.5, 11),
            builtin("martial_arts", "martial_arts", "#D63031", ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS, 7.0, 12),
            builtin("swimming", "swimming", "#0984E3", ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, 6.0, 13),
            builtin("hiit", "hiit", "#E84393", ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING, 8.0, 14),
            builtin("team_sports", "football", "#00CEC9", ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, 7.0, 15),
            builtin("mobility", "mobility", "#FDCB6E", ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING, 2.3, 16),
            builtin("cardio_machines", "cycling", "#636E72", ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, 7.0, 17),
            builtin("other", "star", "#B2BEC3", ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, 4.0, 18),
        )

        private fun builtin(id: String, icon: String, color: String, hc: Int, met: Double, order: Int) = ActivityTypeEntity(
            id = id, name = null, icon = icon, color = color, recorder = Recorder.DURATION.name,
            healthConnectType = hc, metValue = met, builtin = true, sortOrder = order
        )

        fun ActivityTypeEntity.toDomain() = TrainingType(
            id = id,
            name = name,
            icon = icon,
            color = color,
            recorder = runCatching { Recorder.valueOf(recorder) }.getOrDefault(Recorder.DURATION),
            healthConnectType = healthConnectType,
            metValue = metValue,
            builtin = builtin,
            sortOrder = sortOrder,
            hidden = hiddenAt != null
        )
    }
}
