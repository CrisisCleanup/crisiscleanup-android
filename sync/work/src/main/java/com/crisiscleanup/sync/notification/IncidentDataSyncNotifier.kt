package com.crisiscleanup.sync.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.crisiscleanup.core.common.KeyResourceTranslator
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
    private val translator: KeyResourceTranslator,
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
                        val title = translator.translate("sync.syncing_incident_name", 0)
                            .replace("{incident_name}", incidentName)
                        val text = notificationMessage.ifBlank {
                            var message = if (isIndeterminate) {
                                translator.translate("sync.saving_data", 0)
                            } else if (pullType == IncidentPullDataType.WorksitesCore) {
                                translator.translate(
                                    "sync.saved_case_count_of_total_count",
                                    0,
                                )
                                    .replace("{case_count}", "$savedCount")
                                    .replace("{total_case_count}", "$dataCount")
                            } else if (pullType == IncidentPullDataType.WorksitesAdditional) {
                                translator.translate(
                                    "sync.saved_case_count_of_total_count_offline",
                                    0,
                                )
                                    .replace("{case_count}", "$savedCount")
                                    .replace("{total_case_count}", "$dataCount")
                            } else {
                                translator.translate("sync.saving_more_data", 0)
                            }
                            if (currentStep in 1..stepTotal) {
                                message = translator.translate(
                                    "({current_step}/{total_step_count}) {message}",
                                    0,
                                )
                                    .replace("{current_step}", "$currentStep")
                                    .replace("{total_step_count}", "$stepTotal")
                                    .replace("{message}", message)
                            }
                            message
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
                                    translator.translate("sync.stop_syncing", 0),
                                    stopSyncIntent,
                                )
                                .setOnlyAlertOnce(true)
                                .build(),
                        )
                    } else if (isEnded) {
                        appContext.channelNotificationManager()?.cancel(SYNC_NOTIFICATION_ID)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    suspend fun <T> notifySync(syncOperation: suspend () -> T): T {
        synchronized(syncCounter) {
            if (syncCounter.getAndIncrement() == 0) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    appContext.registerReceiver(
                        stopSyncingReceiver,
                        IntentFilter(STOP_SYNCING_ACTION),
                    )
                } else {
                    appContext.registerReceiver(
                        stopSyncingReceiver,
                        IntentFilter(STOP_SYNCING_ACTION),
                        RECEIVER_NOT_EXPORTED,
                    )
                }
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
