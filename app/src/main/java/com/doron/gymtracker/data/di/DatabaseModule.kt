package com.doron.gymtracker.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doron.gymtracker.data.db.GymDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GymDatabase =
        Room.databaseBuilder(context, GymDatabase::class.java, "gym_tracker.db")
            .addMigrations(*GymDatabase.MIGRATIONS)
            // A fresh install starts at the current version, so the migrations never run: install
            // what sync needs (change log, triggers) when Room creates the database.
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = GymDatabase.installFresh(db)
            })
            .build()

    @Provides fun provideWorkoutDao(db: GymDatabase): WorkoutDao = db.workoutDao()
    @Provides fun provideSessionDao(db: GymDatabase): SessionDao = db.sessionDao()
    @Provides fun provideExerciseEntryDao(db: GymDatabase): ExerciseEntryDao = db.exerciseEntryDao()
    @Provides fun provideSetDao(db: GymDatabase): SetDao = db.setDao()
    @Provides fun provideTrackLogDao(db: GymDatabase): TrackLogDao = db.trackLogDao()
    @Provides fun provideLocationLogDao(db: GymDatabase): LocationLogDao = db.locationLogDao()
    @Provides fun provideRunDetailsDao(db: GymDatabase): RunDetailsDao = db.runDetailsDao()
    @Provides fun provideRouteDao(db: GymDatabase): RouteDao = db.routeDao()
    @Provides fun provideExerciseLibraryDao(db: GymDatabase): ExerciseLibraryDao = db.exerciseLibraryDao()
    @Provides fun provideProgramDao(db: GymDatabase): ProgramDao = db.programDao()
}
