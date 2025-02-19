package com.crisiscleanup.feature.incidentcache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.common.subscribedReplay
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentCacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentWorksitesCacheViewModel @Inject constructor(
    incidentSelector: IncidentSelector,
    private val incidentCacheRepository: IncidentCacheRepository,
    appEnv: AppEnv,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val isNotProduction = appEnv.isNotProduction

    val incident = incidentSelector.incident

    val isSyncing = incidentCacheRepository.isSyncingActiveIncident
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = subscribedReplay(),
        )

    val lastSynced = incident
        .flatMapLatest { incident ->
            incidentCacheRepository.streamSyncStats(incident.id)
                .mapNotNull {
                    it?.lastUpdated?.relativeTime
                }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = subscribedReplay(),
        )

    val syncingParameters = incidentCacheRepository.cachePreferences
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = subscribedReplay(3),
        )

    private fun updatePreferences(
        isPaused: Boolean,
        isRegionBounded: Boolean,
    ) {
        syncingParameters.value?.let {
            val preferences = it.copy(
                isPaused = isPaused,
                isRegionBounded = isRegionBounded,
            )
            viewModelScope.launch(ioDispatcher) {
                incidentCacheRepository.updateCachePreferences(preferences)
            }
        }
    }

    fun resumeCachingCases() {
        updatePreferences(isPaused = false, isRegionBounded = false)

        // TODO Restart caching
    }

    fun pauseCachingCases() {
        updatePreferences(isPaused = true, isRegionBounded = false)

        // TODO Cancel ongoing
    }

    fun boundCachingCases() {
        // TODO Region parameters
        updatePreferences(isPaused = false, isRegionBounded = true)

        // TODO Restart caching if region parameters are defined
    }

    fun resetCaching() {
        viewModelScope.launch(ioDispatcher) {
            incidentCacheRepository.resetIncidentSyncStats(incident.value.id)
        }
    }
}
