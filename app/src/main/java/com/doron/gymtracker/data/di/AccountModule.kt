package com.doron.gymtracker.data.di

import android.content.Context
import com.doron.gymtracker.BuildConfig
import com.doron.gymtracker.account.AuthRepository
import com.doron.gymtracker.account.BackendApi
import com.doron.gymtracker.account.EncryptedSessionStore
import com.doron.gymtracker.account.SessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** doron-backend: one API client and one sign-in state for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
object AccountModule {

    @Provides
    @Singleton
    fun backendApi(): BackendApi = BackendApi(BuildConfig.BACKEND_URL)

    @Provides
    @Singleton
    fun sessionStore(@ApplicationContext context: Context): SessionStore = EncryptedSessionStore(context)

    @Provides
    @Singleton
    fun authRepository(api: BackendApi, store: SessionStore): AuthRepository = AuthRepository(api, store)
}
