package com.crisiscleanup

import android.app.Application
import com.crisiscleanup.sync.initializers.Sync
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CrisisCleanupApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Sync.initialize(this)
    }
}
