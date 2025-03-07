package com.crisiscleanup.sync.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.data.incidentcache.IncidentDataPullReporter
import com.crisiscleanup.core.data.model.IncidentPullDataType
import com.crisiscleanup.sync.R
import com.crisiscleanup.sync.initializers.SYNC_NOTIFICATION_ID
import com.crisiscleanup.sync.initializers.channelNotificationManager
import com.crisiscleanup.sync.initializers.notificationBuilder
import com.crisiscleanup.sync.initializers.progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

private const val STOP_SYNCING_ACTION = "com.crisiscleanup.STOP_SYNCING"

internal class IncidentDataSyncNotifier @Inject constructor(
    private val appContext: Context,
    incidentDataPullReporter: IncidentDataPullReporter,
    private val syncPuller: SyncPuller,
    private val logger: AppLogger,
    coroutineScope: CoroutineScope,
) {
    private val syncCounter = AtomicInteger(0)

    private val stopSyncingReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            syncPuller.stopPullWorksites()
        }
    }

    private val isSyncing: Boolean
        get() {
            synchronized(syncCounter) {
                return syncCounter.get() > 0
            }
        }

    init {
        incidentDataPullReporter.incidentDataPullStats
            .onEach {
                with(it) {
                    // Skip the final update or deal with the race condition from updating and
                    // canceling the notification in different scopes
                    if (isOngoing &&
                        isSyncing
                    ) {
                        val title = appContext.getString(R.string.syncing_text, incidentName)
                        val text = if (isIndeterminate) {
                            appContext.getString(
                                R.string.saving_indeterminate_data,
                            )
                        } else if (pullType == IncidentPullDataType.WorksitesCore) {
                            appContext.getString(
                                R.string.saved_cases_out_of,
                                savedCount,
                                dataCount,
                            )
                        } else {
                            appContext.getString(
                                R.string.saved_full_cases_out_of,
                                savedCount,
                                dataCount,
                            )
                        }
                        val stopSyncIntent = PendingIntent.getBroadcast(
                            appContext,
                            0,
                            Intent(STOP_SYNCING_ACTION),
                            PendingIntent.FLAG_IMMUTABLE,
                        )
                        appContext.channelNotificationManager()?.notify(
                            SYNC_NOTIFICATION_ID,
                            appContext.notificationBuilder(title, text)
                                .progress(progress)
                                .addAction(
                                    R.drawable.close,
                                    appContext.getString(R.string.stop_syncing),
                                    stopSyncIntent,
                                )
                                .setOnlyAlertOnce(true)
                                .build(),
                        )
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    suspend fun <T> notifySync(syncOperation: suspend () -> T): T {
        synchronized(syncCounter) {
            if (syncCounter.getAndIncrement() == 0) {
                appContext.registerReceiver(
                    stopSyncingReceiver,
                    IntentFilter(STOP_SYNCING_ACTION),
                )
            }

            // TODO Delete after hanging notification is solved
            logger.logDebug("Sync notification start ${syncCounter.get()}")
        }

        try {
            return syncOperation()
        } finally {
            synchronized(syncCounter) {
                if (syncCounter.decrementAndGet() == 0) {
                    appContext.unregisterReceiver(stopSyncingReceiver)

                    appContext.channelNotificationManager()?.cancel(SYNC_NOTIFICATION_ID)
                }
            }

            // TODO Delete after hanging notification is solved
            logger.logDebug("Sync notification out ${syncCounter.get()}")
        }
    }
}
