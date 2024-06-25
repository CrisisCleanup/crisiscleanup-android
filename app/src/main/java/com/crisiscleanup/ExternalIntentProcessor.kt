package com.crisiscleanup

import android.content.Intent
import android.net.Uri
import com.crisiscleanup.core.common.event.ExternalEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.queryParamMap
import javax.inject.Inject

class ExternalIntentProcessor @Inject constructor(
    private val externalEventBus: ExternalEventBus,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) {
    fun processMainIntent(intent: Intent): Boolean {
        when (val action = intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { intentUri ->
                    intentUri.path?.let { urlPath ->
                        return processMainIntent(intentUri, urlPath)
                    }
                }
            }

            else -> {
                logger.logDebug("Main intent action not handled $action")
            }
        }

        return false
    }

    private fun processMainIntent(url: Uri, urlPath: String): Boolean {
        if (urlPath.startsWith("/l/")) {
            val code = urlPath.replace("/l/", "")
            if (code.isNotBlank()) {
                externalEventBus.onEmailLoginLink(code)
            }
        } else if (urlPath.startsWith("/password/reset/")) {
            val code = urlPath.replace("/password/reset/", "")
            if (code.isNotBlank()) {
                externalEventBus.onResetPassword(code)
            }
        } else if (urlPath.startsWith("/invitation_token/")) {
            val code = urlPath.replace("/invitation_token/", "")
            if (code.isNotBlank()) {
                externalEventBus.onOrgUserInvite(code)
            }
        } else if (urlPath.startsWith("/mobile_app_user_invite")) {
            externalEventBus.onOrgPersistentInvite(url.queryParamMap)
        } else {
            return false
        }
        return true
    }
}
