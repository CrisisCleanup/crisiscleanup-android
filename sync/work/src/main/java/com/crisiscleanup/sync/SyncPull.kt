package com.crisiscleanup.sync

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.sync.model.SyncPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

internal object SyncPull {
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
                if (syncStats?.shouldSync != false) {
                    stepsBuilder.setPullIncidentIdWorksites(incidentId)
                }
            }
        }

        return stepsBuilder.build()
    }

    suspend fun executePlan(
        plan: SyncPlan,
        incidentsRepository: IncidentsRepository,
        worksitesRepository: WorksitesRepository,
        syncLogger: SyncLogger,
        resourceProvider: AndroidResourceProvider? = null,
        updateNotificationMessage: suspend CoroutineScope.(String) -> Unit = {},
    ): Boolean = coroutineScope {
        if (plan.pullIncidents) {
            incidentsRepository.pullIncidents()

            syncLogger.log("Incidents pulled")
        }

        ensureActive()

        plan.pullIncidentIdWorksites?.let { incidentId ->
            incidentsRepository.getIncident(incidentId)?.let { incident ->
                resourceProvider?.let {
                    val syncMessage =
                        resourceProvider.getString(R.string.syncing_incident_text, incident.name)
                    updateNotificationMessage(syncMessage)
                }

                worksitesRepository.refreshWorksites(incidentId)

                syncLogger.log("Incident $incidentId worksites pulled")
            }
        }

        return@coroutineScope true
    }
}
