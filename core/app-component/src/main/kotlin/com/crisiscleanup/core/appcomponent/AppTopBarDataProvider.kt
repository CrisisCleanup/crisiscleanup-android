package com.crisiscleanup.core.appcomponent

import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.commonassets.ui.getDisasterIcon
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AppPreferencesRepository
import com.crisiscleanup.core.data.repository.IncidentCacheRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.domain.LoadSelectIncidents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.crisiscleanup.core.commonassets.R as commonAssetsR

class AppTopBarDataProvider(
    screenTitleKey: String,
    incidentsRepository: IncidentsRepository,
    incidentCacheRepository: IncidentCacheRepository,
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

    val showHeaderLoading = incidentCacheRepository.isSyncingActiveIncident

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
