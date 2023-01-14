package com.crisiscleanup.log

import android.util.Log
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.log.AppLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppLogger @Inject constructor(
    private val appEnv: AppEnv
) : AppLogger {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun logException(e: Exception) {
        crashlytics.recordException(e)

        if (appEnv.isDebuggable) {
            Log.e("logException", e.message, e)
        }
    }
}