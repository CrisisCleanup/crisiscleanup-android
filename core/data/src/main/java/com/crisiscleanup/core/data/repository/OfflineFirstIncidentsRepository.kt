package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.Synchronizer
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.model.incidentLocationCrossReferences
import com.crisiscleanup.core.data.model.locationsAsEntity
import com.crisiscleanup.core.data.util.NetworkMonitor
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.database.model.PopulatedIncident
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError.Companion.collapseMessages
import com.crisiscleanup.core.network.model.NetworkIncident
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstIncidentsRepository @Inject constructor(
    incidentDao: IncidentDao,
    private val incidentsNetworkDataSource: CrisisCleanupNetworkDataSource,
    private val incidentDaoPlus: IncidentDaoPlus,
    private val incidentSelector: IncidentSelector,
    private val worksitesRepository: WorksitesRepository,
    private val networkMonitor: NetworkMonitor,
    private val appLogger: AppLogger,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : IncidentsRepository {
    private var isSyncing = MutableStateFlow(false)

    override val isLoading: Flow<Boolean> = isSyncing

    override val incidents: Flow<List<Incident>> =
        incidentDao.getIncidents().map { it.map(PopulatedIncident::asExternalModel) }

    /**
     * Possibly syncs and caches incidents data
     */
    private suspend fun syncInternal(force: Boolean) {
        if (!force && incidents.first().isNotEmpty()) {
            return
        }

        isSyncing.value = true
        try {
            val networkIncidents = incidentsNetworkDataSource.getIncidents(
                listOf(
                    "id",
                    "start_at",
                    "name",
                    "short_name",
                    "locations",
                    "turn_on_release",
                    "active_phone_number",
                    "is_archived"
                )
            )

            if (networkIncidents.errors != null) {
                throw Exception(collapseMessages(networkIncidents.errors!!))
            }

            networkIncidents.results?.let { incidents ->
                incidentDaoPlus.saveIncidents(
                    incidents.map(NetworkIncident::asEntity),
                    incidents.map(NetworkIncident::locationsAsEntity).flatten(),
                    incidents.map(NetworkIncident::incidentLocationCrossReferences).flatten(),
                )
            }
        } finally {
            isSyncing.value = false
        }
    }

    override suspend fun sync(force: Boolean) = withContext(ioDispatcher) {
        if (!networkMonitor.isOnline.first()) {
            return@withContext
        }

        try {
            syncInternal(force)
            val incidentId = incidentSelector.incidentId.value
            worksitesRepository.refreshWorksites(incidentId, force)
        } catch (e: Exception) {
            appLogger.logException(e)
            if (force) {
                // TODO Emit error to interested listeners (for feedback)
            }
        }
    }

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        return try {
            syncInternal(false)
            // TODO Refresh incidents with the correct incident ID (or skip)
            true
        } catch (e: Exception) {
            appLogger.logException(e)
            false
        }
    }
}