package com.doron.gymtracker

import android.app.Application
import com.doron.gymtracker.media.RestTimerBeepPlayer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GymTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RestTimerBeepPlayer.init(this)
    }
}
