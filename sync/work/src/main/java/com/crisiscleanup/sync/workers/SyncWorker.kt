package com.crisiscleanup.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.tracing.traceAsync
import androidx.work.*
import com.crisiscleanup.core.common.SyncPuller
import com.crisiscleanup.core.common.SyncResult
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.Synchronizer
import com.crisiscleanup.core.data.repository.SyncLogRepository
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
    private val syncLogger: SyncLogRepository,
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
                    val result = syncPuller.syncPullAsync().await()
                    result !is SyncResult.Error
                },
                async {
                    syncPuller.syncPullLanguage()
                    true
                }
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
