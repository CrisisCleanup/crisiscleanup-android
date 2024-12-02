package com.crisiscleanup.core.appcomponent

import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.commonassets.ui.getDisasterIcon
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.AppPreferencesRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.domain.LoadSelectIncidents
import com.crisiscleanup.core.model.data.EmptyIncident
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.crisiscleanup.core.commonassets.R as commonAssetsR

class AppTopBarDataProvider(
    screenTitleKey: String,
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    incidentSelector: IncidentSelector,
    private val translator: KeyResourceTranslator,
    accountDataRepository: AccountDataRepository,
    appPreferencesRepository: AppPreferencesRepository,
    coroutineScope: CoroutineScope,
) {

    val loadSelectIncidents = LoadSelectIncidents(
        incidentsRepository = incidentsRepository,
        accountDataRepository = accountDataRepository,
        incidentSelector = incidentSelector,
        appPreferencesRepository = appPreferencesRepository,
        coroutineScope = coroutineScope,
    )
    val incidentsData = loadSelectIncidents.data

    val disasterIconResId = incidentSelector.incident.map { getDisasterIcon(it.disaster) }
        .stateIn(
            scope = coroutineScope,
            initialValue = commonAssetsR.drawable.ic_disaster_other,
            started = SharingStarted.WhileSubscribed(),
        )

    private val isSyncingWorksitesFull = combine(
        incidentSelector.incidentId,
        worksitesRepository.syncWorksitesFullIncidentId,
    ) { incidentId, syncingIncidentId ->
        incidentId != EmptyIncident.id &&
            incidentId == syncingIncidentId
    }

    val showHeaderLoading = combine(
        incidentsRepository.isLoading,
        worksitesRepository.isLoading,
        isSyncingWorksitesFull,
    ) { b0, b1, b2 -> b0 || b1 || b2 }

    val screenTitle = incidentSelector.incident
        .map { it.shortName.ifBlank { translator(screenTitleKey) } }
        .stateIn(
            scope = coroutineScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    val isAccountExpired = accountDataRepository.accountData
        .map { !it.areTokensValid }
        .stateIn(
            scope = coroutineScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val profilePictureUri = accountDataRepository.accountData
        .map { it.profilePictureUri }
        .stateIn(
            scope = coroutineScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )
}
