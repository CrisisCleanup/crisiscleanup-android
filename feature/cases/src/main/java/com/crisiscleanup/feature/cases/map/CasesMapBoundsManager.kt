package com.crisiscleanup.feature.cases.map

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.mapmarker.util.flattenLatLng
import com.crisiscleanup.core.mapmarker.util.smallOffset
import com.crisiscleanup.core.mapmarker.util.toBounds
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentLocation
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

internal class CasesMapBoundsManager(
    coroutineScope: CoroutineScope,
    private val incidentSelector: IncidentSelector,
    incidentsRepository: IncidentsRepository,
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

    private val selectedIncident = incidentSelector.incidentId.flatMapLatest {
        incidentsRepository.streamIncident(it)
    }

    private val incidentLocationIds = incidentSelector.incident.mapLatest {
        it.locations.map(IncidentLocation::location)
    }

    private val incidentLatLngBoundary = combine(
        selectedIncident,
        incidentLocationIds,
        ::Pair
    )
        .filter { (incident, locationIds) ->
            incident != null && incident.id != EmptyIncident.id && locationIds.isNotEmpty()
        }
        .flatMapLatest { (incident, locationIds) ->
            isMappingLocationBounds.value = true
            try {
                locationsRepository.streamLocations(locationIds)
                    .map { locations ->
                        val latLngs = locations.toLatLng().flattenLatLng()
                        Pair(incident!!.id, latLngs)
                    }
            } finally {
                isMappingLocationBounds.value = false
            }
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = Pair(EmptyIncident.id, emptyList()),
            started = SharingStarted.WhileSubscribed(),
        )

    private val incidentBoundsCache = mutableMapOf<Long, LatLngBounds>()

    init {
        incidentLatLngBoundary
            .onEach { (incidentId, latLngs) ->
                isUpdatingCameraBounds.value = true
                try {
                    if (latLngs.isNotEmpty()) {
                        val bounds = latLngs.toBounds()
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
            val ne = it.northeast.smallOffset(1e-9)
            val bounds = LatLngBounds(it.southwest, ne)

            _mapCameraBounds.value = MapViewCameraBounds(bounds)
            mapBoundsCache = bounds
        }
    }
}