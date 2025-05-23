package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.incidentcache.IncidentOrganizationsSyncer
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.model.asEntitySource
import com.crisiscleanup.core.data.model.incidentLocationCrossReferences
import com.crisiscleanup.core.data.model.locationsAsEntity
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.database.dao.IncidentOrganizationDao
import com.crisiscleanup.core.database.dao.LocationDaoPlus
import com.crisiscleanup.core.database.dao.fts.getMatchingIncidents
import com.crisiscleanup.core.database.model.PopulatedIncident
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.INCIDENT_ORGANIZATIONS_STABLE_MODEL_BUILD_VERSION
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentIdNameType
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkIncident
import com.crisiscleanup.core.network.model.NetworkIncidentLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

@Singleton
class OfflineFirstIncidentsRepository @Inject constructor(
    private val incidentDao: IncidentDao,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val incidentDaoPlus: IncidentDaoPlus,
    private val locationDaoPlus: LocationDaoPlus,
    private val incidentOrganizationDao: IncidentOrganizationDao,
    private val incidentOrganizationsSyncer: IncidentOrganizationsSyncer,
    private val appPreferences: LocalAppPreferencesDataSource,
    @Logger(CrisisCleanupLoggers.Incidents) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : IncidentsRepository {
    private var isSyncing = MutableStateFlow(false)

    private val incidentsQueryFields = listOf(
        "id",
        "start_at",
        "name",
        "short_name",
        "case_label",
        "incident_type",
        "locations",
        "turn_on_release",
        "active_phone_number",
        "is_archived",
    )
    private val fullIncidentQueryFields: List<String> =
        incidentsQueryFields.toMutableList().also { it.add("form_fields") }

    override val isLoading: Flow<Boolean> = isSyncing

    override val isFirstLoad = combine(
        isSyncing,
        incidentDao.streamIncidentCount(),
        ::Pair,
    )
        .mapLatest { (syncing, count) ->
            syncing && count == 0L
        }

    override val incidentCount: Long
        get() = incidentDao.getIncidentCount()

    override val incidents: Flow<List<Incident>> =
        incidentDao.streamIncidents().mapLatest { it.map(PopulatedIncident::asExternalModel) }

    override val hotlineIncidents = incidents.mapLatest {
        it.filter { incident ->
            incident.activePhoneNumbers.isNotEmpty()
        }
    }

    override suspend fun getIncident(id: Long, loadFormFields: Boolean) =
        withContext(ioDispatcher) {
            if (loadFormFields) {
                incidentDao.getFormFieldsIncident(id)?.asExternalModel()
            } else {
                incidentDao.getIncident(id)?.asExternalModel()
            }
        }

    override suspend fun getIncidents(startAt: Instant) = withContext(ioDispatcher) {
        incidentDao.getIncidents(startAt.toEpochMilliseconds())
            .map(PopulatedIncident::asExternalModel)
    }

    override suspend fun getIncidentsList(): List<IncidentIdNameType> {
        try {
            return networkDataSource.getIncidentsList()
                .map {
                    IncidentIdNameType(
                        it.id,
                        it.name,
                        it.shortName,
                        disasterLiteral = it.type,
                    )
                }
        } catch (e: Exception) {
            logger.logException(e)
        }
        return emptyList()
    }

    override fun streamIncident(id: Long) =
        incidentDao.streamFormFieldsIncident(id).mapLatest { it?.asExternalModel() }

    private suspend fun saveLocations(incidents: Collection<NetworkIncident>) {
        val locationIds = incidents.flatMap { it.locations.map(NetworkIncidentLocation::location) }
        val locations = networkDataSource.getIncidentLocations(locationIds)
        locationDaoPlus.saveLocations(locations.asEntitySource())
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
            val pullAll = if (forcePullAll) {
                true
            } else {
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
            val networkIncidents = networkDataSource.getIncidents(queryFields, after = pullAfter)
            if (networkIncidents.isNotEmpty()) {
                saveIncidentsPrimaryData(networkIncidents)

                // TODO Use configurable threshold
                val recentIncidents = networkIncidents.filter { it.startAt > recentTimestamp }
                saveIncidentsSecondaryData(recentIncidents)

                if (networkIncidents.mapNotNull(NetworkIncident::fields).isEmpty()) {
                    val ordered =
                        networkIncidents.sortedWith { a, b -> if (a.startAt > b.startAt) -1 else 1 }
                    ordered.subList(0, ordered.size.coerceAtMost(3))
                        .forEach { incident ->
                            networkDataSource.getIncident(incident.id, fullIncidentQueryFields)
                                ?.let { saveFormFields(listOf(it)) }
                        }
                }
            }
        } finally {
            isSyncing.value = false
        }
    }

    override suspend fun pullIncidents(force: Boolean) = coroutineScope {
        var isSuccessful = false
        try {
            syncInternal(force)
            isSuccessful = true
        } finally {
            // Treat coroutine cancellation as unsuccessful for now
            appPreferences.setSyncAttempt(isSuccessful)
        }
    }

    override suspend fun pullHotlineIncidents() {
        try {
            val hotlineIncidents = networkDataSource
                .getIncidentsNoAuth(
                    incidentsQueryFields,
                    after = Clock.System.now() - 120.days,
                )
                .filter { it.activePhoneNumber?.isNotEmpty() == true }
            if (hotlineIncidents.isNotEmpty()) {
                saveIncidentsPrimaryData(hotlineIncidents)
            }

            val recentActiveIncidents = hotlineIncidents.map(NetworkIncident::id).toSet()
            val localActiveIncidents = incidentDao.getActiveIncidentIds()
                .filter { !recentActiveIncidents.contains(it) }
            for (incidentId in localActiveIncidents) {
                pullIncident(incidentId)
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override suspend fun pullIncident(id: Long) {
        val networkIncident = networkDataSource.getIncident(id, fullIncidentQueryFields)
        networkIncident?.let { incident ->
            val incidents = listOf(incident)
            saveIncidentsPrimaryData(incidents)
            saveIncidentsSecondaryData(incidents)
        }
    }

    override suspend fun pullIncidentOrganizations(incidentId: Long, force: Boolean) {
        if (!force) {
            incidentOrganizationDao.getSyncStats(incidentId)?.let {
                if (it.targetCount > 0 &&
                    it.successfulSync?.let { date -> Clock.System.now() - date < 7.days } == true &&
                    it.appBuildVersionCode >= INCIDENT_ORGANIZATIONS_STABLE_MODEL_BUILD_VERSION
                ) {
                    return
                }
            }
        }

        incidentOrganizationsSyncer.sync(incidentId)
    }

    override suspend fun getMatchingIncidents(q: String) = incidentDaoPlus.getMatchingIncidents(q)
}
