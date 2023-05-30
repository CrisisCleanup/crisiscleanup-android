package com.crisiscleanup.sync.initializers

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
import com.crisiscleanup.sync.workers.SyncMediaWorker
import com.crisiscleanup.sync.workers.SyncWorker
import com.crisiscleanup.sync.workers.SyncWorksitesFullWorker
import com.crisiscleanup.core.common.R as commonR

internal const val SyncNotificationId = 0
internal const val SyncMediaNotificationId = 0
internal const val SyncWorksitesFullNotificationId = 0
private const val SyncNotificationChannelID = "SyncNotificationChannel"

// These names should not be changed otherwise the app may have concurrent sync requests running
internal const val SyncWorkName = "SyncWorkName"
internal const val SyncMediaWorkName = "SyncMediaWorkName"
internal const val SyncWorksitesFullWorkName = "SyncWorksitesFullWorkName"

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

fun scheduleSyncMedia(context: Context) {
    WorkManager.getInstance(context).apply {
        enqueueUniqueWork(
            SyncMediaWorkName,
            ExistingWorkPolicy.KEEP,
            SyncMediaWorker.oneTimeSyncWork()
        )
    }
}

fun scheduleSyncWorksitesFull(context: Context) {
    WorkManager.getInstance(context).apply {
        enqueueUniqueWork(
            SyncWorksitesFullWorkName,
            ExistingWorkPolicy.REPLACE,
            SyncWorksitesFullWorker.oneTimeSyncWork()
        )
    }
}

internal val SyncConstraints
    get() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

internal val SyncMediaConstraints
    get() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED)
        .setRequiresBatteryNotLow(true)
        .build()

/**
 * Foreground information for sync on lower API levels when sync workers are being
 * run with a foreground service
 */
internal fun Context.syncForegroundInfo(
    id: Int = SyncNotificationId,
    title: String = "",
    text: String = "",
): ForegroundInfo {
    channelNotificationManager()

    val notification = notificationBuilder(title, text).build()
    return ForegroundInfo(id, notification)
}

internal fun Context.channelNotificationManager(): NotificationManager? {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.createNotificationChannel(syncNotificationChannel)
    return notificationManager
}

private val Context.syncNotificationChannel: NotificationChannel
    get() = NotificationChannel(
        SyncNotificationChannelID,
        getString(R.string.sync_notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = getString(R.string.sync_notification_channel_description)
    }

internal fun Context.notificationBuilder(
    title: String = "",
    text: String = "",
): NotificationCompat.Builder {
    val contentTitle = title.ifBlank { getString(R.string.sync_notification_title) }
    val contentText = text.ifBlank { getString(R.string.sync_notification_text) }
    return NotificationCompat.Builder(this, SyncNotificationChannelID)
        .setSmallIcon(commonR.drawable.ic_app_notification)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
}

internal fun NotificationCompat.Builder.progress(progress: Float): NotificationCompat.Builder {
    val iProgress = (progress * 1000).toInt().coerceIn(0, 1000)
    return setProgress(1000, iProgress, false)
}
