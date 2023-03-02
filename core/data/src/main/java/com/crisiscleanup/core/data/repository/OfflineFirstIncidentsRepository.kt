package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
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
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError.Companion.tryThrowException
import com.crisiscleanup.core.network.model.NetworkIncident
import com.crisiscleanup.core.network.model.NetworkIncidentLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

@Singleton
class OfflineFirstIncidentsRepository @Inject constructor(
    private val incidentDao: IncidentDao,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val incidentDaoPlus: IncidentDaoPlus,
    private val locationDaoPlus: LocationDaoPlus,
    private val appPreferences: LocalAppPreferencesDataSource,
    private val authEventManager: AuthEventManager,
    private val networkMonitor: NetworkMonitor,
    @Logger(CrisisCleanupLoggers.Incidents) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : IncidentsRepository {
    private var isSyncing = MutableStateFlow(false)

    private var pullSingleIncidentId = MutableStateFlow(EmptyIncident.id)
    private val _pullSingleIncidentId = AtomicLong(0)

    private val incidentsQueryFields = listOf(
        "id",
        "start_at",
        "name",
        "short_name",
        "incident_type",
        "locations",
        "turn_on_release",
        "active_phone_number",
        "is_archived",
    )
    private val fullIncidentQueryFields: List<String> =
        incidentsQueryFields.toMutableList().also { it.add("form_fields") }

    private suspend fun isNotOnline() = networkMonitor.isNotOnline.first()

    override val isLoading: Flow<Boolean> = isSyncing

    override val incidents: Flow<List<Incident>> =
        incidentDao.streamIncidents().map { it.map(PopulatedIncident::asExternalModel) }

    override suspend fun getIncident(id: Long): Incident? =
        withContext(ioDispatcher) { incidentDao.getIncident(id).firstOrNull()?.asExternalModel() }

    private suspend fun saveLocations(incidents: Collection<NetworkIncident>) {
        val locationIds = incidents.flatMap { it.locations.map(NetworkIncidentLocation::location) }
        val networkLocations = networkDataSource.getIncidentLocations(locationIds)
        tryThrowException(authEventManager, networkLocations.errors)

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

    private suspend fun saveIncidentsPrimaryData(incidents: Collection<NetworkIncident>) {
        incidentDaoPlus.saveIncidents(
            incidents.map(NetworkIncident::asEntity),
            incidents.map(NetworkIncident::locationsAsEntity).flatten(),
            incidents.map(NetworkIncident::incidentLocationCrossReferences).flatten(),
        )
    }

    private suspend fun saveFormFields(incidents: Collection<NetworkIncident>) {
        val incidentsFields = incidents
            .filter { it.fields?.isNotEmpty() == true }
            .map { incident ->
                val fields = incident.fields?.map { field -> field.asEntity(incident.id) }
                    ?: emptyList()
                Pair(incident.id, fields)
            }
        incidentDaoPlus.updateFormFields(incidentsFields)
    }

    private suspend fun saveIncidentsSecondaryData(incidents: Collection<NetworkIncident>) {
        saveLocations(incidents)
        saveFormFields(incidents)
    }

    /**
     * Possibly syncs and caches incidents data
     */
    private suspend fun syncInternal(forcePullAll: Boolean = false) = coroutineScope {
        isSyncing.value = true
        try {
            val pullAll = if (forcePullAll) true else {
                val localIncidentsCount = incidentDao.getIncidentCount()
                localIncidentsCount < 10
            }
            val queryFields: List<String>
            val pullAfter: Instant?
            val recentTimestamp = Clock.System.now() - 180.days
            if (pullAll) {
                queryFields = incidentsQueryFields
                pullAfter = null
            } else {
                queryFields = fullIncidentQueryFields
                pullAfter = recentTimestamp
            }
            val networkIncidents =
                networkDataSource.getIncidents(queryFields, after = pullAfter)
            tryThrowException(authEventManager, networkIncidents.errors)

            networkIncidents.results?.let { incidents ->
                saveIncidentsPrimaryData(incidents)

                // TODO Use configurable threshold
                val recentIncidents = incidents.filter { it.startAt > recentTimestamp }
                saveIncidentsSecondaryData(recentIncidents)

                if (incidents.mapNotNull(NetworkIncident::fields).isEmpty()) {
                    val ordered =
                        incidents.sortedWith { a, b -> if (a.startAt > b.startAt) -1 else 1 }
                    ordered.subList(0, ordered.size.coerceAtMost(3))
                        .forEach { incident ->
                            val networkIncident =
                                networkDataSource.getIncident(incident.id, fullIncidentQueryFields)
                            tryThrowException(authEventManager, networkIncident.errors)

                            networkIncident.incident?.let { saveFormFields(listOf(it)) }
                        }
                }
            }
        } finally {
            isSyncing.value = false
        }
    }

    override suspend fun pullIncidents() = coroutineScope {
        var isSuccessful = false
        try {
            syncInternal()
            isSuccessful = true
        } finally {
            // Treat coroutine cancellation as unsuccessful for now
            appPreferences.setSyncAttempt(isSuccessful)
        }
    }

    override suspend fun pullIncident(id: Long) {
        if (isNotOnline()) {
            return
        }

        synchronized(_pullSingleIncidentId) {
            _pullSingleIncidentId.set(id)
            pullSingleIncidentId.value = id
        }
        try {
            val networkIncident = networkDataSource.getIncident(id, fullIncidentQueryFields)
            // TODO Expired token still needs testing
            tryThrowException(authEventManager, networkIncident.errors)

            networkIncident.incident?.let { incident ->
                val incidents = listOf(incident)
                saveIncidentsPrimaryData(incidents)
                saveIncidentsSecondaryData(incidents)
            }
        } finally {
            synchronized(_pullSingleIncidentId) {
                if (_pullSingleIncidentId.compareAndSet(id, 0)) {
                    pullSingleIncidentId.value = EmptyIncident.id
                }
            }
        }
    }
}