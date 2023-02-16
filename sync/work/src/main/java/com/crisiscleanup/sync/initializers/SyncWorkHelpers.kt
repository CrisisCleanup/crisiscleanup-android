package com.crisiscleanup.sync.initializers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.crisiscleanup.sync.R
import com.crisiscleanup.sync.workers.SyncWorker
import com.crisiscleanup.core.common.R as commonR

internal const val SyncNotificationId = 0
private const val SyncNotificationChannelID = "SyncNotificationChannel"

// This name should not be changed otherwise the app may have concurrent sync requests running
internal const val SyncWorkName = "SyncWorkName"

fun scheduleSync(context: Context) {
    WorkManager.getInstance(context).apply {
        // Run sync and ensure only one sync worker runs at any time
        enqueueUniqueWork(
            SyncWorkName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            SyncWorker.oneTimeSyncWork()
        )
    }
}

// All sync work needs an internet connection
val SyncConstraints
    get() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

/**
 * Foreground information for sync on lower API levels when sync workers are being
 * run with a foreground service
 */
fun Context.syncForegroundInfo(text: String = "") = ForegroundInfo(
    SyncNotificationId,
    syncWorkNotification(text)
)

/**
 * Notification displayed on lower API levels when sync workers are being
 * run with a foreground service
 */
internal fun Context.syncWorkNotification(contextText: String = ""): Notification {
    val channel = NotificationChannel(
        SyncNotificationChannelID,
        getString(R.string.sync_notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = getString(R.string.sync_notification_channel_description)
    }
    // Register the channel with the system
    val notificationManager: NotificationManager? =
        getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.createNotificationChannel(channel)

    val notificationText = contextText.ifEmpty { getString(R.string.sync_notification_text) }

    return NotificationCompat.Builder(
        this,
        SyncNotificationChannelID
    )
        .setSmallIcon(
            commonR.drawable.ic_app_notification
        )
        .setContentTitle(getString(R.string.sync_notification_title))
        .setContentText(notificationText)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        // TODO Color? Is it possible from here? To use MaterialTheme? Or inject?
        .build()
}
