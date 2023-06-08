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
import com.crisiscleanup.sync.initializers.SyncMediaConstraints
import com.crisiscleanup.sync.initializers.SyncMediaNotificationId
import com.crisiscleanup.sync.initializers.channelNotificationManager
import com.crisiscleanup.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

@HiltWorker
class SyncMediaWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPusher: SyncPusher,
    private val syncLogger: SyncLogger,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun getForegroundInfo() = appContext.syncForegroundInfo(
        SyncMediaNotificationId,
        text = appContext.getString(R.string.sync_media_notification_text),
    )

    override suspend fun doWork() = withContext(ioDispatcher) {
        traceAsync("MediaSync", 0) {
            syncLogger.type = "background-sync-media"
            syncLogger.log("Media sync start")

            val isSyncSuccess = awaitAll(
                async {
                    val result = syncPusher.syncPushMedia()
                    if (result !is SyncResult.Success) {
                        syncLogger.log("Result $result")
                    }
                    result !is SyncResult.Error
                },
            ).all { it }

            syncLogger
                .log("Media sync end. success=$isSyncSuccess")
                .flush()

            appContext.channelNotificationManager()?.cancel(SyncMediaNotificationId)

            if (isSyncSuccess) Result.success()
            else Result.retry()
        }
    }

    companion object {
        fun oneTimeSyncWork(): OneTimeWorkRequest {
            val data = Data.Builder()
                .putAll(SyncMediaWorker::class.delegatedData())
                .build()

            return OneTimeWorkRequestBuilder<DelegatingWorker>()
                .setConstraints(SyncMediaConstraints)
                .setInputData(data)
                .build()
        }
    }
}