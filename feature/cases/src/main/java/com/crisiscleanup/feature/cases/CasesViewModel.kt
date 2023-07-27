package com.crisiscleanup.feature.cases

import android.content.ComponentCallbacks2
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.HaversineDistance
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.VisualAlertManager
import com.crisiscleanup.core.common.WorksiteLocationEditor
import com.crisiscleanup.core.common.event.TrimMemoryEventManager
import com.crisiscleanup.core.common.event.TrimMemoryListener
import com.crisiscleanup.core.common.kmToMiles
import com.crisiscleanup.core.common.locationPermissionGranted
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.commonassets.getDisasterIcon
import com.crisiscleanup.core.commoncase.WorksiteProvider
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.data.util.IncidentDataPullReporter
import com.crisiscleanup.core.data.util.IncidentDataPullStats
import com.crisiscleanup.core.domain.LoadIncidentDataUseCase
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.feature.cases.map.CasesMapBoundsManager
import com.crisiscleanup.feature.cases.map.CasesMapMarkerManager
import com.crisiscleanup.feature.cases.map.CasesMapTileLayerManager
import com.crisiscleanup.feature.cases.map.CasesOverviewMapTileRenderer
import com.crisiscleanup.feature.cases.map.IncidentIdWorksiteCount
import com.crisiscleanup.feature.cases.model.CoordinateBounds
import com.crisiscleanup.feature.cases.model.WorksiteQueryState
import com.crisiscleanup.feature.cases.model.asWorksiteGoogleMapMark
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds
import com.crisiscleanup.core.commonassets.R as commonAssetsR

@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    incidentBoundsProvider: IncidentBoundsProvider,
    private val worksitesRepository: WorksitesRepository,
    private val incidentSelector: IncidentSelector,
    loadIncidentDataUseCase: LoadIncidentDataUseCase,
    dataPullReporter: IncidentDataPullReporter,
    private val mapCaseIconProvider: MapCaseIconProvider,
    private val mapTileRenderer: CasesOverviewMapTileRenderer,
    private val tileProvider: TileProvider,
    private val worksiteLocationEditor: WorksiteLocationEditor,
    private val permissionManager: PermissionManager,
    private val locationProvider: LocationProvider,
    filterRepository: CasesFilterRepository,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    worksiteProvider: WorksiteProvider,
    worksiteChangeRepository: WorksiteChangeRepository,
    private val translator: KeyResourceTranslator,
    private val syncPuller: SyncPuller,
    val visualAlertManager: VisualAlertManager,
    appMemoryStats: AppMemoryStats,
    trimMemoryEventManager: TrimMemoryEventManager,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Cases) private val logger: AppLogger,
    val appEnv: AppEnv,
) : ViewModel(), TrimMemoryListener {
    val incidentsData = loadIncidentDataUseCase()

    val incidentId: Long
        get() = incidentSelector.incidentId.value
    val selectedIncident = incidentSelector.incident
    val disasterIconResId = incidentSelector.incident.map { getDisasterIcon(it.disaster) }
        .stateIn(
            scope = viewModelScope,
            initialValue = commonAssetsR.drawable.ic_disaster_other,
            started = SharingStarted.WhileSubscribed(),
        )

    private val qsm = CasesQueryStateManager(
        incidentSelector,
        filterRepository,
        viewModelScope,
    )

    val filtersCount = filterRepository.filtersCount

    val isTableView = qsm.isTableView

    private val tableDataDistanceSortSearchRadius = 100.0f

    val tableViewSort = appPreferencesRepository.userPreferences
        .map { it.tableViewSortBy }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            initialValue = WorksiteSortBy.None,
            started = SharingStarted.WhileSubscribed(),
        )
    private val pendingTableSort = AtomicReference(WorksiteSortBy.None)
    val tableSortResultsMessage = MutableStateFlow("")

    private val tableViewDataLoader = CasesTableViewDataLoader(
        worksiteProvider,
        worksitesRepository,
        worksiteChangeRepository,
    )
    val isLoadingTableViewData = tableViewDataLoader.isLoading
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val openWorksiteAddFlagCounter = MutableStateFlow(0)
    private val openWorksiteAddFlag = AtomicBoolean(false)

    fun setContentViewType(isTableView: Boolean) {
        this.isTableView.value = isTableView

        if (!isTableView) {
            mapBoundsManager.restoreBounds()
        }
    }

    var isLayerView = mutableStateOf(false)
        private set

    fun toggleLayersView() {
        isLayerView.value = !isLayerView.value
    }

    val editedWorksiteLocation: LatLng?
        get() = worksiteLocationEditor.takeEditedLocation()?.let { LatLng(it.first, it.second) }

    val showDataProgress = dataPullReporter.incidentDataPullStats.map { it.isOngoing }
    val dataProgress = dataPullReporter.incidentDataPullStats.map { it.progress }

    private var _mapCameraZoom = MutableStateFlow(MapViewCameraZoomDefault)
    val mapCameraZoom = _mapCameraZoom.asStateFlow()

    private val mapBoundsManager = CasesMapBoundsManager(
        viewModelScope,
        incidentSelector,
        incidentBoundsProvider,
        ioDispatcher,
        logger,
    )

    val incidentLocationBounds = mapBoundsManager.mapCameraBounds

    val isIncidentLoading = incidentsRepository.isLoading

    private val incidentWorksitesCount = incidentSelector.incidentId.flatMapLatest { id ->
        worksitesRepository.streamIncidentWorksitesCount(id)
            .map { count -> IncidentIdWorksiteCount(id, count) }
    }.shareIn(
        scope = viewModelScope,
        replay = 1,
        started = SharingStarted.WhileSubscribed(1_000)
    )

    private val isGeneratingWorksiteMarkers = MutableStateFlow(false)
    val isMapBusy = combine(
        mapBoundsManager.isDeterminingBounds,
        mapTileRenderer.isBusy,
        isGeneratingWorksiteMarkers,
    ) { b0, b1, b2 -> b0 || b1 || b2 }

    private val isFetchingTableData = MutableStateFlow(false)
    val isTableBusy: StateFlow<Boolean> = isFetchingTableData

    private val mapMarkerManager = CasesMapMarkerManager(
        worksitesRepository,
        appMemoryStats,
        logger,
    )

    val worksitesMapMarkers = combine(
        incidentWorksitesCount,
        qsm.worksiteQueryState,
        mapBoundsManager.isMapLoaded,
        ::Triple
    )
        // TODO Make delay a parameter
        .debounce(250)
        .mapLatest { (_, wqs, isMapLoaded) ->
            val id = wqs.incidentId

            val skipMarkers = !isMapLoaded ||
                    isTableView.value ||
                    id == EmptyIncident.id ||
                    mapTileRenderer.rendersAt(wqs.zoom)

            if (skipMarkers) {
                emptyList()
            } else {
                isGeneratingWorksiteMarkers.value = true
                try {
                    val markers = generateWorksiteMarkers(wqs)
                    markers
                } finally {
                    isGeneratingWorksiteMarkers.value = false
                }
            }
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val tableData = combine(
        incidentWorksitesCount,
        qsm.worksiteQueryState,
        ::Pair,
    )
        .mapLatest { (_, wqs) ->
            if (wqs.isTableView) {
                tableSortResultsMessage.value = ""
                fetchTableData(wqs)
            } else {
                emptyList()
            }
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    private val casesMapTileManager = CasesMapTileLayerManager(
        viewModelScope,
        incidentSelector,
        mapBoundsManager,
        logger,
    )
    val overviewTileDataChange = casesMapTileManager.overviewTileDataChange
    val clearTileLayer: Boolean
        get() = casesMapTileManager.clearTileLayer

    private val tileClearRefreshInterval = 5.seconds
    private var tileRefreshedInstant: Instant = Instant.fromEpochSeconds(0)
    private var tileClearWorksitesCount = 0

    val casesCount = combine(
        isIncidentLoading,
        worksitesMapMarkers,
        incidentWorksitesCount,
    ) { isSyncing, markers, worksitesCount ->
        var totalCount = worksitesCount.count
        if (totalCount == 0 && isSyncing) {
            totalCount = -1
        }
        Pair(markers.size, totalCount)
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = Pair(0, -1),
            started = SharingStarted.WhileSubscribed(),
        )

    var showExplainPermissionLocation by mutableStateOf(false)
    var isMyLocationEnabled by mutableStateOf(false)

    init {
        trimMemoryEventManager.addListener(this)

        mapTileRenderer.enableTileBoundaries()

        combine(
            incidentWorksitesCount,
            dataPullReporter.incidentDataPullStats,
            ::Pair,
        )
            .debounce(16)
            .throttleLatest(1_000)
            .onEach { refreshTiles(it.first, it.second) }
            .launchIn(viewModelScope)

        permissionManager.permissionChanges
            .map {
                if (it == locationPermissionGranted) {
                    if (isTableView.value) {
                        val sortBy = pendingTableSort.getAndSet(WorksiteSortBy.None)
                        if (sortBy != WorksiteSortBy.None) {
                            setSortBy(sortBy)
                        }
                    } else {
                        setMapToMyCoordinates()
                    }
                }
                isMyLocationEnabled = permissionManager.hasLocationPermission
            }
            .launchIn(viewModelScope)

        tableViewSort
            .onEach { qsm.tableViewSort.value = it }
            .launchIn(viewModelScope)
    }

    fun refreshIncidentsData() {
        syncPuller.appPull(true, cancelOngoing = true)
    }

    fun overviewMapTileProvider(): TileProvider {
        // Do not try and be efficient here by returning null when tiling is not necessary (when used in compose).
        // Doing so will cause errors with TileOverlay and TileOverlayState#clearTileCache.
        return tileProvider
    }

    private val zeroOffset = Pair(0f, 0f)
    private suspend fun generateWorksiteMarkers(wqs: WorksiteQueryState) = coroutineScope {
        val id = wqs.incidentId
        val sw = wqs.coordinateBounds.southWest
        val ne = wqs.coordinateBounds.northEast
        val marksQuery = mapMarkerManager.queryWorksitesInBounds(id, sw, ne)
        val marks = marksQuery.first
        val markOffsets = denseMarkerOffsets(marks)

        ensureActive()

        marks.mapIndexed { index, mark ->
            val offset = if (index < markOffsets.size) markOffsets[index] else zeroOffset
            mark.asWorksiteGoogleMapMark(mapCaseIconProvider, offset)
        }
    }

    private suspend fun fetchTableData(wqs: WorksiteQueryState) = coroutineScope {
        val filters = wqs.filters
        var sortBy = wqs.tableViewSort

        val isDistanceSort = sortBy == WorksiteSortBy.Nearest
        val locationCoordinates = locationProvider.getLocation()
        val hasLocation = locationCoordinates != null
        if (isDistanceSort && !hasLocation) {
            sortBy = WorksiteSortBy.CaseNumber
        }

        val locationLatitude = locationCoordinates?.first ?: 0.0
        val locationLongitude = locationCoordinates?.second ?: 0.0
        val worksites = worksitesRepository.getTableData(
            incidentId,
            filters,
            sortBy,
            locationLatitude,
            locationLongitude,
        )

        val strideCount = 100
        val locationLatitudeRad = locationLatitude.radians
        val locationLongitudeRad = locationLongitude.radians
        val tableData = worksites.mapIndexed { i, worksite ->
            if (i % strideCount == 0) {
                ensureActive()
            }

            var distance = -1.0
            if (hasLocation) {
                val worksiteLatitudeRad = worksite.latitude.radians
                val worksiteLongitudeRad = worksite.longitude.radians
                val haversineDistance = HaversineDistance.calculate(
                    locationLatitudeRad, locationLongitudeRad,
                    worksiteLatitudeRad, worksiteLongitudeRad,
                )
                distance = haversineDistance.kmToMiles
            }

            WorksiteDistance(worksite, distance)
        }

        if (isDistanceSort && tableData.isEmpty()) {
            tableSortResultsMessage.value =
                translator("~~No Cases were found within {search_radius} mi.")
                    .replace(
                        "{search_radius}",
                        tableDataDistanceSortSearchRadius.toInt().toString()
                    )
        }

        tableData
    }

    fun onMapLoadStart() {
        // Do not try to optimize delaying marker loading until the map is loaded.
        // A lot of state is changing. Refactor or keep simple.
    }

    fun onMapLoaded() {
        if (mapBoundsManager.onMapLoaded()) {
            mapBoundsManager.restoreBounds()
        }
    }

    fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean
    ) {
        qsm.mapZoom.value = cameraPosition.zoom

        if (mapBoundsManager.isMapLoaded.value) {
            projection?.let {
                val visibleBounds = it.visibleRegion.latLngBounds
                qsm.mapBounds.value = CoordinateBounds(
                    visibleBounds.southwest,
                    visibleBounds.northeast,
                )
                mapBoundsManager.cacheBounds(visibleBounds)
            }
        }
    }

    private fun adjustMapZoom(zoomLevel: Float) {
        if (zoomLevel < 3 || zoomLevel > 17) {
            return
        }

        _mapCameraZoom.value = MapViewCameraZoom(
            mapBoundsManager.centerCache,
            (zoomLevel + Math.random() * 1e-3).toFloat(),
        )
    }

    fun zoomIn() = adjustMapZoom(qsm.mapZoom.value + 1)

    fun zoomOut() = adjustMapZoom(qsm.mapZoom.value - 1)

    fun zoomToIncidentBounds() {
        mapBoundsManager.restoreIncidentBounds()
    }

    fun zoomToInteractive() = adjustMapZoom(mapTileRenderer.zoomThreshold + 1.1f)

    private fun setMapToMyCoordinates() {
        viewModelScope.launch {
            locationProvider.getLocation()?.let { myLocation ->
                _mapCameraZoom.value = MapViewCameraZoom(
                    myLocation.toLatLng(),
                    (11f + Math.random() * 1e-3).toFloat(),
                )
            }
        }
    }

    fun useMyLocation() {
        when (permissionManager.requestLocationPermission()) {
            PermissionStatus.Granted -> {
                setMapToMyCoordinates()
            }

            PermissionStatus.ShowRationale -> {
                showExplainPermissionLocation = true
            }

            PermissionStatus.Requesting,
            PermissionStatus.Denied,
            PermissionStatus.Undefined -> {
                // Ignore these statuses as they're not important
            }
        }
    }

    private suspend fun refreshTiles(
        idCount: IncidentIdWorksiteCount,
        pullStats: IncidentDataPullStats,
    ) = coroutineScope {
        var refreshTiles = true
        var clearCache = false

        pullStats.run {
            if (!isStarted || idCount.id != incidentId) {
                return@run
            }

            refreshTiles = isEnded
            clearCache = isEnded

            if (this.dataCount < 3000) {
                return@run
            }

            val now = Clock.System.now()
            if (!refreshTiles && progress > saveStartedAmount) {
                val sinceLastRefresh = now - tileRefreshedInstant
                val projectedDelta = projectedFinish - now
                refreshTiles = now - pullStart > tileClearRefreshInterval &&
                        sinceLastRefresh > tileClearRefreshInterval &&
                        projectedDelta > tileClearRefreshInterval
                if (idCount.count - tileClearWorksitesCount >= 6000 &&
                    dataCount - tileClearWorksitesCount > 3000
                ) {
                    clearCache = true
                    refreshTiles = true
                }
            }
            if (refreshTiles) {
                tileRefreshedInstant = now
            }
        }

        if (refreshTiles) {
            if (mapTileRenderer.setIncident(idCount.id, idCount.count, clearCache)) {
                clearCache = true
            }
        }

        if (clearCache) {
            tileClearWorksitesCount = idCount.count
            casesMapTileManager.clearTiles()
        } else if (refreshTiles) {
            casesMapTileManager.onTileChange()
        }
    }

    private val denseMarkCountThreshold = 15
    private val denseMarkZoomThreshold = 14
    private val denseDegreeThreshold = 0.0001
    private val denseScreenOffsetScale = 0.6f
    private suspend fun denseMarkerOffsets(marks: List<WorksiteMapMark>): List<Pair<Float, Float>> =
        coroutineScope {
            if (marks.size > denseMarkCountThreshold ||
                qsm.mapZoom.value < denseMarkZoomThreshold
            ) {
                return@coroutineScope emptyList()
            }

            ensureActive()

            val bucketIndices = IntArray(marks.size) { -1 }
            val buckets = mutableListOf<MutableList<Int>>()
            for (i in 0 until marks.size - 1) {
                val iMark = marks[i]
                for (j in i + 1 until marks.size) {
                    val jMark = marks[j]
                    if (abs(iMark.latitude - jMark.latitude) < denseDegreeThreshold &&
                        abs(iMark.longitude - jMark.longitude) < denseDegreeThreshold
                    ) {
                        val bucketI = bucketIndices[i]
                        if (bucketI >= 0) {
                            bucketIndices[j] = bucketI
                            buckets[bucketI].add(j)
                        } else {
                            val bucketJ = bucketIndices[j]
                            if (bucketJ >= 0) {
                                bucketIndices[i] = bucketJ
                                buckets[bucketJ].add(i)
                            } else {
                                val bucketIndex = buckets.size
                                bucketIndices[i] = bucketIndex
                                bucketIndices[j] = bucketIndex
                                buckets.add(mutableListOf(i, j))
                            }
                        }
                        break
                    }
                }
                ensureActive()
            }

            val markOffsets = marks.map { zeroOffset }.toMutableList()
            if (buckets.isNotEmpty()) {
                buckets.forEach {
                    val count = it.size
                    val offsetScale = denseScreenOffsetScale + (count - 5).coerceAtLeast(0) * 0.2f
                    if (count > 1) {
                        var offsetDir = (PI * 0.5).toFloat()
                        val deltaDirDegrees = (2 * PI / count).toFloat()
                        it.forEach { index ->
                            markOffsets[index] = Pair(
                                offsetScale * cos(offsetDir),
                                offsetScale * sin(offsetDir),
                            )
                            offsetDir += deltaDirDegrees
                        }
                    }
                }
            }
            markOffsets
        }

    private fun setSortBy(sortBy: WorksiteSortBy) {
        viewModelScope.launch(ioDispatcher) {
            appPreferencesRepository.setTableViewSortBy(sortBy)
        }
    }

    fun changeTableSort(sortBy: WorksiteSortBy) {
        if (sortBy == WorksiteSortBy.Nearest) {
            when (permissionManager.requestLocationPermission()) {
                PermissionStatus.Granted -> {
                    setSortBy(sortBy)
                }

                PermissionStatus.ShowRationale -> {
                    pendingTableSort.set(sortBy)
                    showExplainPermissionLocation = true
                }

                PermissionStatus.Requesting -> {
                    pendingTableSort.set(sortBy)
                }

                PermissionStatus.Denied,
                PermissionStatus.Undefined -> {
                    // Ignorable
                }
            }
        } else {
            setSortBy(sortBy)
        }
    }

    fun onOpenCaseFlags(worksite: Worksite) {
        viewModelScope.launch(ioDispatcher) {
            if (tableViewDataLoader.loadWorksiteForAddFlags(worksite.id)) {
                openWorksiteAddFlag.set(true)
                openWorksiteAddFlagCounter.value++
            }
        }
    }

    fun takeOpenWorksiteAddFlag() = openWorksiteAddFlag.getAndSet(false)

    // TrimMemoryListener

    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                casesMapTileManager.clearTiles()
            }
        }
    }
}

data class WorksiteDistance(
    val worksite: Worksite,
    val distanceMiles: Double,
)