package com.crisiscleanup.feature.cases.map

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentLocation
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

internal class CasesMapBoundsManager constructor(
    coroutineScope: CoroutineScope,
    private val incidentSelector: IncidentSelector,
    private val locationsRepository: LocationsRepository,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val logger: AppLogger,
) {
    var isMapLoaded = false
        private set

    val centerCache: LatLng
        get() = mapBoundsCache.center

    private var _mapCameraBounds = MutableStateFlow(MapViewCameraBoundsDefault)
    val mapCameraBounds = _mapCameraBounds.asStateFlow()

    private val isMappingLocationBounds = MutableStateFlow(false)
    private val isUpdatingCameraBounds = MutableStateFlow(false)

    val isDeterminingBounds = combine(
        isMappingLocationBounds,
        isUpdatingCameraBounds
    ) { isMapping,
        isUpdating ->
        isMapping || isUpdating
    }

    private val incidentLatLngBoundary: StateFlow<Pair<Long, List<LatLng>>> =
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
                        Pair(incident.id, latLngs)
                    }
                    .flowOn(ioDispatcher)
            } finally {
                isMappingLocationBounds.value = false
            }
        }.stateIn(
            scope = coroutineScope,
            initialValue = Pair(EmptyIncident.id, emptyList()),
            started = SharingStarted.WhileSubscribed(),
        )

    private val incidentBoundsCache = mutableMapOf<Long, LatLngBounds>()

    init {
        incidentLatLngBoundary
            .onEach { (incidentId, latLngs) ->
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
                        incidentBoundsCache[incidentId] = bounds
                        if (isMapLoaded) {
                            _mapCameraBounds.value = MapViewCameraBounds(bounds)
                        }
                        cacheBounds(bounds)
                    }
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

    // TODO Call when it becomes possible to determine newly created maps
    //      There doesn't seem to be an API for such with Compose maps
    fun onNewMap() {
        isMapLoaded = false
    }

    /**
     * @return true if state is changing from not loaded->loaded or false if loaded signal was already received
     */
    fun onMapLoaded(): Boolean {
        if (isMapLoaded) {
            return false
        }
        isMapLoaded = true

        return true
    }

    fun restoreBounds() {
        _mapCameraBounds.value = MapViewCameraBounds(mapBoundsCache, 0)
    }

    fun restoreIncidentBounds() {
        val incidentId = incidentSelector.incidentId.value
        incidentBoundsCache[incidentId]?.let {
            // TODO Force rebounding in a less hacky way
            val latitude = it.northeast.latitude + 1e-9 * Math.random()
            val ne = LatLng(latitude, it.northeast.longitude)
            val bounds = LatLngBounds(it.southwest, ne)

            _mapCameraBounds.value = MapViewCameraBounds(bounds)
            mapBoundsCache = bounds
        }
    }
}