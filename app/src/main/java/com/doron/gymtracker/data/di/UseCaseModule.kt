package com.doron.gymtracker.data.di

import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.prefs.ProgramPrefs
import com.doron.gymtracker.data.prefs.RotationPrefs
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.usecase.CloseStaleSessionsUseCase
import com.doron.gymtracker.domain.usecase.FinishSessionUseCase
import com.doron.gymtracker.domain.usecase.GetNextWorkoutUseCase
import com.doron.gymtracker.domain.usecase.GetWorkoutPreviewUseCase
import com.doron.gymtracker.domain.usecase.LogSetUseCase
import com.doron.gymtracker.domain.usecase.GetLastWeightUseCase
import com.doron.gymtracker.domain.usecase.IsDeloadWeekUseCase
import com.doron.gymtracker.domain.usecase.ProgressionHintUseCase
import com.doron.gymtracker.domain.usecase.ResolveExerciseVariantUseCase
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
    fun provideCloseStaleSessionsUseCase(repo: SessionRepository) =
        CloseStaleSessionsUseCase(repo)

    @Provides
    fun provideStartSessionUseCase(repo: SessionRepository) =
        StartSessionUseCase(repo)

    @Provides
    fun provideLogSetUseCase(repo: SessionRepository) =
        LogSetUseCase(repo)

    @Provides
    fun provideResolveExerciseVariantUseCase(prefs: ProgramPrefs) =
        ResolveExerciseVariantUseCase(prefs)

    @Provides
    fun provideIsDeloadWeekUseCase(prefs: ProgramPrefs) =
        IsDeloadWeekUseCase(prefs)

    @Provides
    fun provideProgressionHintUseCase(repo: SessionRepository) =
        ProgressionHintUseCase(repo)

    @Provides
    fun provideGetLastWeightUseCase(repo: SessionRepository) =
        GetLastWeightUseCase(repo)

    @Provides
    fun provideGetWorkoutPreviewUseCase(
        repo: SessionRepository,
        getLastWeight: GetLastWeightUseCase,
        resolveVariant: ResolveExerciseVariantUseCase
    ) = GetWorkoutPreviewUseCase(repo, getLastWeight, resolveVariant)

    @Provides
    fun provideFinishSessionUseCase(
        sessionRepo: SessionRepository,
        workoutRepo: WorkoutRepository,
        prefs: RotationPrefs,
        healthConnect: HealthConnectManager
    ) = FinishSessionUseCase(sessionRepo, workoutRepo, prefs, healthConnect)
}
