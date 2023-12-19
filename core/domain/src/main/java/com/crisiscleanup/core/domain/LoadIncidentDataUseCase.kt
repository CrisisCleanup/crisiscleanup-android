package com.crisiscleanup.core.domain

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadIncidentDataUseCase @Inject constructor(
    incidentsRepository: IncidentsRepository,
    private val incidentSelector: IncidentSelector,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    @ApplicationScope coroutineScope: CoroutineScope,
) {
    private val data = incidentsRepository.incidents.map { incidents ->
        var selectedId = incidentSelector.incidentId.first()
        if (selectedId == EmptyIncident.id) {
            selectedId = appPreferencesRepository.userPreferences.first().selectedIncidentId
        }

        // Update incident data or select first if current incident (ID) not found
        var incident = incidents.find { it.id == selectedId } ?: EmptyIncident
        if (incident == EmptyIncident && incidents.isNotEmpty()) {
            incident = incidents[0]
            appPreferencesRepository.setSelectedIncident(incident.id)
        }

        incidentSelector.setIncident(incident)

        if (incident.id == EmptyIncident.id) {
            IncidentsData.Empty
        } else {
            IncidentsData.Incidents(incidents)
        }
    }.shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(1_000),
        replay = 1,
    )

    operator fun invoke() = data
}

sealed interface IncidentsData {
    data object Loading : IncidentsData

    data class Incidents(
        val incidents: List<Incident>,
    ) : IncidentsData

    data object Empty : IncidentsData
}

@Module
@InstallIn(SingletonComponent::class)
object LoadIncidentsDataModule {
    @Provides
    @Singleton
    fun providesLoadIncidentsData(
        incidentsRepository: IncidentsRepository,
        incidentSelector: IncidentSelector,
        appPreferencesRepository: LocalAppPreferencesRepository,
        @ApplicationScope coroutineScope: CoroutineScope,
    ) = LoadIncidentDataUseCase(
        incidentsRepository,
        incidentSelector,
        appPreferencesRepository,
        coroutineScope,
    )
}
