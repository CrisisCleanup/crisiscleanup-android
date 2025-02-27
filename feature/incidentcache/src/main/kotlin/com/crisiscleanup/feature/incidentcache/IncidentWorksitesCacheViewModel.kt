package com.crisiscleanup.feature.incidentcache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.common.subscribedReplay
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentCacheRepository
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.model.data.BOUNDED_REGION_RADIUS_MILES_DEFAULT
import com.crisiscleanup.core.model.data.BoundedRegionParameters
import com.crisiscleanup.core.model.data.InitialIncidentWorksitesCachePreferences
import com.crisiscleanup.core.model.data.boundedRegionParametersNone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class IncidentWorksitesCacheViewModel @Inject constructor(
    incidentSelector: IncidentSelector,
    private val incidentCacheRepository: IncidentCacheRepository,
    permissionManager: PermissionManager,
    locationProvider: LocationProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    appEnv: AppEnv,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Logger(CrisisCleanupLoggers.Sync) private val logger: AppLogger,
) : ViewModel() {
    val isNotProduction = appEnv.isNotProduction

    val incident = incidentSelector.incident

    val boundedRegionDataEditor: BoundedRegionDataEditor =
        IncidentCacheBoundedRegionDataEditor(
            permissionManager,
            locationProvider,
            drawableResourceBitmapProvider,
            viewModelScope,
            ioDispatcher,
        )

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

    val isUpdatingSyncMode = MutableStateFlow(false)
    val syncingParameters = incidentCacheRepository.cachePreferences
        .stateIn(
            scope = viewModelScope,
            initialValue = InitialIncidentWorksitesCachePreferences,
            started = subscribedReplay(),
        )

    private val hasUserChangedBoundedRegion = AtomicBoolean(false)
    val isUpdatingBoundedRegionParameters = MutableStateFlow(false)

    val editableRegionBoundedParameters = MutableStateFlow(boundedRegionParametersNone)

    init {
        syncingParameters
            .onEach {
                val syncingRegionParameters = it.boundedRegionParameters
                if (editableRegionBoundedParameters.compareAndSet(
                        boundedRegionParametersNone,
                        syncingRegionParameters,
                    )
                ) {
                    with(syncingRegionParameters) {
                        if (regionLatitude != 0.0 || regionLongitude != 0.0) {
                            boundedRegionDataEditor.setCoordinates(regionLatitude, regionLongitude)
                        }
                    }

                    if (syncingRegionParameters.isRegionMyLocation &&
                        !permissionManager.hasLocationPermission.value
                    ) {
                        updateBoundedRegionParameters { regionParameters ->
                            regionParameters.copy(isRegionMyLocation = false)
                        }
                    }
                }
            }
            .launchIn(viewModelScope)

        boundedRegionDataEditor.centerCoordinates
            .throttleLatest(300)
            .onEach { coordinates ->
                if (hasUserChangedBoundedRegion.get() || boundedRegionDataEditor.isUserActed) {
                    updateBoundedRegionParameters {
                        it.copy(
                            regionLatitude = coordinates.latitude,
                            regionLongitude = coordinates.longitude,
                        )
                    }
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(externalScope)

        editableRegionBoundedParameters
            .throttleLatest(300)
            .onEach {
                if (!hasUserChangedBoundedRegion.get() &&
                    !boundedRegionDataEditor.isUserActed
                ) {
                    return@onEach
                }

                if (!isUpdatingBoundedRegionParameters.compareAndSet(
                        expect = false,
                        update = true,
                    )
                ) {
                    return@onEach
                }

                val updatedParameters = syncingParameters.value.copy(
                    boundedRegionParameters = it,
                )
                try {
                    incidentCacheRepository.updateCachePreferences(updatedParameters)
                } finally {
                    isUpdatingBoundedRegionParameters.value = false
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(externalScope)
    }

    private fun updatePreferences(
        isPaused: Boolean,
        isRegionBounded: Boolean,
        isNearMe: Boolean = false,
        onPreferencesUpdated: () -> Unit = {},
    ) {
        if (!isUpdatingSyncMode.compareAndSet(expect = false, update = true)) {
            return
        }

        val parameters = syncingParameters.value
        val boundedRegionParameters = with(editableRegionBoundedParameters.value) {
            if (isRegionBounded && regionRadiusMiles <= 0) {
                copy(
                    regionRadiusMiles = BOUNDED_REGION_RADIUS_MILES_DEFAULT,
                )
            } else {
                this
            }
        }.copy(
            isRegionMyLocation = isNearMe,
        )
        val updatedParameters = parameters.copy(
            isPaused = isPaused,
            isRegionBounded = isRegionBounded,
            boundedRegionParameters,
        )

        viewModelScope.launch(ioDispatcher) {
            try {
                // TODO Atomic update
                incidentCacheRepository.updateCachePreferences(updatedParameters)
                editableRegionBoundedParameters.value = updatedParameters.boundedRegionParameters
                onPreferencesUpdated()
            } finally {
                isUpdatingSyncMode.value = false
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

    fun boundCachingCases(isNearMe: Boolean) {
        val isValidUpdate = if (isNearMe) {
            boundedRegionDataEditor.checkMyLocation()
        } else {
            true
        }

        if (isNearMe && !isValidUpdate) {
            // TODO Alert if requested permissions and permissions is granted quickly after
        }

        if (isValidUpdate) {
            updatePreferences(
                isPaused = false,
                isRegionBounded = true,
                isNearMe = isNearMe,
            ) {
                if (isNearMe) {
                    boundedRegionDataEditor.useMyLocation()
                }
            }
        }

        // TODO Restart caching if region parameters are defined
    }

    fun resetCaching() {
        viewModelScope.launch(ioDispatcher) {
            incidentCacheRepository.resetIncidentSyncStats(incident.value.id)
        }
    }

    private fun updateBoundedRegionParameters(op: (parameters: BoundedRegionParameters) -> BoundedRegionParameters) {
        editableRegionBoundedParameters.value = op(editableRegionBoundedParameters.value)
    }

    fun setBoundedRegionRadius(radius: Float) {
        hasUserChangedBoundedRegion.set(true)
        updateBoundedRegionParameters { it.copy(regionRadiusMiles = radius.toDouble()) }
    }
}
