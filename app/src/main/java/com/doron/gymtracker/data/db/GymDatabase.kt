package com.doron.gymtracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doron.gymtracker.data.db.dao.ActivityTypeDao
import com.doron.gymtracker.data.db.dao.ExerciseEntryDao
import com.doron.gymtracker.data.db.dao.ExerciseLibraryDao
import com.doron.gymtracker.data.db.dao.LocationLogDao
import com.doron.gymtracker.data.db.dao.ProgramDao
import com.doron.gymtracker.data.db.dao.RouteDao
import com.doron.gymtracker.data.db.dao.RunDetailsDao
import com.doron.gymtracker.data.db.dao.SessionDao
import com.doron.gymtracker.data.db.dao.SetDao
import com.doron.gymtracker.data.db.dao.TrackLogDao
import com.doron.gymtracker.data.db.dao.WorkoutDao
import com.doron.gymtracker.data.db.entities.ActivityTypeEntity
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.ExerciseEntity
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity
import com.doron.gymtracker.data.db.entities.ExerciseMuscleEntity
import com.doron.gymtracker.data.db.entities.ExerciseNameEntity
import com.doron.gymtracker.data.db.entities.LocationLogEntity
import com.doron.gymtracker.data.db.entities.ProgramEntity
import com.doron.gymtracker.data.db.entities.RouteEntity
import com.doron.gymtracker.data.db.entities.RoutePointEntity
import com.doron.gymtracker.data.db.entities.RunDetailsEntity
import com.doron.gymtracker.data.db.entities.RunPauseEntity
import com.doron.gymtracker.data.db.entities.RunStepEntity
import com.doron.gymtracker.data.db.entities.SessionEntity
import com.doron.gymtracker.data.db.entities.SetEntity
import com.doron.gymtracker.data.db.entities.SetPartEntity
import com.doron.gymtracker.data.db.entities.TrackLogEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity

@Database(
    entities = [
        WorkoutEntity::class,
        ExerciseDefinitionEntity::class,
        SessionEntity::class,
        ExerciseEntryEntity::class,
        SetEntity::class,
        SetPartEntity::class,
        TrackLogEntity::class,
        LocationLogEntity::class,
        RunDetailsEntity::class,
        RouteEntity::class,
        RoutePointEntity::class,
        RunPauseEntity::class,
        ExerciseEntity::class,
        ExerciseNameEntity::class,
        ExerciseMuscleEntity::class,
        ProgramEntity::class,
        ActivityTypeEntity::class,
        RunStepEntity::class
    ],
    version = 19,
    // Each version's schema is written to app/schemas/ (see app/build.gradle.kts) for the migration tests
    exportSchema = true
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun sessionDao(): SessionDao
    abstract fun exerciseEntryDao(): ExerciseEntryDao
    abstract fun setDao(): SetDao
    abstract fun trackLogDao(): TrackLogDao
    abstract fun locationLogDao(): LocationLogDao
    abstract fun runDetailsDao(): RunDetailsDao
    abstract fun routeDao(): RouteDao
    abstract fun exerciseLibraryDao(): ExerciseLibraryDao
    abstract fun programDao(): ProgramDao
    abstract fun activityTypeDao(): ActivityTypeDao

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

        // The plan becomes editable: exercise order and soft removal.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercise_definitions ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE exercise_definitions SET sortOrder = rowid")
                database.execSQL("ALTER TABLE exercise_definitions ADD COLUMN isRemoved INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Run stats (target, laps, steps, climb, calories, weather) and each fix's accuracy and direction
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `run_details` (
                        `sessionId` INTEGER NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `targetValue` REAL,
                        `lapMeters` INTEGER NOT NULL,
                        `stepCounterAtStart` INTEGER,
                        `steps` INTEGER,
                        `elevationGainM` REAL,
                        `calories` INTEGER,
                        `weatherTempC` INTEGER,
                        `weatherCondition` TEXT,
                        PRIMARY KEY(`sessionId`)
                    )
                """.trimIndent())
                database.execSQL("ALTER TABLE location_log ADD COLUMN accuracy REAL")
                database.execSQL("ALTER TABLE location_log ADD COLUMN bearing REAL")
            }
        }

        // Routes to run, from past runs or GPX files
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `routes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `distanceMeters` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `route_points` (
                        `routeId` INTEGER NOT NULL,
                        `seq` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `elevationM` REAL,
                        PRIMARY KEY(`routeId`, `seq`)
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_route_points_routeId` ON `route_points` (`routeId`)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `run_pauses` (
                        `sessionId` INTEGER NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `endedAt` INTEGER,
                        PRIMARY KEY(`sessionId`, `startedAt`)
                    )
                """.trimIndent())
            }
        }

        // A run's steps as it went, so the run summary can show cadence over the run and by lap.
        // A table of its own, outside sync: a synced track is rebuilt from the server's copy.
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `run_steps` (`sessionId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "`steps` INTEGER NOT NULL, PRIMARY KEY(`sessionId`, `timestamp`))"
                )
            }
        }

        // A programme's deload load and sets, and how long its variant weeks last, set by the user.
        // The defaults are what the app did until now: 60% and 2 sets, variants every other week.
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `programs` ADD COLUMN `deloadLoadPercent` INTEGER NOT NULL DEFAULT 60")
                db.execSQL("ALTER TABLE `programs` ADD COLUMN `deloadSets` INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE `programs` ADD COLUMN `variantEveryWeeks` INTEGER NOT NULL DEFAULT 1")
            }
        }

        /** Every migration, oldest first. The app and the migration tests open the database with this one list. */
        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
            MIGRATION_8_9, MIGRATION_9_10, SyncMigration.MIGRATION_10_11, SyncMigration.MIGRATION_11_12,
            SyncMigration.MIGRATION_12_13, LibraryMigration.MIGRATION_13_14,
            SetsMigration.MIGRATION_14_15, ProgramMigration.MIGRATION_15_16, ActivityTypeMigration.MIGRATION_16_17,
            MIGRATION_17_18, MIGRATION_18_19
        )

        /**
         * What a database created fresh at the current version gets beyond Room's tables, which the
         * migrations give an upgraded one: sync's change log and triggers, and the built-in training
         * types. Room's onCreate.
         */
        fun installFresh(db: SupportSQLiteDatabase) {
            SyncMigration.installFresh(db)
            ActivityTypeMigration.builtinRows().forEach(db::execSQL)
        }
    }
}
