package com.crisiscleanup.feature.cases

import android.content.ComponentCallbacks2
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.VisualAlertManager
import com.crisiscleanup.core.common.WorksiteLocationEditor
import com.crisiscleanup.core.common.event.TrimMemoryEventManager
import com.crisiscleanup.core.common.event.TrimMemoryListener
import com.crisiscleanup.core.common.haversineDistance
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
import com.crisiscleanup.core.commoncase.TransferWorkTypeProvider
import com.crisiscleanup.core.commoncase.WorksiteProvider
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.WorksiteInteractor
import com.crisiscleanup.core.data.incidentcache.IncidentDataPullReporter
import com.crisiscleanup.core.data.model.IncidentDataPullStats
import com.crisiscleanup.core.data.model.IncidentPullDataType
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.data.repository.IncidentCacheRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.domain.LoadSelectIncidents
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentIdWorksiteCount
import com.crisiscleanup.core.model.data.TableDataWorksite
import com.crisiscleanup.core.model.data.TableWorksiteClaimAction
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.feature.cases.CasesConstant.MAP_MARKERS_ZOOM_LEVEL
import com.crisiscleanup.feature.cases.map.CasesMapBoundsManager
import com.crisiscleanup.feature.cases.map.CasesMapMarkerManager
import com.crisiscleanup.feature.cases.map.CasesMapTileLayerManager
import com.crisiscleanup.feature.cases.map.CasesOverviewMapTileRenderer
import com.crisiscleanup.feature.cases.model.CoordinateBounds
import com.crisiscleanup.feature.cases.model.WorksiteQueryState
import com.crisiscleanup.feature.cases.model.asWorksiteGoogleMapMark
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
import kotlin.time.Duration.Companion.seconds
import com.crisiscleanup.core.commonassets.R as commonAssetsR

@OptIn(FlowPreview::class)
@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    incidentBoundsProvider: IncidentBoundsProvider,
    private val worksitesRepository: WorksitesRepository,
    incidentCacheRepository: IncidentCacheRepository,
    val incidentSelector: IncidentSelector,
    dataPullReporter: IncidentDataPullReporter,
    private val mapCaseIconProvider: MapCaseIconProvider,
    private val worksiteInteractor: WorksiteInteractor,
    private val mapTileRenderer: CasesOverviewMapTileRenderer,
    private val tileProvider: TileProvider,
    private val worksiteLocationEditor: WorksiteLocationEditor,
    private val permissionManager: PermissionManager,
    private val locationProvider: LocationProvider,
    filterRepository: CasesFilterRepository,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    worksiteProvider: WorksiteProvider,
    worksiteChangeRepository: WorksiteChangeRepository,
    accountDataRepository: AccountDataRepository,
    organizationsRepository: OrganizationsRepository,
    val transferWorkTypeProvider: TransferWorkTypeProvider,
    private val translator: KeyResourceTranslator,
    private val syncPuller: SyncPuller,
    val visualAlertManager: VisualAlertManager,
    appMemoryStats: AppMemoryStats,
    trimMemoryEventManager: TrimMemoryEventManager,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Cases) private val logger: AppLogger,
    val appEnv: AppEnv,
) : ViewModel(), TrimMemoryListener {
    val loadSelectIncidents = LoadSelectIncidents(
        incidentsRepository = incidentsRepository,
        accountDataRepository = accountDataRepository,
        incidentSelector = incidentSelector,
        appPreferencesRepository = appPreferencesRepository,
        coroutineScope = viewModelScope,
    )
    val incidentsData = loadSelectIncidents.data
    val enableIncidentSelect = incidentsRepository.isFirstLoad
        .map(Boolean::not)
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

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
        appPreferencesRepository,
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
    private val tableViewSortChange = AtomicBoolean(false)
    private val pendingTableSort = AtomicReference(WorksiteSortBy.None)
    val tableSortResultsMessage = MutableStateFlow("")

    private val tableViewDataLoader = CasesTableViewDataLoader(
        worksiteProvider,
        worksitesRepository,
        worksiteChangeRepository,
        accountDataRepository,
        organizationsRepository,
        incidentsRepository,
        translator,
        logger,
    )
    val isLoadingTableViewData = tableViewDataLoader.isLoading
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val openWorksiteAddFlagCounter = MutableStateFlow(0)
    private val openWorksiteAddFlag = AtomicBoolean(false)

    val worksitesChangingClaimAction = tableViewDataLoader.worksitesChangingClaimAction
    val changeClaimActionErrorMessage = MutableStateFlow("")

    fun setContentViewType(isTableView: Boolean) {
        this.isTableView.value = isTableView

        viewModelScope.launch {
            appPreferencesRepository.setWorkScreenView(isTableView)
        }

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

    val isIncidentLoading = incidentsRepository.isLoading

    val dataProgress = dataPullReporter.incidentDataPullStats
        .map {
            val showProgress = it.isOngoing && it.isPullingWorksites
            val isSecondary = it.pullType == IncidentPullDataType.WorksitesAdditional
            val progress = it.progress
            DataProgressMetrics(
                isSecondary,
                showProgress,
                progress,
            )
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = zeroDataProgress,
            started = SharingStarted.WhileSubscribed(),
        )

    /**
     * Incident or worksites data are currently saving/caching/loading
     */
    val isLoadingData = combine(
        incidentCacheRepository.isSyncingActiveIncident,
        worksitesRepository.isDeterminingWorksitesCount,
    ) { b0, b1 -> b0 || b1 }

    private var _mapCameraZoom = MutableStateFlow(MapViewCameraZoomDefault)
    val mapCameraZoom = _mapCameraZoom.asStateFlow()

    private val mapBoundsManager = CasesMapBoundsManager(
        incidentSelector,
        incidentBoundsProvider,
        appPreferencesRepository,
        viewModelScope,
        ioDispatcher,
        logger,
    )

    val incidentLocationBounds = mapBoundsManager.mapCameraBounds

    private val incidentWorksitesCount =
        worksitesRepository.streamIncidentWorksitesCount(incidentSelector.incidentId)
            .flowOn(ioDispatcher)
            .shareIn(
                scope = viewModelScope,
                replay = 1,
                started = SharingStarted.WhileSubscribed(1_000),
            )

    private val isGeneratingWorksiteMarkers = MutableStateFlow(false)
    val isMapBusy = combine(
        mapBoundsManager.isDeterminingBounds,
        mapTileRenderer.isBusy,
        isGeneratingWorksiteMarkers,
    ) { b0, b1, b2 -> b0 || b1 || b2 }

    private val mapMarkerManager = CasesMapMarkerManager(
        worksitesRepository,
        appMemoryStats,
        locationProvider,
        logger,
    )

    @OptIn(FlowPreview::class)
    val worksitesMapMarkers = combine(
        incidentWorksitesCount,
        qsm.worksiteQueryState,
        mapBoundsManager.isMapLoadedFlow,
        ::Triple,
    )
        // TODO Make delay a parameter
        .debounce(250)
        .mapLatest { (_, wqs, isMapLoaded) ->
            val id = wqs.incidentId

            val skipMarkers = !isMapLoaded ||
                wqs.isTableView ||
                id == EmptyIncident.id ||
                wqs.zoom < MAP_MARKERS_ZOOM_LEVEL

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
        worksitesChangingClaimAction,
        qsm.worksiteQueryState,
        ::Triple,
    )
        .mapLatest { (_, _, wqs) ->
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

    private val epochZero = Instant.fromEpochSeconds(0)
    private val tileClearRefreshInterval = 5.seconds
    private var tileRefreshedInstant = epochZero

    private val totalCasesCount = combine(
        isLoadingData,
        incidentSelector.incidentId,
        incidentWorksitesCount,
    ) { isLoading, incidentId, worksitesCount ->
        if (incidentId != worksitesCount.id) {
            return@combine -1
        }

        val totalCount = worksitesCount.filteredCount
        if (totalCount == 0 && isLoading) {
            return@combine -1
        }

        totalCount
    }
        .flowOn(ioDispatcher)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    val casesCountTableText = combine(
        totalCasesCount,
        qsm.isTableView,
        ::Pair,
    )
        .filter { (_, isTable) -> isTable }
        .map { (totalCount, _) ->
            when {
                totalCount < 0 -> ""
                totalCount == 1 -> "$totalCount ${translator("casesVue.case")}"
                else -> "$totalCount ${translator("casesVue.cases")}"
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    val casesCountMapText = combine(
        totalCasesCount,
        qsm.isTableView,
        worksitesMapMarkers,
        ::Triple,
    )
        .filter { (_, isTable, _) -> !isTable }
        .map { (totalCount, _, markers) ->
            if (totalCount < 0) {
                return@map ""
            }

            val visibleCount = markers.filterNot { it.isFilteredOut }.size

            val countText = if (visibleCount == totalCount || visibleCount == 0) {
                if (visibleCount == 0) {
                    translator("info.t_of_t_cases")
                        .replace("{visible_count}", "$totalCount")
                } else if (totalCount == 1) {
                    translator("info.1_of_1_case")
                } else {
                    translator("info.t_of_t_cases")
                        .replace("{visible_count}", "$totalCount")
                }
            } else {
                translator("info.v_of_t_cases")
                    .replace("{visible_count}", "$visibleCount")
                    .replace("{total_count}", "$totalCount")
            }

            countText
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    var showExplainPermissionLocation by mutableStateOf(false)
    var isMyLocationEnabled by mutableStateOf(false)

    init {
        trimMemoryEventManager.addListener(this)

        mapTileRenderer.enableTileBoundaries()
        viewModelScope.launch {
            setTileRendererLocation()
        }

        incidentSelector.incidentId
            .onEach {
                tileRefreshedInstant = epochZero
                mapTileRenderer.setIncident(it, 0, true)
                casesMapTileManager.clearTiles()
            }
            .launchIn(viewModelScope)

        combine(
            incidentWorksitesCount,
            dataPullReporter.incidentDataPullStats,
            filterRepository.casesFiltersLocation,
            ::Triple,
        )
            .throttleLatest(600)
            .onEach { refreshTiles(it.first, it.second) }
            .launchIn(viewModelScope)

        permissionManager.permissionChanges
            .onEach {
                if (it == locationPermissionGranted) {
                    setTileRendererLocation()

                    if (isTableView.value) {
                        val sortBy = pendingTableSort.getAndSet(WorksiteSortBy.None)
                        if (sortBy != WorksiteSortBy.None) {
                            setSortBy(sortBy)
                        }
                    } else {
                        setMapToMyCoordinates()
                    }
                }
                isMyLocationEnabled = permissionManager.hasLocationPermission.value
            }
            .launchIn(viewModelScope)

        tableViewSort
            .onEach { qsm.tableViewSort.value = it }
            .launchIn(viewModelScope)

        dataPullReporter.onIncidentDataPullComplete
            .throttleLatest(600)
            .onEach {
                filterRepository.reapplyFilters()
            }
            .launchIn(viewModelScope)
    }

    fun syncWorksitesDelta(forceRefreshAll: Boolean = false) {
        syncPuller.appPullIncidentData(
            cacheFullWorksites = true,
            restartCacheCheckpoint = forceRefreshAll,
        )
    }

    private suspend fun setTileRendererLocation() {
        mapTileRenderer.setLocation(locationProvider.getLocation())
    }

    fun refreshIncidentsData() {
        syncPuller.appPullIncidents()
    }

    suspend fun refreshIncidentsAsync() {
        syncPuller.syncPullIncidents()
    }

    fun overviewMapTileProvider(): TileProvider {
        // Do not try and be efficient here by returning null when tiling is not necessary (when used in compose).
        // Doing so will cause errors with TileOverlay and TileOverlayState#clearTileCache.
        return tileProvider
    }

    private suspend fun generateWorksiteMarkers(wqs: WorksiteQueryState) = coroutineScope {
        val id = wqs.incidentId
        val sw = wqs.coordinateBounds.southWest
        val ne = wqs.coordinateBounds.northEast
        val marksQuery = mapMarkerManager.queryWorksitesInBounds(id, sw, ne)
        val marks = marksQuery.first
        val markOffsets = mapMarkerManager.denseMarkerOffsets(marks, qsm.mapZoom.value)

        ensureActive()

        val now = Clock.System.now()
        marks.mapIndexed { index, mark ->
            val offset = if (index < markOffsets.size) {
                markOffsets[index]
            } else {
                mapMarkerManager.zeroOffset
            }
            val isSelected =
                worksiteInteractor.wasCaseSelected(incidentId, mark.id, reference = now)
            mark.asWorksiteGoogleMapMark(mapCaseIconProvider, isSelected, offset)
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

        val worksites = worksitesRepository.getTableData(
            wqs.incidentId,
            filters,
            sortBy,
            locationCoordinates,
        )

        val strideCount = 100
        val latitudeRad = locationCoordinates?.first?.radians ?: 0.0
        val longitudeRad = locationCoordinates?.second?.radians ?: 0.0
        val tableData = worksites.mapIndexed { i, tableData ->
            if (i % strideCount == 0) {
                ensureActive()
            }

            val distance = if (hasLocation) {
                val worksite = tableData.worksite
                haversineDistance(
                    latitudeRad,
                    longitudeRad,
                    worksite.latitude.radians,
                    worksite.longitude.radians,
                ).kmToMiles
            } else {
                -1.0
            }

            WorksiteDistance(tableData, distance)
        }

        if (isDistanceSort && tableData.isEmpty()) {
            tableSortResultsMessage.value =
                translator("caseView.no_cases_found_within_radius")
                    .replace(
                        "{search_radius}",
                        tableDataDistanceSortSearchRadius.toInt().toString(),
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
        isActiveChange: Boolean,
    ) {
        qsm.mapZoom.value = cameraPosition.zoom

        if (mapBoundsManager.isMapLoaded) {
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
            zoomLevel,
        )
    }

    fun zoomIn() = adjustMapZoom(qsm.mapZoom.value + 1)

    fun zoomOut() = adjustMapZoom(qsm.mapZoom.value - 1)

    fun zoomToIncidentBounds() {
        mapBoundsManager.restoreIncidentBounds()
    }

    fun zoomToInteractive() = adjustMapZoom(MAP_MARKERS_ZOOM_LEVEL + 0.5f)

    private fun setMapToMyCoordinates() {
        viewModelScope.launch {
            locationProvider.getLocation(10.seconds)?.let { myLocation ->
                _mapCameraZoom.value = MapViewCameraZoom(
                    myLocation.toLatLng(),
                    11f,
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
            PermissionStatus.Undefined,
            -> {
                // Ignore these statuses as they're not important
            }
        }
    }

    private suspend fun refreshTiles(
        idCount: IncidentIdWorksiteCount,
        pullStats: IncidentDataPullStats,
    ) = coroutineScope {
        if (mapTileRenderer.tilesIncident != idCount.id ||
            idCount.id != pullStats.incidentId
        ) {
            return@coroutineScope
        }

        val now = Clock.System.now()

        if (pullStats.isEnded) {
            tileRefreshedInstant = now
            mapTileRenderer.setIncident(idCount.id, idCount.totalCount, true)
            casesMapTileManager.clearTiles()
            return@coroutineScope
        }

        pullStats.apply {
            if (!isStarted || idCount.totalCount == 0) {
                return@coroutineScope
            }
        }

        val sinceLastRefresh = now - tileRefreshedInstant
        val refreshTiles = tileRefreshedInstant == epochZero ||
            now - pullStats.startTime > tileClearRefreshInterval &&
            sinceLastRefresh > tileClearRefreshInterval
        if (refreshTiles) {
            tileRefreshedInstant = now
            mapTileRenderer.setIncident(idCount.id, idCount.totalCount, true)
            casesMapTileManager.clearTiles()
        }
    }

    private fun setSortBy(sortBy: WorksiteSortBy) {
        viewModelScope.launch(ioDispatcher) {
            if (sortBy != appPreferencesRepository.userPreferences.first().tableViewSortBy) {
                tableViewSortChange.set(true)
                appPreferencesRepository.setTableViewSortBy(sortBy)
            }
        }
    }

    fun takeSortByChange() = tableViewSortChange.getAndSet(false)

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
                PermissionStatus.Undefined,
                -> {
                    // Ignorable
                }
            }
        } else {
            setSortBy(sortBy)
        }
    }

    fun onOpenCaseFlags(worksite: Worksite) {
        viewModelScope.launch(ioDispatcher) {
            if (tableViewDataLoader.loadWorksiteForAddFlags(worksite)) {
                openWorksiteAddFlag.set(true)
                openWorksiteAddFlagCounter.value++
            }
        }
    }

    fun onWorksiteClaimAction(
        worksite: Worksite,
        claimAction: TableWorksiteClaimAction,
    ) {
        changeClaimActionErrorMessage.value = ""
        viewModelScope.launch(ioDispatcher) {
            val result = tableViewDataLoader.onWorkTypeClaimAction(
                worksite,
                claimAction,
                transferWorkTypeProvider,
            )
            if (result.errorMessage.isNotBlank()) {
                changeClaimActionErrorMessage.value = result.errorMessage
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
    val data: TableDataWorksite,
    val distanceMiles: Double,
) {
    val worksite = data.worksite
    val claimStatus = data.claimStatus
}

data class DataProgressMetrics(
    val isSecondaryData: Boolean = false,
    val showProgress: Boolean = false,
    val progress: Float = 0.0f,
    val isLoadingPrimary: Boolean = showProgress && !isSecondaryData,
)

val zeroDataProgress = DataProgressMetrics()
