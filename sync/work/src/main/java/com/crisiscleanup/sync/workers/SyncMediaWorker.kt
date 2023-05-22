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
import com.crisiscleanup.sync.initializers.SyncMediaConstraints
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
    override suspend fun doWork() = withContext(ioDispatcher) {
        traceAsync("MediaSync", 0) {
            syncLogger.type = "background-sync-media"
            syncLogger.log("Media sync start")

            val isSyncSuccess = awaitAll(
                async {
                    syncPusher.syncPushMedia() !is SyncResult.Error
                },
            ).all { it }

            syncLogger
                .log("Sync end. success=$isSyncSuccess")
                .flush()

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