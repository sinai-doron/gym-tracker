package com.doron.gymtracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doron.gymtracker.data.db.dao.ExerciseEntryDao
import com.doron.gymtracker.data.db.dao.LocationLogDao
import com.doron.gymtracker.data.db.dao.SessionDao
import com.doron.gymtracker.data.db.dao.SetRecordDao
import com.doron.gymtracker.data.db.dao.TrackLogDao
import com.doron.gymtracker.data.db.dao.WorkoutDao
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity
import com.doron.gymtracker.data.db.entities.LocationLogEntity
import com.doron.gymtracker.data.db.entities.SessionEntity
import com.doron.gymtracker.data.db.entities.SetRecordEntity
import com.doron.gymtracker.data.db.entities.TrackLogEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity

@Database(
    entities = [
        WorkoutEntity::class,
        ExerciseDefinitionEntity::class,
        SessionEntity::class,
        ExerciseEntryEntity::class,
        SetRecordEntity::class,
        TrackLogEntity::class,
        LocationLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun sessionDao(): SessionDao
    abstract fun exerciseEntryDao(): ExerciseEntryDao
    abstract fun setRecordDao(): SetRecordDao
    abstract fun trackLogDao(): TrackLogDao
    abstract fun locationLogDao(): LocationLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE set_records ADD COLUMN distanceM INTEGER")
                database.execSQL("ALTER TABLE exercise_definitions ADD COLUMN targetDistanceM INTEGER")
                database.execSQL("ALTER TABLE exercise_definitions ADD COLUMN isOptional INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE exercise_definitions ADD COLUMN isPerSide INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `track_log` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sessions ADD COLUMN type TEXT NOT NULL DEFAULT 'GYM'")
                database.execSQL("ALTER TABLE sessions ADD COLUMN distanceMeters REAL")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `location_log` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `speed` REAL,
                        `altitude` REAL
                    )
                """)
            }
        }

        // Bodyweight exercises are logged without weight. The plan is seeded once,
        // so existing installs need the seeded bodyweight exercises flagged here.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercise_definitions ADD COLUMN isBodyweight INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE exercise_definitions SET isBodyweight = 1 WHERE id IN ('d2_e8a', 'd2_e8b')")
            }
        }

        // Week-B variants become their own rows (inserted by WorkoutRepository's plan sync).
        // The crunches are bodyweight, not cable, so the seeded name is fixed here too.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercise_definitions ADD COLUMN variantOfId TEXT")
                database.execSQL("UPDATE exercise_definitions SET name = 'כפיפות בטן' WHERE id = 'd2_e8a'")
            }
        }
    }
}
