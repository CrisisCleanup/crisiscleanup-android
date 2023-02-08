package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.Synchronizer
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.model.incidentLocationCrossReferences
import com.crisiscleanup.core.data.model.locationsAsEntity
import com.crisiscleanup.core.data.util.NetworkMonitor
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.database.dao.LocationDaoPlus
import com.crisiscleanup.core.database.dao.LocationEntitySource
import com.crisiscleanup.core.database.model.PopulatedIncident
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError.Companion.tryGetException
import com.crisiscleanup.core.network.model.NetworkIncident
import com.crisiscleanup.core.network.model.NetworkIncidentLocation
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
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val incidentDaoPlus: IncidentDaoPlus,
    private val locationDaoPlus: LocationDaoPlus,
    private val incidentSelector: IncidentSelector,
    private val worksitesRepository: WorksitesRepository,
    private val networkMonitor: NetworkMonitor,
    private val appLogger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : IncidentsRepository {
    private var isSyncing = MutableStateFlow(false)

    override val isLoading: Flow<Boolean> = isSyncing

    override val incidents: Flow<List<Incident>> =
        incidentDao.getIncidents().map { it.map(PopulatedIncident::asExternalModel) }

    private suspend fun saveLocations(incidents: List<NetworkIncident>) {
        val locationIds = incidents.flatMap { it.locations.map(NetworkIncidentLocation::location) }
        val networkLocations = networkDataSource.getIncidentLocations(locationIds)

        networkLocations.errors?.let {
            tryGetException(it)?.let { exception -> throw exception }
        }

        networkLocations.results?.let { locations ->
            val sourceLocations = locations.map {
                val multiCoordinates = it.geom?.condensedCoordinates
                val coordinates = it.poly?.condensedCoordinates ?: it.point?.coordinates
                LocationEntitySource(
                    id = it.id,
                    shapeType = it.shapeType,
                    coordinates = if (multiCoordinates == null) coordinates else null,
                    multiCoordinates = multiCoordinates,
                )
            }
            locationDaoPlus.saveLocations(sourceLocations)
        }
    }

    /**
     * Possibly syncs and caches incidents data
     */
    private suspend fun syncInternal(force: Boolean) {
        if (!force && incidents.first().isNotEmpty()) {
            return
        }

        isSyncing.value = true
        try {
            val networkIncidents = networkDataSource.getIncidents(
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

            networkIncidents.errors?.let {
                tryGetException(it)?.let { exception -> throw exception }
            }

            networkIncidents.results?.let { incidents ->
                incidentDaoPlus.saveIncidents(
                    incidents.map(NetworkIncident::asEntity),
                    incidents.map(NetworkIncident::locationsAsEntity).flatten(),
                    incidents.map(NetworkIncident::incidentLocationCrossReferences).flatten(),
                )

                saveLocations(incidents)
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