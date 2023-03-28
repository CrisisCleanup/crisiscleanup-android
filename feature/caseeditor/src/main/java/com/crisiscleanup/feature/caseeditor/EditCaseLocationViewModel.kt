package com.crisiscleanup.feature.caseeditor

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
import com.crisiscleanup.core.data.repository.IncidentsRepository
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
import com.crisiscleanup.feature.caseeditor.model.*
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import com.crisiscleanup.core.common.R as commonR
import com.crisiscleanup.core.mapmarker.R as mapMarkerR

@HiltViewModel
class EditCaseLocationViewModel @Inject constructor(
    private val incidentsRepository: IncidentsRepository,
    worksiteProvider: EditableWorksiteProvider,
    private val permissionManager: PermissionManager,
    private val locationProvider: LocationProvider,
    searchWorksitesRepository: SearchWorksitesRepository,
    addressSearchRepository: AddressSearchRepository,
    caseIconProvider: MapCaseIconProvider,
    resourceProvider: AndroidResourceProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    private val existingWorksiteSelector: ExistingWorksiteSelector,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
    @Dispatcher(Default) private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val locationInputData: LocationInputData

    private val locationSearchManager: LocationSearchManager
    val searchResults: StateFlow<LocationSearchResults>
    private val isSearchResultSelected = AtomicBoolean(false)

    val editIncidentWorksite = existingWorksiteSelector.selected

    private val clearSearchInputFocus = AtomicBoolean(false)
    val takeClearSearchInputFocus: Boolean
        get() = clearSearchInputFocus.getAndSet(false)

    var isMoveLocationOnMapMode = mutableStateOf(false)
    private var hasEnteredMoveLocationMapMode = false

    val defaultMapZoom = 13 + (Math.random() * 1e-3).toFloat()
    private var zoomCache = defaultMapZoom
    private var _mapCameraZoom = MutableStateFlow(MapViewCameraZoomDefault)
    val mapCameraZoom = _mapCameraZoom.asStateFlow()

    private var inBoundsPinIcon: BitmapDescriptor? = null
    private var outOfBoundsPinIcon: BitmapDescriptor? = null

    /**
     * Indicates if the map was manually moved (since last checked)
     */
    private val isMapMoved = AtomicBoolean(false)

    val showExplainPermissionLocation = mutableStateOf(false)

    init {
        var worksite = worksiteProvider.editableWorksite.value

        if (worksite.isNew &&
            (worksite.coordinates() == EmptyWorksite.coordinates() ||
                    worksite.coordinates() == DefaultCoordinates)
        ) {
            val incidentBounds = worksiteProvider.incidentBounds
            var worksiteCoordinates: LatLng = incidentBounds.center
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
            .launchIn(viewModelScope)

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
            scope = viewModelScope,
            initialValue = LocationSearchResults("", emptyList(), emptyList()),
            started = SharingStarted.WhileSubscribed(),
        )

        permissionManager.permissionChanges.map {
            if (it == locationPermissionGranted && !isMapMoved.getAndSet(false)) {
                setMyLocationCoordinates()
            }
        }.launchIn(viewModelScope)

        val pinMarkerSize = Pair(32f, 48f)
        viewModelScope.launch {
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

    val isLocationSearching = locationSearchManager.isSearching

    val isShortQuery = locationInputData.locationQuery
        .map { it.trim().length < locationSearchManager.querySearchThresholdLength }
        .stateIn(
            scope = viewModelScope,
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

    val mapMarkerIcon = isLocationInBounds
        .map {
            if (it) inBoundsPinIcon
            else outOfBoundsPinIcon
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    val locationOutOfBoundsMessage = isLocationInBounds
        .map {
            if (it) ""
            else resourceProvider.getString(
                R.string.location_out_of_incident_bounds,
                worksiteProvider.incident.name,
            )
        }
        .stateIn(
            scope = viewModelScope,
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
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }

    private fun setMyLocationCoordinates() {
        viewModelScope.launch(coroutineDispatcher) {
            locationProvider.getLocation()?.let {
                val coordinates = it.toLatLng().smallOffset()
                locationInputData.coordinates.value = coordinates
                _mapCameraZoom.value = MapViewCameraZoom(coordinates, defaultMapZoom)
            }
        }
    }

    fun useMyLocation() {
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

    fun onQueryChange(q: String) {
        locationInputData.locationQuery.value = q
    }

    private fun centerCoordinatesZoom(durationMs: Int = 0) = MapViewCameraZoom(
        locationInputData.coordinates.value.smallOffset(),
        defaultMapZoom,
        durationMs,
    )

    fun onMapLoaded() {
        val isResultSelected = isSearchResultSelected.compareAndSet(true, false)
        val duration = if (isResultSelected) 500 else 0
        _mapCameraZoom.value = centerCoordinatesZoom(duration)
    }

    fun onMapCameraChange(
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

    fun toggleMoveLocationOnMap() {
        if (isMoveLocationOnMapMode.value) {
            _mapCameraZoom.value = centerCoordinatesZoom()
        } else {
            hasEnteredMoveLocationMapMode = false
        }
        isMoveLocationOnMapMode.value = !isMoveLocationOnMapMode.value
    }

    fun centerOnLocation() {
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

    fun onExistingWorksiteSelected(caseLocation: ExistingCaseLocation) {
        viewModelScope.launch(ioDispatcher) {
            existingWorksiteSelector.onNetworkWorksiteSelected(caseLocation.networkWorksiteId)
        }
    }

    fun onGeocodeAddressSelected(locationAddress: LocationAddress) {
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

    private fun onBackValidateSaveWorksite(): Boolean {
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

    override fun onSystemBack() = onBackValidateSaveWorksite()

    override fun onNavigateBack() = onBackValidateSaveWorksite()
}
