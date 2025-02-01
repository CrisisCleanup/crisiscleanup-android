package com.crisiscleanup.feature.incidentcache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentCacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class IncidentWorksitesCacheViewModel @Inject constructor(
    incidentSelector: IncidentSelector,
    private val incidentCacheRepository: IncidentCacheRepository,
    appEnv: AppEnv,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val isNotProduction = appEnv.isNotProduction

    val incident = incidentSelector.incident

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
            started = SharingStarted.WhileSubscribed(3.seconds.inWholeMilliseconds),
        )

    fun resetCaching() {
        viewModelScope.launch(ioDispatcher) {
            incidentCacheRepository.resetIncidentSyncStats(incident.value.id)
        }
    }
}
