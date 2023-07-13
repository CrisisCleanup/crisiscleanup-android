package com.crisiscleanup.sync.workers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.hilt.work.HiltWorker
import androidx.tracing.traceAsync
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.sync.SyncResult
import com.crisiscleanup.core.data.WorksitesFullSyncer
import com.crisiscleanup.sync.R
import com.crisiscleanup.sync.initializers.SyncConstraints
import com.crisiscleanup.sync.initializers.SyncWorksitesFullNotificationId
import com.crisiscleanup.sync.initializers.channelNotificationManager
import com.crisiscleanup.sync.initializers.notificationBuilder
import com.crisiscleanup.sync.initializers.progress
import com.crisiscleanup.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private const val StopSyncingAction = "com.crisiscleanup.STOP_SYNCING"

@HiltWorker
class SyncWorksitesFullWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    worksitesFullSyncer: WorksitesFullSyncer,
    private val syncPuller: SyncPuller,
    private val syncLogger: SyncLogger,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope coroutineScope: CoroutineScope,
) : CoroutineWorker(appContext, workerParams) {
    private val isSyncing = AtomicBoolean(false)

    private val stopSyncingReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            syncPuller.stopSyncPullWorksitesFull()
        }
    }

    init {
        worksitesFullSyncer.fullPullStats
            .onEach {
                with(it) {
                    if (isSyncing.get() &&
                        totalCount > 0 &&
                        // Skip the final update or deal with the race condition from updating and
                        // canceling the notification in different scopes
                        pullCount < totalCount
                    ) {
                        val title = appContext.getString(R.string.syncing_text, it.incidentName)
                        val text = if (isApproximateTotal)
                            appContext.getString(
                                R.string.saved_cases_approximate_out_of,
                                pullCount,
                                totalCount,
                            )
                        else appContext.getString(
                            R.string.saved_cases_out_of,
                            pullCount,
                            totalCount,
                        )
                        val progress = pullCount / totalCount.toFloat()
                        val stopSyncIntent = PendingIntent.getBroadcast(
                            appContext,
                            0,
                            Intent(StopSyncingAction),
                            PendingIntent.FLAG_IMMUTABLE,
                        )
                        appContext.channelNotificationManager()?.notify(
                            SyncWorksitesFullNotificationId,
                            appContext.notificationBuilder(title, text)
                                .progress(progress)
                                .addAction(
                                    R.drawable.close,
                                    appContext.getString(R.string.stop_syncing),
                                    stopSyncIntent,
                                )
                                .build()
                        )
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    override suspend fun getForegroundInfo() = appContext.syncForegroundInfo(
        SyncWorksitesFullNotificationId,
        text = appContext.getString(R.string.sync_worksites_full_notification_text),
    )

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override suspend fun doWork() = withContext(ioDispatcher) {
        traceAsync("WorksitesFullSync", 0) {
            syncLogger.type = "background-sync-worksites-full"
            syncLogger.log("Worksites full sync start")

            try {
                appContext.registerReceiver(stopSyncingReceiver, IntentFilter(StopSyncingAction))

                isSyncing.set(true)

                val isSyncSuccess = awaitAll(
                    async {
                        val result = syncPuller.syncPullWorksitesFullAsync().await()
                        result is SyncResult.Success || result is SyncResult.NotAttempted
                    },
                ).all { it }

                syncLogger
                    .log("Worksites full sync end. success=$isSyncSuccess")
                    .flush()

                if (isSyncSuccess) Result.success()
                else Result.retry()
            } finally {
                isSyncing.set(false)
                appContext.unregisterReceiver(stopSyncingReceiver)
                appContext.channelNotificationManager()?.cancel(SyncWorksitesFullNotificationId)
            }
        }
    }

    companion object {
        fun oneTimeSyncWork(): OneTimeWorkRequest {
            val data = Data.Builder()
                .putAll(SyncWorksitesFullWorker::class.delegatedData())
                .build()

            // TODO Research why battery level causes this work type to fail
            return OneTimeWorkRequestBuilder<DelegatingWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(SyncConstraints)
                .setInputData(data)
                .build()
        }
    }
}