package com.doron.gymtracker.data.di

import com.doron.gymtracker.data.prefs.RotationPrefs
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.usecase.FinishSessionUseCase
import com.doron.gymtracker.domain.usecase.GetNextWorkoutUseCase
import com.doron.gymtracker.domain.usecase.LogSetUseCase
import com.doron.gymtracker.domain.usecase.StartSessionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideGetNextWorkoutUseCase(prefs: RotationPrefs, repo: WorkoutRepository) =
        GetNextWorkoutUseCase(prefs, repo)

    @Provides
    fun provideStartSessionUseCase(repo: SessionRepository) =
        StartSessionUseCase(repo)

    @Provides
    fun provideLogSetUseCase(repo: SessionRepository) =
        LogSetUseCase(repo)

    @Provides
    fun provideFinishSessionUseCase(
        sessionRepo: SessionRepository,
        workoutRepo: WorkoutRepository,
        prefs: RotationPrefs
    ) = FinishSessionUseCase(sessionRepo, workoutRepo, prefs)
}
