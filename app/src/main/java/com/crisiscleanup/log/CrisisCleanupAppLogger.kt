package com.crisiscleanup.log

import android.util.Log
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.log.TagLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

class CrisisCleanupAppLogger @Inject constructor(
    private val appEnv: AppEnv,
) : TagLogger {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override var tag: String? = null

    override fun logDebug(vararg logs: Any) {
        if (appEnv.isDebuggable) {
            Log.d(tag, logs.joinToString(" "))
        }
    }

    override fun logException(e: Exception) {
        crashlytics.recordException(e)

        if (appEnv.isDebuggable) {
            Log.e(tag, e.message, e)
        }
    }
}