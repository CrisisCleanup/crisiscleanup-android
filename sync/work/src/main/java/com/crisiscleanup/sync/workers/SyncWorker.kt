package com.crisiscleanup.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.tracing.traceAsync
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.Synchronizer
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.sync.SyncPipeline.determineSyncSteps
import com.crisiscleanup.sync.initializers.SyncConstraints
import com.crisiscleanup.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Syncs the data layer by delegating to the appropriate repository instances with
 * sync functionality.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val incidentsRepository: IncidentsRepository,
    private val worksitesRepository: WorksitesRepository,
    private val accountDataRepository: AccountDataRepository,
    private val appPreferences: LocalAppPreferencesDataSource,
    private val resourceProvider: AndroidResourceProvider,
) : CoroutineWorker(appContext, workerParams), Synchronizer {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.syncForegroundInfo()

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        val accountData = accountDataRepository.accountData.first()
        if (accountData.isTokenInvalid) {
            // Downstream work should wait for valid access token before commencing
            return@withContext Result.failure()
        }

        val plan = determineSyncSteps(incidentsRepository, worksitesRepository, appPreferences)
        if (!plan.requiresSync) {
            return@withContext Result.success()
        }

        traceAsync("Sync", 0) {
            val syncedSuccessfully = awaitAll(
                async {
                    // TODO Consolidate all syncing so only a single process is run at a time
//                    performSync(
//                        plan,
//                        incidentsRepository,
//                        worksitesRepository,
//                        resourceProvider,
//                    ) { text -> setForeground(appContext.syncForegroundInfo(text)) }
                    true
                }
            ).all { it }

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
