package com.crisiscleanup.core.testerfeedback

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.crisiscleanup.core.common.*
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.testerfeedbackapi.FeedbackReceiver
import com.crisiscleanup.core.testerfeedbackapi.FeedbackTrigger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject

// Modified https://github.com/firebase/firebase-android-sdk/blob/master/firebase-appdistribution/test-app/src/main/kotlin/com/googletest/firebase/appdistribution/testapp/ScreenshotDetectionFeedbackTrigger.kt
class ScreenshotFeedbackTrigger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager,
    private val feedbackReceiver: FeedbackReceiver,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : ContentObserver(Handler(Looper.getMainLooper())),
    DefaultLifecycleObserver,
    FeedbackTrigger {
    private val contentProjection =
        arrayOf(MediaStore.Images.Media.DATA, MediaStore.MediaColumns.IS_PENDING)

    private val seenImages = HashSet<Uri>()

    private val externalContentRegex =
        "${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}/\\d+".toRegex()

    override fun onStart(owner: LifecycleOwner) {
        context.contentResolver?.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            this,
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        context.contentResolver?.unregisterContentObserver(this)
    }

    private fun isExternalContent(uri: Uri) = uri.toString().matches(externalContentRegex)

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        uri?.let {
            if (!isExternalContent(it) || seenImages.contains(it)) {
                return
            }

            maybeStartFeedbackForScreenshot(uri)
        }
    }

    private fun maybeStartFeedbackForScreenshot(uri: Uri) {
        val permissionStatus = permissionManager.requestScreenshotReadPermission()
        if (permissionStatus != PermissionStatus.Granted) {
            // Will need to take consecutive screenshots.
            // No time to implement full flow for all statuses.
            return
        }

        try {
            context.contentResolver
                ?.query(uri, contentProjection, null, null, null)
                ?.let { cursor ->
                    cursor.use {
                        if (cursor.moveToFirst()) {
                            seenImages.add(uri)
                            val path =
                                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                            if (path.lowercase(Locale.getDefault()).contains("screenshot")) {
                                val payload = Bundle(1)
                                payload.putString("screenshot-uri", uri.toString())
                                feedbackReceiver.onStartFeedback("screenshot", payload)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }
}
