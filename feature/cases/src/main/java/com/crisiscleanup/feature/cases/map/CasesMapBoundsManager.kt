package com.crisiscleanup.feature.cases.map

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.mapmarker.model.DefaultIncidentBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.mapmarker.util.smallOffset
import com.crisiscleanup.core.model.data.IncidentCoordinateBounds
import com.crisiscleanup.core.model.data.IncidentCoordinateBoundsNone
import com.crisiscleanup.feature.cases.model.asIncidentCoordinateBounds
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class CasesMapBoundsManager(
    private val incidentSelector: IncidentSelector,
    private val incidentBoundsProvider: IncidentBoundsProvider,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    private val coroutineScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val logger: AppLogger,
) {
    var isMapLoaded = MutableStateFlow(false)

    val centerCache: LatLng
        get() = mapBoundsCache.center

    private var _mapCameraBounds = MutableStateFlow(MapViewCameraBoundsDefault)
    val mapCameraBounds = _mapCameraBounds.asStateFlow()

    private val mappingBoundsIncidentIds = incidentBoundsProvider.mappingBoundsIncidentIds
    private val isMappingLocationBounds = combine(
        incidentSelector.incidentId,
        mappingBoundsIncidentIds,
    ) { id, ids ->
        ids.contains(id)
    }

    val isDeterminingBounds = isMappingLocationBounds

    private val incidentIdBounds = incidentSelector.incident.flatMapLatest { incident ->
        incidentBoundsProvider.mapIncidentBounds(incident)
    }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = DefaultIncidentBounds,
            started = SharingStarted.WhileSubscribed(),
        )

    private var savedMapBounds = IncidentCoordinateBoundsNone

    init {
        incidentIdBounds
            .onEach { incidentBounds ->
                if (incidentBounds.locations.isNotEmpty()) {
                    val bounds = incidentBounds.bounds
                    cacheBounds(bounds)
                    if (isMapLoaded.value) {
                        _mapCameraBounds.value = MapViewCameraBounds(bounds)
                    }
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(coroutineScope)

        coroutineScope.launch(ioDispatcher) {
            val incidentBounds = appPreferencesRepository.userPreferences.first().casesMapBounds
            val incidentId = incidentSelector.incidentId.first()
            if (incidentBounds.incidentId == incidentId) {
                savedMapBounds = incidentBounds
            }
        }
    }

    private var mapBoundsCache = MapViewCameraBoundsDefault.bounds

    fun cacheBounds(bounds: LatLngBounds) {
        val isLoaded = isMapLoaded.value

        mapBoundsCache = if (isLoaded || savedMapBounds == IncidentCoordinateBoundsNone) {
            bounds
        } else {
            savedMapBounds.latLngBounds
        }

        if (isLoaded) {
            val incidentId = incidentSelector.incidentId.value
            coroutineScope.launch(ioDispatcher) {
                appPreferencesRepository.setCasesMapBounds(
                    bounds.asIncidentCoordinateBounds(
                        incidentId,
                    ),
                )
            }
        }
    }

    /**
     * @return true if state is changing from not loaded->loaded or false if loaded signal was already received
     */
    fun onMapLoaded() = isMapLoaded.compareAndSet(expect = false, update = true)

    fun restoreBounds() {
        _mapCameraBounds.value = MapViewCameraBounds(mapBoundsCache, 0)
    }

    fun restoreIncidentBounds() {
        val incidentId = incidentSelector.incidentId.value
        incidentBoundsProvider.getIncidentBounds(incidentId)?.let {
            val bounds = it.bounds
            val ne = bounds.northeast.smallOffset(1e-9)
            val latLngBounds = LatLngBounds(bounds.southwest, ne)

            _mapCameraBounds.value = MapViewCameraBounds(latLngBounds)
        }
    }
}

val IncidentCoordinateBounds.latLngBounds: LatLngBounds
    get() = LatLngBounds(
        LatLng(south, west),
        LatLng(north, east),
    )
