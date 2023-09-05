package com.crisiscleanup

import android.content.Intent
import android.net.Uri
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import javax.inject.Inject

class ExternalIntentProcessor @Inject constructor(
    private val authEventBus: AuthEventBus,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) {
    fun processMainIntent(intent: Intent) {
        when (val action = intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { intentUri ->
                    intentUri.path?.let { urlPath ->
                        processMainIntent(intentUri, urlPath)
                    }
                }
            }

            else -> {
                logger.logDebug("Main intent action not handled $action")
            }
        }
    }

    private fun processMainIntent(url: Uri, urlPath: String) {
        if (urlPath.startsWith("/o/callback")) {
            url.getQueryParameter("code")?.let { code ->
                authEventBus.onEmailLoginLink(code)
            }
        } else if (urlPath.startsWith("/password/reset/")) {
            val code = urlPath.replace("/password/reset/", "")
            if (code.isNotBlank()) {
                authEventBus.onResetPassword(code)
            }
        }
    }
}
