package com.crisiscleanup.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.DatabaseVersionProvider
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.commonassets.R
import com.crisiscleanup.core.commonassets.getDisasterIcon
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupAccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.SyncLogRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.domain.LoadSelectIncidents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    val incidentSelector: IncidentSelector,
    private val translator: KeyResourceTranslator,
    syncLogRepository: SyncLogRepository,
    private val accountDataRepository: AccountDataRepository,
    private val accountDataRefresher: AccountDataRefresher,
    private val appVersionProvider: AppVersionProvider,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    private val appEnv: AppEnv,
    private val syncPuller: SyncPuller,
    private val databaseVersionProvider: DatabaseVersionProvider,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val isDebuggable = appEnv.isDebuggable
    val isNotProduction = appEnv.isNotProduction

    val loadSelectIncidents = LoadSelectIncidents(
        incidentsRepository = incidentsRepository,
        incidentSelector = incidentSelector,
        appPreferencesRepository = appPreferencesRepository,
        coroutineScope = viewModelScope,
    )
    val incidentsData = loadSelectIncidents.data

    val disasterIconResId = incidentSelector.incident.map { getDisasterIcon(it.disaster) }
        .stateIn(
            scope = viewModelScope,
            initialValue = R.drawable.ic_disaster_other,
            started = SharingStarted.WhileSubscribed(),
        )

    private val isSyncingWorksitesFull = combine(
        incidentSelector.incidentId,
        worksitesRepository.syncWorksitesFullIncidentId,
    ) { incidentId, syncingIncidentId -> incidentId == syncingIncidentId }

    val showHeaderLoading = combine(
        incidentsRepository.isLoading,
        worksitesRepository.isLoading,
        isSyncingWorksitesFull,
    ) { b0, b1, b2 -> b0 || b1 || b2 }

    val screenTitle = incidentSelector.incident
        .map { it.shortName.ifBlank { translator("nav.menu") } }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    val isAccountExpired = accountDataRepository.accountData
        .map { !it.areTokensValid }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val profilePictureUri = accountDataRepository.accountData
        .map { it.profilePictureUri }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    val versionText: String
        get() {
            val version = appVersionProvider.version
            return "${version.second} (${version.first}) ${appEnv.apiEnvironment} Android"
        }

    val databaseVersionText: String
        get() = if (isNotProduction) "DB ${databaseVersionProvider.databaseVersion}" else ""

    val isSharingAnalytics = appPreferencesRepository.userPreferences.map {
        it.allowAllAnalytics
    }

    init {
        externalScope.launch(ioDispatcher) {
            syncLogRepository.trimOldLogs()

            accountDataRefresher.updateProfilePicture()
        }
    }

    fun shareAnalytics(share: Boolean) {
        viewModelScope.launch {
            appPreferencesRepository.setAnalytics(share)
        }
    }

    fun simulateTokenExpired() {
        if (isDebuggable) {
            (accountDataRepository as CrisisCleanupAccountDataRepository).expireAccessToken()
        }
    }

    fun clearRefreshToken() {
        if (isDebuggable) {
            externalScope.launch {
                accountDataRepository.clearAccountTokens()
            }
        }
    }

    fun syncWorksitesFull() {
        if (isDebuggable) {
            syncPuller.scheduleSyncWorksitesFull()
        }
    }
}
