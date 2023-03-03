package com.crisiscleanup.sync

import com.crisiscleanup.core.common.Syncer
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.sync.SyncPipeline.determineSyncSteps
import com.crisiscleanup.sync.SyncPipeline.performSync
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync service to pull data while the app is in the foreground
 */
@Singleton
class AppSyncer @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val incidentsRepository: IncidentsRepository,
    private val worksitesRepository: WorksitesRepository,
    private val appPreferences: LocalAppPreferencesDataSource,
    private val authEventManager: AuthEventManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : Syncer {

    private val syncMutex = Mutex()
    private var syncJob: Job? = null

    private var incidentSyncJob: Job? = null

    private suspend fun isInvalidAccountToken(): Boolean {
        val accountData = accountDataRepository.accountData.first()
        if (accountData.isTokenInvalid) {
            authEventManager.onExpiredToken()
            return true
        }
        return false
    }

    private suspend fun sync(force: Boolean) = coroutineScope {
        val unforcedPlan = determineSyncSteps(
            incidentsRepository,
            worksitesRepository,
            appPreferences,
        )
        val plan = if (force) unforcedPlan.copy(pullIncidents = true)
        else unforcedPlan
        if (!plan.requiresSync) {
            return@coroutineScope
        }

        performSync(
            plan,
            incidentsRepository,
            worksitesRepository,
        )
    }

    override fun sync(force: Boolean, cancelOngoing: Boolean) {
        applicationScope.launch {
            if (isInvalidAccountToken()) {
                return@launch
            }

            syncMutex.withLock {
                if (!cancelOngoing && syncJob?.isActive == true) {
                    return@withLock
                }

                syncJob?.cancel()
                syncJob = applicationScope.launch(ioDispatcher) {
                    sync(force)
                }
            }
        }
    }

    override fun syncIncident(id: Long, force: Boolean) {
        if (id == EmptyIncident.id) {
            return
        }

        incidentSyncJob?.cancel()
        incidentSyncJob = applicationScope.launch(ioDispatcher) {
            if (isInvalidAccountToken()) {
                return@launch
            }

            incidentsRepository.pullIncident(id)
        }
    }
}