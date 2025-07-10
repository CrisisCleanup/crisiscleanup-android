package com.crisiscleanup.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.tracing.traceAsync
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.common.sync.SyncResult
import com.crisiscleanup.sync.initializers.SYNC_WORKSITES_NOTIFICATION_ID
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
internal class SyncWorksitesWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPusher: SyncPusher,
    private val translator: KeyResourceTranslator,
    private val syncLogger: SyncLogger,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun getForegroundInfo() = appContext.syncForegroundInfo(
        SYNC_WORKSITES_NOTIFICATION_ID,
        text = translator.translate("sync.syncing_cases", 0),
    )

    override suspend fun doWork() = withContext(ioDispatcher) {
        traceAsync("WorksitesSync", 0) {
            syncLogger.type = "background-sync-worksites"
            syncLogger.log("Worksites push start")

            val isSyncSuccess = awaitAll(
                async {
                    val result = syncPusher.syncPushWorksites()
                    val isSuccess = result is SyncResult.Success

                    if (isSuccess) {
                        syncPusher.scheduleSyncMedia()
                    } else {
                        syncLogger.log("Sync worksites result $result")
                        if (result is SyncResult.InvalidAccountTokens) {
                            // TODO Notify invalid tokens is preventing sync
                        }
                    }

                    isSuccess
                },
            ).all { it }

            syncLogger
                .log("Worksites push end. success=$isSyncSuccess")
                .flush()

            appContext.channelNotificationManager()?.cancel(SYNC_WORKSITES_NOTIFICATION_ID)

            if (isSyncSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        fun oneTimeSyncWork() = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setConstraints(SyncConstraints)
            .setInputData(SyncWorksitesWorker::class.delegatedData())
            .build()
    }
}
