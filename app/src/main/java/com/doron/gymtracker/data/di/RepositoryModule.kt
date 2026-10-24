package com.doron.gymtracker.data.di

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.SessionRepositoryImpl
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.repository.WorkoutRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
