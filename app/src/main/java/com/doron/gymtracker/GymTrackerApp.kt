package com.doron.gymtracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.doron.gymtracker.media.RestTimerBeepPlayer
import com.doron.gymtracker.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GymTrackerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    // WorkManager builds the sync worker through Hilt (its default initializer is off in the manifest).
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        RestTimerBeepPlayer.init(this)
        SyncWorker.schedulePeriodic(this)
    }
}
