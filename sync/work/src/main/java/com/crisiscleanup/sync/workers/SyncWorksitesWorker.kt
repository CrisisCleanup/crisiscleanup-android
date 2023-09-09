package com.crisiscleanup.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.tracing.traceAsync
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.common.sync.SyncResult
import com.crisiscleanup.sync.R
import com.crisiscleanup.sync.initializers.SyncConstraints
import com.crisiscleanup.sync.initializers.SyncWorksitesNotificationId
import com.crisiscleanup.sync.initializers.channelNotificationManager
import com.crisiscleanup.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorksitesWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPusher: SyncPusher,
    private val syncLogger: SyncLogger,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun getForegroundInfo() = appContext.syncForegroundInfo(
        SyncWorksitesNotificationId,
        text = appContext.getString(R.string.sync_cases_notification_text),
    )

    override suspend fun doWork() = withContext(ioDispatcher) {
        traceAsync("WorksitesSync", 0) {
            syncLogger.type = "background-sync-worksites"
            syncLogger.log("Worksites sync start")

            val isSyncSuccess = awaitAll(
                async {
                    val result = syncPusher.syncPushWorksites()
                    val isSuccess = result !is SyncResult.Error
                    if (isSuccess) {
                        syncPusher.scheduleSyncMedia()
                    }
                    isSuccess
                },
            ).all { it }

            syncLogger
                .log("Worksites sync end. success=$isSyncSuccess")
                .flush()

            appContext.channelNotificationManager()?.cancel(SyncWorksitesNotificationId)

            if (isSyncSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        fun oneTimeSyncWork(): OneTimeWorkRequest {
            val data = Data.Builder()
                .putAll(SyncWorksitesWorker::class.delegatedData())
                .build()

            return OneTimeWorkRequestBuilder<DelegatingWorker>()
                .setConstraints(SyncConstraints)
                .setInputData(data)
                .build()
        }
    }
}