package com.doron.gymtracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.doron.gymtracker.data.db.dao.ExerciseEntryDao
import com.doron.gymtracker.data.db.dao.SessionDao
import com.doron.gymtracker.data.db.dao.SetRecordDao
import com.doron.gymtracker.data.db.dao.WorkoutDao
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity
import com.doron.gymtracker.data.db.entities.SessionEntity
import com.doron.gymtracker.data.db.entities.SetRecordEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity

@Database(
    entities = [
        WorkoutEntity::class,
        ExerciseDefinitionEntity::class,
        SessionEntity::class,
        ExerciseEntryEntity::class,
        SetRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun sessionDao(): SessionDao
    abstract fun exerciseEntryDao(): ExerciseEntryDao
    abstract fun setRecordDao(): SetRecordDao
}
