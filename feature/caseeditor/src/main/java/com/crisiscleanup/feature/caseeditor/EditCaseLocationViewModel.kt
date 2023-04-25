package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.addresssearch.AddressSearchRepository
import com.crisiscleanup.core.addresssearch.model.toLatLng
import com.crisiscleanup.core.common.*
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.Default
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.mapmarker.model.DefaultCoordinates
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.util.smallOffset
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.LocationAddress
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.model.*
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import com.crisiscleanup.core.common.R as commonR
import com.crisiscleanup.core.mapmarker.R as mapMarkerR

interface CaseLocationDataEditor {
    val locationInputData: LocationInputData

    val searchResults: StateFlow<LocationSearchResults>

    val editIncidentWorksite: StateFlow<ExistingWorksiteIdentifier>

    val takeClearSearchInputFocus: Boolean

    var isMoveLocationOnMapMode: MutableState<Boolean>

    val defaultMapZoom: Float

    val mapCameraZoom: StateFlow<MapViewCameraZoom>

    val showExplainPermissionLocation: MutableState<Boolean>

    val isLocationSearching: Flow<Boolean>

    val isShortQuery: StateFlow<Boolean>

    val mapMarkerIcon: StateFlow<BitmapDescriptor?>

    val locationOutOfBoundsMessage: StateFlow<String>

    fun useMyLocation()

    fun onQueryChange(q: String)

    fun onMapLoaded()

    fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean,
    )

    fun toggleMoveLocationOnMap()
    fun setMoveLocationOnMap(moveOnMap: Boolean)

    fun centerOnLocation()

    fun onExistingWorksiteSelected(result: CaseSummaryResult)

    fun onGeocodeAddressSelected(locationAddress: LocationAddress)

    fun onBackValidateSaveWorksite(): Boolean

    fun commitChanges()
}

internal class EditableLocationDataEditor(
    private val worksiteProvider: EditableWorksiteProvider,
    private val permissionManager: PermissionManager,
    private val locationProvider: LocationProvider,
    searchWorksitesRepository: SearchWorksitesRepository,
    addressSearchRepository: AddressSearchRepository,
    caseIconProvider: MapCaseIconProvider,
    resourceProvider: AndroidResourceProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    private val existingWorksiteSelector: ExistingWorksiteSelector,
    logger: AppLogger,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScope: CoroutineScope,
) : CaseLocationDataEditor {
    // Worksite before lat,lng may have been auto updated
    private val worksiteIn: Worksite

    override val locationInputData: LocationInputData

    private val locationSearchManager: LocationSearchManager
    override val searchResults: StateFlow<LocationSearchResults>
    private val isSearchResultSelected = AtomicBoolean(false)

    override val editIncidentWorksite = existingWorksiteSelector.selected

    private val clearSearchInputFocus = AtomicBoolean(false)
    override val takeClearSearchInputFocus: Boolean
        get() = clearSearchInputFocus.getAndSet(false)

    override var isMoveLocationOnMapMode = mutableStateOf(false)
    private var hasEnteredMoveLocationMapMode = false

    override val defaultMapZoom: Float
        get() = (if (isMoveLocationOnMapMode.value) 19 else 13) + (Math.random() * 1e-3).toFloat()
    private var zoomCache = defaultMapZoom
    private var _mapCameraZoom = MutableStateFlow(MapViewCameraZoomDefault)
    override val mapCameraZoom = _mapCameraZoom.asStateFlow()

    private var inBoundsPinIcon: BitmapDescriptor? = null
    private var outOfBoundsPinIcon: BitmapDescriptor? = null

    /**
     * Indicates if the map was manually moved (since last checked)
     */
    private val isMapMoved = AtomicBoolean(false)

    override val showExplainPermissionLocation = mutableStateOf(false)

    init {
        var worksite = worksiteProvider.editableWorksite.value
        worksiteIn = worksite

        if (worksite.isNew &&
            (worksite.coordinates() == EmptyWorksite.coordinates() ||
                    worksite.coordinates() == DefaultCoordinates)
        ) {
            val incidentBounds = worksiteProvider.incidentBounds
            var worksiteCoordinates: LatLng = incidentBounds.centroid
            locationProvider.coordinates?.let {
                val deviceLocation = LatLng(it.first, it.second)
                if (incidentBounds.containsLocation(deviceLocation)) {
                    worksiteCoordinates = deviceLocation
                }
            }
            worksite = worksite.copy(
                latitude = worksiteCoordinates.latitude,
                longitude = worksiteCoordinates.longitude,
            )
        }

        locationInputData = LocationInputData(
            worksite,
            resourceProvider,
        )
        locationInputData.coordinates
            .debounce(100)
            .onEach {
                if (!worksiteProvider.incidentBounds.containsLocation(it)) {
                    // TODO Prompt depending if in another recent incident's bounds or not
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(coroutineScope)

        locationSearchManager = LocationSearchManager(
            worksite.incidentId,
            worksiteProvider,
            locationInputData,
            searchWorksitesRepository,
            locationProvider,
            addressSearchRepository,
            caseIconProvider,
            ioDispatcher,
        )
        searchResults = locationSearchManager.searchResults.stateIn(
            scope = coroutineScope,
            initialValue = LocationSearchResults("", emptyList(), emptyList()),
            started = SharingStarted.WhileSubscribed(),
        )

        permissionManager.permissionChanges.map {
            if (it == locationPermissionGranted && !isMapMoved.getAndSet(false)) {
                setMyLocationCoordinates()
            }
        }.launchIn(coroutineScope)

        val pinMarkerSize = Pair(32f, 48f)
        coroutineScope.launch {
            inBoundsPinIcon = drawableResourceBitmapProvider.getIcon(
                commonR.drawable.cc_foreground_pin,
                pinMarkerSize,
            )
            outOfBoundsPinIcon = drawableResourceBitmapProvider.getIcon(
                mapMarkerR.drawable.cc_pin_location_out_of_bounds,
                pinMarkerSize,
            )
        }

        setDefaultMapCamera(worksite.coordinates())
    }

    override val isLocationSearching = locationSearchManager.isSearching

    override val isShortQuery = locationInputData.locationQuery
        .map { it.trim().length < locationSearchManager.querySearchThresholdLength }
        .stateIn(
            scope = coroutineScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(),
        )

    private val isLocationInBounds = locationInputData.coordinates
        .debounce(100)
        .map {
            val isInBounds = worksiteProvider.incidentBounds.containsLocation(it)

            isInBounds
        }
        .flowOn(ioDispatcher)

    override val mapMarkerIcon = isLocationInBounds
        .map {
            if (it) inBoundsPinIcon
            else outOfBoundsPinIcon
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    override val locationOutOfBoundsMessage = isLocationInBounds
        .map {
            if (it) ""
            else resourceProvider.getString(
                R.string.location_out_of_incident_bounds,
                worksiteProvider.incident.name,
            )
        }
        .stateIn(
            scope = coroutineScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )


    private fun setDefaultMapCamera(coordinates: LatLng) {
        if (coordinates == DefaultCoordinates) {
            setMyLocationCoordinates()
        } else {
            _mapCameraZoom.value = MapViewCameraZoom(coordinates, defaultMapZoom)
        }
    }

    private fun validateSaveWorksite(): Boolean {
        val updatedWorksite = locationInputData.updateCase()
        if (updatedWorksite != null) {
            var hasActualChanges = true

            if (worksiteIn.isNew && updatedWorksite.address.isBlank()) {
                val unchangedLatLngWorksite = updatedWorksite.copy(
                    latitude = worksiteIn.latitude,
                    longitude = worksiteIn.longitude,
                )
                if (unchangedLatLngWorksite == worksiteIn) {
                    hasActualChanges = false
                }
            }

            // Do not update editable if only the coordinates were auto completed
            if (hasActualChanges) {
                worksiteProvider.editableWorksite.value = updatedWorksite
            }
            return true
        }
        return false
    }

    private fun setMyLocationCoordinates() {
        coroutineScope.launch(coroutineDispatcher) {
            locationProvider.getLocation()?.let {
                val coordinates = it.toLatLng().smallOffset()
                locationInputData.coordinates.value = coordinates
                _mapCameraZoom.value = MapViewCameraZoom(coordinates, defaultMapZoom)
            }
        }
    }

    override fun useMyLocation() {
        when (permissionManager.requestLocationPermission()) {
            PermissionStatus.Granted -> {
                setMyLocationCoordinates()
            }
            PermissionStatus.ShowRationale -> {
                showExplainPermissionLocation.value = true
            }
            PermissionStatus.Requesting -> {
                isMapMoved.set(false)
            }
            PermissionStatus.Denied,
            PermissionStatus.Undefined -> {
                // Ignore these statuses as they're not important
            }
        }
    }

    private fun clearQuery() {
        locationInputData.locationQuery.value = ""
    }

    override fun onQueryChange(q: String) {
        locationInputData.locationQuery.value = q
    }

    private fun centerCoordinatesZoom(durationMs: Int = 0) = MapViewCameraZoom(
        locationInputData.coordinates.value.smallOffset(),
        defaultMapZoom,
        durationMs,
    )

    override fun onMapLoaded() {
        val isResultSelected = isSearchResultSelected.compareAndSet(true, false)
        val duration = if (isResultSelected) 500 else 0
        _mapCameraZoom.value = centerCoordinatesZoom(duration)
    }

    override fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean
    ) {
        zoomCache = cameraPosition.zoom

        if (isMoveLocationOnMapMode.value) {
            projection?.let {
                if (hasEnteredMoveLocationMapMode) {
                    val center = it.visibleRegion.latLngBounds.center
                    locationInputData.coordinates.value = center
                } else {
                    hasEnteredMoveLocationMapMode = true
                    _mapCameraZoom.value = centerCoordinatesZoom()
                }
            }
        }

        if (isActiveChange) {
            isMapMoved.set(true)
        }
    }

    override fun toggleMoveLocationOnMap() {
        _mapCameraZoom.value = centerCoordinatesZoom()
        if (!isMoveLocationOnMapMode.value) {
            hasEnteredMoveLocationMapMode = false
        }
        isMoveLocationOnMapMode.value = !isMoveLocationOnMapMode.value
    }

    override fun setMoveLocationOnMap(moveOnMap: Boolean) {
        if (isMoveLocationOnMapMode.value == moveOnMap) {
            return
        }
        toggleMoveLocationOnMap()
    }

    override fun centerOnLocation() {
        val coordinates = locationInputData.coordinates.value.smallOffset()
        _mapCameraZoom.value = MapViewCameraZoom(coordinates, zoomCache)
    }

    private fun onSearchResultSelect(
        coordinates: LatLng,
        address: String,
        zipCode: String,
        county: String,
        city: String,
        state: String,
    ) {
        with(locationInputData) {
            this.coordinates.value = coordinates
            streetAddress = address
            this.zipCode = zipCode
            this.county = county
            this.city = city
            this.state = state
            resetValidity()
        }
        isSearchResultSelected.set(true)
        clearSearchInputFocus.set(true)
        clearQuery()
    }

    override fun onExistingWorksiteSelected(result: CaseSummaryResult) {
        coroutineScope.launch(ioDispatcher) {
            existingWorksiteSelector.onNetworkWorksiteSelected(result.networkWorksiteId)
        }
    }

    override fun onGeocodeAddressSelected(locationAddress: LocationAddress) {
        with(locationAddress) {
            onSearchResultSelect(
                toLatLng(),
                address,
                zipCode,
                county,
                city,
                state,
            )
        }
    }

    override fun onBackValidateSaveWorksite(): Boolean {
        if (isMoveLocationOnMapMode.value) {
            isMoveLocationOnMapMode.value = false
            return false
        }

        with(locationInputData) {
            if (locationQuery.value.isNotEmpty()) {
                locationQuery.value = ""
                return false
            }
        }

        return validateSaveWorksite()
    }

    // This was added for the infinite form. Understand the design before making changes.
    override fun commitChanges() {
        worksiteProvider.setAddressChanged(locationInputData.addressChangeWorksite)
    }
}

@HiltViewModel
class EditCaseLocationViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    permissionManager: PermissionManager,
    locationProvider: LocationProvider,
    searchWorksitesRepository: SearchWorksitesRepository,
    addressSearchRepository: AddressSearchRepository,
    caseIconProvider: MapCaseIconProvider,
    resourceProvider: AndroidResourceProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    existingWorksiteSelector: ExistingWorksiteSelector,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
    @Dispatcher(Default) coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val editor: CaseLocationDataEditor = EditableLocationDataEditor(
        worksiteProvider,
        permissionManager,
        locationProvider,
        searchWorksitesRepository,
        addressSearchRepository,
        caseIconProvider,
        resourceProvider,
        drawableResourceBitmapProvider,
        existingWorksiteSelector,
        logger,
        coroutineDispatcher,
        ioDispatcher,
        viewModelScope,
    )

    private fun onBackValidateSaveWorksite() = editor.onBackValidateSaveWorksite()

    override fun onSystemBack() = onBackValidateSaveWorksite()

    override fun onNavigateBack() = onBackValidateSaveWorksite()
}
