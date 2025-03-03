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
import com.crisiscleanup.core.common.ReplaySubscribed3
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
import com.crisiscleanup.core.commonassets.ui.getDisasterIcon
import com.crisiscleanup.core.commoncase.CaseFlagsNavigationState
import com.crisiscleanup.core.commoncase.CasesConstant.MAP_MARKERS_ZOOM_LEVEL
import com.crisiscleanup.core.commoncase.CasesCounter
import com.crisiscleanup.core.commoncase.TransferWorkTypeProvider
import com.crisiscleanup.core.commoncase.WorksiteProvider
import com.crisiscleanup.core.commoncase.map.CasesMapBoundsManager
import com.crisiscleanup.core.commoncase.map.CasesMapMarkerManager
import com.crisiscleanup.core.commoncase.map.CasesMapTileLayerManager
import com.crisiscleanup.core.commoncase.map.CasesOverviewMapTileRenderer
import com.crisiscleanup.core.commoncase.map.MapTileRefresher
import com.crisiscleanup.core.commoncase.model.CoordinateBounds
import com.crisiscleanup.core.commoncase.reset
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.WorksiteInteractor
import com.crisiscleanup.core.data.di.CasesFilterType
import com.crisiscleanup.core.data.di.CasesFilterTypes
import com.crisiscleanup.core.data.incidentcache.IncidentDataPullReporter
import com.crisiscleanup.core.data.model.progressMetrics
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AppPreferencesRepository
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.domain.LoadSelectIncidents
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.TableDataWorksite
import com.crisiscleanup.core.model.data.TableWorksiteClaimAction
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.core.model.data.zeroDataProgress
import com.crisiscleanup.feature.cases.model.WorksiteQueryState
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import com.crisiscleanup.core.commonassets.R as commonAssetsR

@OptIn(FlowPreview::class)
@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    incidentBoundsProvider: IncidentBoundsProvider,
    private val worksitesRepository: WorksitesRepository,
    val incidentSelector: IncidentSelector,
    dataPullReporter: IncidentDataPullReporter,
    mapCaseIconProvider: MapCaseIconProvider,
    worksiteInteractor: WorksiteInteractor,
    @CasesFilterType(CasesFilterTypes.Cases)
    private val mapTileRenderer: CasesOverviewMapTileRenderer,
    @CasesFilterType(CasesFilterTypes.Cases)
    private val tileProvider: TileProvider,
    private val worksiteLocationEditor: WorksiteLocationEditor,
    private val permissionManager: PermissionManager,
    private val locationProvider: LocationProvider,
    @CasesFilterType(CasesFilterTypes.Cases)
    filterRepository: CasesFilterRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
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

    val tableViewSort = appPreferencesRepository.preferences
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

    private val flagsNavigationState = CaseFlagsNavigationState(
        worksiteChangeRepository,
        worksitesRepository,
        worksiteProvider,
        viewModelScope,
        ioDispatcher,
    )
    val openWorksiteAddFlagCounter = flagsNavigationState.openWorksiteAddFlagCounter

    val isLoadingTableViewData = combine(
        tableViewDataLoader.isLoading,
        flagsNavigationState.isLoadingFlagsWorksite,
        ::Pair,
    )
        .map { (b0, b1) -> b0 || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val worksitesChangingClaimAction = tableViewDataLoader.worksitesChangingClaimAction
    val changeClaimActionErrorMessage = MutableStateFlow("")

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

    val isIncidentLoading = incidentsRepository.isLoading

    val dataProgress = dataPullReporter.incidentDataPullStats
        .map { it.progressMetrics }
        .stateIn(
            scope = viewModelScope,
            initialValue = zeroDataProgress,
            started = ReplaySubscribed3,
        )

    /**
     * Incident or worksites data are currently saving/caching/loading
     */
    val isLoadingData = combine(
        isIncidentLoading,
        dataProgress,
        worksitesRepository.isDeterminingWorksitesCount,
    ) { b0, progress, b2 -> b0 || progress.isLoadingPrimary || b2 }

    private var mapCameraZoomInternal = MutableStateFlow(MapViewCameraZoomDefault)
    val mapCameraZoom: StateFlow<MapViewCameraZoom> = mapCameraZoomInternal

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
        worksitesRepository.streamIncidentWorksitesCount(
            incidentSelector.incidentId,
            useTeamFilters = false,
        )
            .flowOn(ioDispatcher)
            .shareIn(
                scope = viewModelScope,
                replay = 1,
                started = ReplaySubscribed3,
            )

    private val mapMarkerManager = CasesMapMarkerManager(
        isTeamCasesMap = false,
        worksitesRepository,
        qsm.worksiteQueryState,
        mapBoundsManager,
        worksiteInteractor,
        mapCaseIconProvider,
        appMemoryStats,
        locationProvider,
        viewModelScope,
        ioDispatcher,
    )
    val worksitesMapMarkers = mapMarkerManager.worksitesMapMarkers
    val isMapBusy = combine(
        mapBoundsManager.isDeterminingBounds,
        mapTileRenderer.isBusy,
        mapMarkerManager.isGeneratingWorksiteMarkers,
    ) { b0, b1, b2 -> b0 || b1 || b2 }

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

    private val casesCounter = CasesCounter(
        incidentSelector,
        incidentWorksitesCount,
        isLoadingData,
        isMapVisible = qsm.worksiteQueryState.map { it.isMapView },
        worksitesMapMarkers,
        translator,
        viewModelScope,
        ioDispatcher,
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

    private val mapTileRefresher = MapTileRefresher(
        mapTileRenderer,
        casesMapTileManager,
    )

    private val totalCasesCount = casesCounter.totalCasesCount

    val casesCountTableText = combine(
        totalCasesCount,
        qsm.worksiteQueryState.map { it.isTableView },
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

    val casesCountMapText = casesCounter.casesCountMapText

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
                worksiteProvider.reset(it)
            }
            .launchIn(viewModelScope)

        combine(
            incidentWorksitesCount,
            dataPullReporter.incidentDataPullStats,
            filterRepository.casesFiltersLocation,
            ::Triple,
        )
            .throttleLatest(600)
            .onEach { mapTileRefresher.refreshTiles(it.first, it.second) }
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

        mapCameraZoomInternal.value = MapViewCameraZoom(
            mapBoundsManager.centerCache,
            (zoomLevel + Math.random() * 1e-3).toFloat(),
        )
    }

    fun zoomIn() = adjustMapZoom(qsm.mapZoom.value + 1)

    fun zoomOut() = adjustMapZoom(qsm.mapZoom.value - 1)

    fun zoomToIncidentBounds() = mapBoundsManager.restoreIncidentBounds()

    fun zoomToInteractive() = adjustMapZoom(MAP_MARKERS_ZOOM_LEVEL + 0.5f)

    private fun setMapToMyCoordinates() {
        viewModelScope.launch {
            locationProvider.getLocation()?.let { myLocation ->
                mapCameraZoomInternal.value = MapViewCameraZoom(
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
            PermissionStatus.Undefined,
            -> {
                // Ignore these statuses as they're not important
            }
        }
    }

    private fun setSortBy(sortBy: WorksiteSortBy) {
        viewModelScope.launch(ioDispatcher) {
            if (sortBy != appPreferencesRepository.preferences.first().tableViewSortBy) {
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

    fun onOpenCaseFlags(worksite: Worksite) = flagsNavigationState.onOpenCaseFlags(worksite)

    fun takeOpenWorksiteAddFlag() = flagsNavigationState.takeOpenWorksiteAddFlag()

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
