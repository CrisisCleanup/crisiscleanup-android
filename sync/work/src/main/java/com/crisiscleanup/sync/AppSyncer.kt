package com.crisiscleanup.sync

import com.crisiscleanup.core.common.Syncer
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.sync.SyncPipeline.determineSyncSteps
import com.crisiscleanup.sync.SyncPipeline.performSync
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    @ApplicationScope private val applicationScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : Syncer {

    private var syncJob: Job? = null
    override fun sync(force: Boolean) {
        syncJob?.cancel()
        syncJob = applicationScope.launch(ioDispatcher) {
            val accountData = accountDataRepository.accountData.first()
            if (accountData.isTokenInvalid) {
                return@launch
            }

            val unforcedPlan = determineSyncSteps(
                incidentsRepository,
                worksitesRepository,
                appPreferences,
            )
            val plan = if (force) unforcedPlan.copy(pullIncidents = true)
            else unforcedPlan
            if (!plan.requiresSync) {
                return@launch
            }

            performSync(
                plan,
                incidentsRepository,
                worksitesRepository,
            )
        }
    }
}