package com.crisiscleanup.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.tracing.traceAsync
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.sync.SyncResult
import com.crisiscleanup.sync.initializers.SYNC_NOTIFICATION_ID
import com.crisiscleanup.sync.initializers.SyncConstraints
import com.crisiscleanup.sync.initializers.channelNotificationManager
import com.crisiscleanup.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

@HiltWorker
internal class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPuller: SyncPuller,
    private val syncLogger: SyncLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun getForegroundInfo() = appContext.syncForegroundInfo()

    override suspend fun doWork() = withContext(ioDispatcher) {
        traceAsync("Sync", 0) {
            syncLogger.type = "background-sync"
            syncLogger.log("Sync start")

            val isSyncSuccess = awaitAll(
                async {
                    // TODO Observe progress and update notification
                    syncPuller.syncPullAsync().await() !is SyncResult.Error
                },
                async {
                    syncPuller.syncPullLanguage() !is SyncResult.Error
                },
                async {
                    syncPuller.syncPullStatuses() !is SyncResult.Error
                },
            ).all { it }

            syncLogger
                .log("Sync end. success=$isSyncSuccess")
                .flush()

            appContext.channelNotificationManager()?.cancel(SYNC_NOTIFICATION_ID)

            if (isSyncSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        fun oneTimeSyncWork() = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .setInputData(SyncWorker::class.delegatedData())
            .build()
    }
}
