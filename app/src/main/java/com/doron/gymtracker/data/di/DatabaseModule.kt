package com.doron.gymtracker.data.di

import android.content.Context
import androidx.room.Room
import com.doron.gymtracker.data.db.GymDatabase
import com.doron.gymtracker.data.db.dao.ExerciseEntryDao
import com.doron.gymtracker.data.db.dao.SessionDao
import com.doron.gymtracker.data.db.dao.SetRecordDao
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
        Room.databaseBuilder(context, GymDatabase::class.java, "gym_tracker.db").build()

    @Provides fun provideWorkoutDao(db: GymDatabase): WorkoutDao = db.workoutDao()
    @Provides fun provideSessionDao(db: GymDatabase): SessionDao = db.sessionDao()
    @Provides fun provideExerciseEntryDao(db: GymDatabase): ExerciseEntryDao = db.exerciseEntryDao()
    @Provides fun provideSetRecordDao(db: GymDatabase): SetRecordDao = db.setRecordDao()
}
