package com.crisiscleanup.feature.cases

import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.model.data.IncidentLocation
import com.crisiscleanup.feature.cases.model.MapViewCameraBounds
import com.crisiscleanup.feature.cases.model.MapViewCameraBoundsDefault
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class CasesMapBoundsManager constructor(
    coroutineScope: CoroutineScope,
    incidentSelector: IncidentSelector,
    private val locationsRepository: LocationsRepository,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    private var _mapCameraBounds = MutableStateFlow(MapViewCameraBoundsDefault)
    var mapCameraBounds = _mapCameraBounds.asStateFlow()

    private val isMappingLocationBounds = MutableStateFlow(false)
    private val isUpdatingCameraBounds = MutableStateFlow(false)

    val isDeterminingBounds = combine(
        isMappingLocationBounds,
        isUpdatingCameraBounds
    ) { isMapping,
        isUpdating ->
        isMapping || isUpdating
    }

    private val incidentLatLngBoundary: StateFlow<List<LatLng>> =
        incidentSelector.incident.flatMapLatest { incident ->
            isMappingLocationBounds.value = true
            try {
                val locationIds = incident.locations.map(IncidentLocation::location)
                locationsRepository.streamLocations(locationIds)
                    .map { locations ->
                        // Assumes coordinates and multiCoordinates are lng-lat ordered pairs
                        val coordinates = locations.mapNotNull {
                            it.multiCoordinates?.flatten() ?: it.coordinates
                        }.flatten()
                        val latLngs = mutableListOf<LatLng>()
                        for (i in 1 until coordinates.size step 2) {
                            latLngs.add(LatLng(coordinates[i], coordinates[i - 1]))
                        }
                        latLngs
                    }
                    .flowOn(ioDispatcher)
            } finally {
                isMappingLocationBounds.value = false
            }
        }.stateIn(
            scope = coroutineScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        incidentLatLngBoundary
            .onEach { latLngs ->
                var bounds = MapViewCameraBoundsDefault.bounds

                isUpdatingCameraBounds.value = true
                try {
                    if (latLngs.isNotEmpty()) {
                        val locationBounds =
                            latLngs.fold(LatLngBounds.builder()) { acc, latLng ->
                                acc.include(latLng)
                            }

                        // Bounds must have error or build throws
                        if (bounds.southwest.latitude == bounds.northeast.latitude ||
                            bounds.southwest.longitude == bounds.northeast.longitude
                        ) {
                            locationBounds.include(
                                LatLng(
                                    bounds.southwest.latitude + 0.02,
                                    bounds.southwest.longitude + 0.02,
                                )
                            )
                        }

                        bounds = locationBounds.build()
                    }

                    _mapCameraBounds.value = MapViewCameraBounds(bounds)
                } finally {
                    isUpdatingCameraBounds.value = false
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(coroutineScope)
    }

    private var mapBoundsCache = MapViewCameraBoundsDefault.bounds

    fun cacheBounds(bounds: LatLngBounds) {
        mapBoundsCache = bounds
    }

    fun restoreBounds() {
        _mapCameraBounds.value = MapViewCameraBounds(mapBoundsCache, 0)
    }
}