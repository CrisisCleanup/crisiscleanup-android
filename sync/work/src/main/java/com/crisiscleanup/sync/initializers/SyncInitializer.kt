package com.crisiscleanup.sync.initializers

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer

object Sync {
    // This method is initializes sync, the process that keeps the app's data current.
    // It is called from the app module's Application.onCreate() and should be only done once.
    fun initialize(context: Context) {
        scheduleSync(context)
        scheduleSyncWorksites(context)
        // scheduleSyncMedia is run from MainActivityViewModel
        scheduleInactiveCheckup(context)
    }
}
