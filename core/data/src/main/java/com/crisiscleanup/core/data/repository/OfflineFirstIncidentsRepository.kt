package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.data.Synchronizer
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.model.incidentLocationCrossReferences
import com.crisiscleanup.core.data.model.locationsAsEntity
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.database.model.PopulatedIncident
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkIncident
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstIncidentsRepository @Inject constructor(
    incidentDao: IncidentDao,
    private val incidentsNetworkDataSource: CrisisCleanupNetworkDataSource,
    private val incidentDaoPlus: IncidentDaoPlus,
    private val appLogger: AppLogger,
) : IncidentsRepository {

    override val incidents: Flow<List<Incident>> =
        incidentDao.getIncidents().map { it.map(PopulatedIncident::asExternalModel) }

    var isSyncing: MutableStateFlow<Boolean> = MutableStateFlow(false)
        private set

    override suspend fun sync(force: Boolean) {
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
            if (networkIncidents.count > 0) {
                val incidents = networkIncidents.results
                incidentDaoPlus.saveIncidents(
                    incidents.map(NetworkIncident::asEntity),
                    incidents.map(NetworkIncident::locationsAsEntity).flatten(),
                    incidents.map(NetworkIncident::incidentLocationCrossReferences).flatten(),
                )
            }

            // TODO Likely a good idea to catch error here? Since foreground app also calls this.

        } finally {
            isSyncing.value = false
        }
    }

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        return try {
            sync(true)
            true
        } catch (e: Exception) {
            appLogger.logException(e)
            false
        }
    }
}