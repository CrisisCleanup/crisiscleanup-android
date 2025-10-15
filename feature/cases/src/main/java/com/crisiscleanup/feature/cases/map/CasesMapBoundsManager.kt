package com.crisiscleanup.feature.cases.map

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.model.data.EmptyIncident
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.FlowPreview::class)
internal class CasesMapBoundsManager(
    private val incidentSelector: IncidentSelector,
    private val incidentBoundsProvider: IncidentBoundsProvider,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    coroutineScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val logger: AppLogger,
) {
    private val epoch0 = Instant.fromEpochSeconds(0)
    private val mapLoadTime = MutableStateFlow(epoch0)

    val isMapLoaded: Boolean
        get() = mapLoadTime.value > epoch0
    val isMapLoadedFlow = mapLoadTime.mapLatest {
        it > epoch0
    }

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

    private val zeroBounds = LatLngBounds(
        LatLng(0.0, 0.0),
        LatLng(0.0, 0.0),
    )

    private val incidentBounds = incidentSelector.incident
        .flatMapLatest { incident ->
            incidentBoundsProvider.mapIncidentBounds(incident)
        }
        .map { incidentBounds ->
            if (incidentBounds.locations.isEmpty()) {
                zeroBounds
            } else {
                incidentBounds.bounds
            }
        }
        .distinctUntilChanged()
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    private val savedBounds = combine(
        incidentSelector.incidentId,
        appPreferencesRepository.userPreferences,
        ::Pair,
    )
        .map { (incidentId, userPreferences) ->
            val mapBounds = userPreferences.casesMapBounds
            if (incidentId != EmptyIncident.id &&
                incidentId == mapBounds.incidentId
            ) {
                mapBounds.latLngBounds
            } else {
                zeroBounds
            }
        }
        .distinctUntilChanged()

    private val isStarted: Boolean
        get() = isMapLoaded && Clock.System.now() - mapLoadTime.value > 2.seconds

    private val saveIncidentMapBounds = MutableStateFlow(IncidentCoordinateBoundsNone)

    init {
        // Starting bounds
        combine(
            isMapLoadedFlow,
            incidentBounds,
            savedBounds,
            ::Triple,
        )
            .throttleLatest(1.seconds.inWholeMilliseconds)
            .onEach { (_, ib, sb) ->
                val bounds = if (isStarted) {
                    zeroBounds
                } else {
                    if (sb != zeroBounds) {
                        sb
                    } else if (ib != zeroBounds) {
                        ib
                    } else {
                        zeroBounds
                    }
                }
                if (bounds != zeroBounds) {
                    cacheBounds(bounds, false)
                    _mapCameraBounds.value = MapViewCameraBounds(bounds, 0)
                }
            }
            .launchIn(coroutineScope)

        // Incident change bounds
        combine(
            isMapLoadedFlow,
            incidentBounds,
            ::Pair,
        )
            .debounce(100)
            .onEach { (_, ib) ->
                if (isStarted && ib != zeroBounds) {
                    cacheBounds(ib, true)
                    _mapCameraBounds.value = MapViewCameraBounds(ib)
                }
            }
            .launchIn(coroutineScope)

        saveIncidentMapBounds
            .filter { it != IncidentCoordinateBoundsNone }
            .throttleLatest(600)
            .onEach {
                appPreferencesRepository.setCasesMapBounds(it)
            }
            .flowOn(ioDispatcher)
            .launchIn(coroutineScope)
    }

    private var mapBoundsCache = MapViewCameraBoundsDefault.bounds

    private fun cacheBounds(bounds: LatLngBounds, cacheToDisk: Boolean) {
        if (bounds == mapBoundsCache) {
            return
        }

        mapBoundsCache = bounds

        if (isStarted && cacheToDisk) {
            val incidentId = incidentSelector.incidentId.value
            saveIncidentMapBounds.value = bounds.asIncidentCoordinateBounds(incidentId)
        }
    }

    fun cacheBounds(bounds: LatLngBounds) {
        cacheBounds(bounds, isStarted)
    }

    /**
     * @return true if state is changing from not loaded->loaded or false if loaded signal was already received
     */
    fun onMapLoaded() = mapLoadTime.compareAndSet(expect = epoch0, update = Clock.System.now())

    fun restoreBounds() {
        _mapCameraBounds.value = MapViewCameraBounds(mapBoundsCache, 0)
    }

    fun restoreIncidentBounds() {
        val incidentId = incidentSelector.incidentId.value
        incidentBoundsProvider.getIncidentBounds(incidentId)?.let {
            _mapCameraBounds.value = MapViewCameraBounds(it.bounds)
        }
    }
}

val IncidentCoordinateBounds.latLngBounds: LatLngBounds
    get() = LatLngBounds(
        LatLng(south, west),
        LatLng(north, east),
    )
