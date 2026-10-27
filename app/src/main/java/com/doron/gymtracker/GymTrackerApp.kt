package com.doron.gymtracker

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import com.doron.gymtracker.media.RestTimerBeepPlayer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GymTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RestTimerBeepPlayer.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LocaleManager::class.java)
            if (localeManager != null && localeManager.applicationLocales.isEmpty) {
                localeManager.applicationLocales = LocaleList.forLanguageTags("iw")
            }
        }
    }
}
