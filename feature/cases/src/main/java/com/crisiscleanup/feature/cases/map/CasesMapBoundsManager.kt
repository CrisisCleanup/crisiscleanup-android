package com.crisiscleanup.feature.cases.map

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.mapmarker.model.DefaultIncidentBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.mapmarker.util.smallOffset
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

internal class CasesMapBoundsManager(
    coroutineScope: CoroutineScope,
    private val incidentSelector: IncidentSelector,
    private val incidentBoundsProvider: IncidentBoundsProvider,
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

    init {
        incidentIdBounds
            .onEach { incidentBounds ->
                if (incidentBounds.locations.isNotEmpty()) {
                    val bounds = incidentBounds.bounds
                    if (isMapLoaded.value) {
                        _mapCameraBounds.value = MapViewCameraBounds(bounds)
                    }
                    cacheBounds(bounds)
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
        isMapLoaded.value = false
    }

    /**
     * @return true if state is changing from not loaded->loaded or false if loaded signal was already received
     */
    fun onMapLoaded(): Boolean {
        if (isMapLoaded.value) {
            return false
        }
        isMapLoaded.value = true

        return true
    }

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
            mapBoundsCache = latLngBounds
        }
    }
}