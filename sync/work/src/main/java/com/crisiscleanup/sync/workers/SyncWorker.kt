package com.crisiscleanup.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.tracing.traceAsync
import androidx.work.*
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.sync.SyncResult
import com.crisiscleanup.core.data.Synchronizer
import com.crisiscleanup.sync.initializers.SyncConstraints
import com.crisiscleanup.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPuller: SyncPuller,
    private val syncLogger: SyncLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams), Synchronizer {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.syncForegroundInfo()

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        traceAsync("Sync", 0) {
            syncLogger.log("Sync start")

            val syncedSuccessfully = awaitAll(
                async {
                    // TODO Observe progress and update notification
                    // text -> setForeground(appContext.syncForegroundInfo(text)) }
                    syncPuller.syncPullAsync().await() !is SyncResult.Error
                },
                async {
                    syncPuller.syncPullLanguageAsync().await() !is SyncResult.Error
                },
                async {
                    syncPuller.syncPullStatusesAsync().await() !is SyncResult.Error
                },
            ).all { it }

            syncLogger
                .log("Sync end. success=$syncedSuccessfully")
                .flush()

            // TODO Notification seems to hang around.
            //      Research if needs to manually clear.
            //      Nia doesn't need to clear notification...

            if (syncedSuccessfully) Result.success()
            else Result.retry()
        }
    }

    companion object {
        fun oneTimeSyncWork(): OneTimeWorkRequest {
            val data = Data.Builder()
                .putAll(SyncWorker::class.delegatedData())
                .build()

            return OneTimeWorkRequestBuilder<DelegatingWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(SyncConstraints)
                .setInputData(data)
                .build()
        }
    }
}
