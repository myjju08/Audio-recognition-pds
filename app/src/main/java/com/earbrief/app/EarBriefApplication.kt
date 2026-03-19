package com.earbrief.app

import android.app.Application
import com.earbrief.app.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EarBriefApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }
}
