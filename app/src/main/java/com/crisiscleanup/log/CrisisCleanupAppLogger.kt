package com.crisiscleanup.log

import android.util.Log
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.log.TagLogger
import com.crisiscleanup.core.network.model.ExpiredTokenException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class CrisisCleanupAppLogger @Inject constructor(
    appEnv: AppEnv,
) : TagLogger {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override var tag: String? = null

    private val isDebuggable = appEnv.isDebuggable

    override fun logDebug(vararg logs: Any) {
        if (isDebuggable) {
            Log.d(tag, logs.joinToString(" "))
        }
    }

    override fun logException(e: Exception) {
        if (e is CancellationException ||
            e is ExpiredTokenException
        ) {
            return
        }

        crashlytics.recordException(e)

        if (isDebuggable) {
            Log.e(tag, e.message, e)
        }
    }

    override fun logCapture(message: String) {
        if (isDebuggable) {
            Log.i(tag, message)
        } else {
            crashlytics.log(message)
        }
    }

    override fun setAccountId(id: String) {
        crashlytics.setUserId(id)
    }
}
