package com.crisiscleanup.sync

import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.sync.model.SyncPlan
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

internal object SyncPull {
    suspend fun determineSyncSteps(
        incidentsRepository: IncidentsRepository,
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
        incidentsRepository.getIncident(incidentId)?.let {
            stepsBuilder.setPullIncidentIdWorksites(incidentId)
        }

        return stepsBuilder.build()
    }

    suspend fun executePlan(
        plan: SyncPlan,
        accountDataRefresher: AccountDataRefresher,
        incidentsRepository: IncidentsRepository,
        worksitesRepository: WorksitesRepository,
        syncLogger: SyncLogger,
    ) = coroutineScope {
        if (plan.pullIncidents) {
            accountDataRefresher.updateApprovedIncidents(true)
            incidentsRepository.pullIncidents()

            syncLogger.log("Incidents pulled")
        }

        ensureActive()

        plan.pullIncidentIdWorksites?.let { incidentId ->
            worksitesRepository.refreshWorksites(incidentId)

            syncLogger.log("Incident $incidentId worksites pulled")
        }

        return@coroutineScope true
    }
}
