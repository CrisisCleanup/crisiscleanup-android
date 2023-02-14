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
import com.crisiscleanup.sync.R
import com.crisiscleanup.sync.initializers.SyncConstraints
import com.crisiscleanup.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

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

    private val isForced: Boolean
        get() = inputData.getBoolean(KEY_FORCE_SYNC, false)

    private suspend fun trySync(): Boolean {
        if (!isForced) {
            if (incidentsRepository.incidents.first().isEmpty()) {
                return true
            }

            val incidentId = appPreferences.userData.first().selectedIncidentId
            if (incidentId <= 0) {
                val syncAttempt = appPreferences.userData.first().syncAttempt
                return !(syncAttempt.isRecent() || syncAttempt.isBackingOff())
            }

            val syncStats = worksitesRepository.getWorksitesSyncStats(incidentId)
            syncStats?.let {
                val syncAttempt = it.syncAttempt
                return !(syncAttempt.isRecent() || syncAttempt.isBackingOff())
            }
        }

        return true
    }

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        // Skip sync if not necessary
        if (!trySync()) {
            return@withContext Result.success()
        }

        val accountData = accountDataRepository.accountData.first()
        if (accountData.accessToken.isEmpty() ||
            accountData.tokenExpiry < Clock.System.now()
        ) {
            // Downstream work should wait for valid access token before commencing
            return@withContext Result.failure()
        }

        traceAsync("Sync", 0) {
            val syncedSuccessfully = awaitAll(
                async {
                    if (incidentsRepository.sync()) {
                        val selectedIncidentId = appPreferences.userData.first().selectedIncidentId

                        incidentsRepository.getIncident(selectedIncidentId)?.let {
                            val syncMessage =
                                resourceProvider.getString(R.string.syncing_incident_text, it.name)
                            setForeground(appContext.syncForegroundInfo(syncMessage))

                            worksitesRepository.refreshWorksites(selectedIncidentId, isForced)
                        }
                        true
                    } else {
                        false
                    }
                },
            ).all { it }

            appPreferences.setSyncAttempt(syncedSuccessfully)

            if (syncedSuccessfully) Result.success()
            else Result.retry()
        }
    }

    companion object {
        private const val KEY_FORCE_SYNC = "force-sync"

        fun oneTimeSyncWork(force: Boolean = false): OneTimeWorkRequest {
            val data = Data.Builder()
                .putAll(SyncWorker::class.delegatedData())
                .putBoolean(KEY_FORCE_SYNC, force)
                .build()

            return OneTimeWorkRequestBuilder<DelegatingWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(SyncConstraints)
                .setInputData(data)
                .build()
        }
    }
}
