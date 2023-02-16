package com.crisiscleanup.sync

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.sync.model.SyncPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

object SyncPipeline {
    suspend fun determineSyncSteps(
        incidentsRepository: IncidentsRepository,
        worksitesRepository: WorksitesRepository,
        appPreferences: LocalAppPreferencesDataSource,
    ): SyncPlan {
        val stepsBuilder = SyncPlan.Builder()

        if (incidentsRepository.incidents.first().isEmpty()) {
            stepsBuilder.setPullIncidents()
        } else {
            val syncAttempt = appPreferences.userData.first().syncAttempt
            if (syncAttempt.shouldSyncPassively()) {
                stepsBuilder.setPullIncidents()
            }
        }

        val incidentId = appPreferences.userData.first().selectedIncidentId
        if (incidentId > 0) {
            incidentsRepository.getIncident(incidentId)?.let {
                val syncStats = worksitesRepository.getWorksitesSyncStats(incidentId)
                if (syncStats?.syncAttempt?.shouldSyncPassively() != false) {
                    stepsBuilder.setPullIncidentIdWorksites(incidentId)
                }
            }
        }

        return stepsBuilder.build()
    }

    // TODO Prevent multiple calls to this concurrently
    suspend fun performSync(
        plan: SyncPlan,
        incidentsRepository: IncidentsRepository,
        worksitesRepository: WorksitesRepository,
        resourceProvider: AndroidResourceProvider? = null,
        updateNotificationMessage: suspend CoroutineScope.(String) -> Unit = {},
    ): Boolean = coroutineScope {
        if (plan.pullIncidents) {
            incidentsRepository.pullIncidents()
        }

        ensureActive()

        plan.pullIncidentIdWorksites?.let { incidentId ->
            incidentsRepository.getIncident(incidentId)?.let { incident ->
                resourceProvider?.let {
                    val syncMessage =
                        resourceProvider.getString(R.string.syncing_incident_text, incident.name)
                    updateNotificationMessage(syncMessage)
                }

                worksitesRepository.refreshWorksites(incidentId, false)
            }
        }

        return@coroutineScope true
    }
}
