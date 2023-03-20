package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.addresssearch.AddressSearchRepository
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
import com.crisiscleanup.feature.caseeditor.model.ExistingCaseLocation
import com.crisiscleanup.feature.caseeditor.model.LocationInputData
import com.crisiscleanup.feature.caseeditor.model.coordinates
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
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @Dispatcher(Default) private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val locationInputData: LocationInputData

    private val locationSearchManager: LocationSearchManager
    val searchResults: StateFlow<LocationSearchResults>
    private val isSearchResultSelected = AtomicBoolean(false)

    val navigateBack = mutableStateOf(false)

    var isMoveLocationOnMapMode = mutableStateOf(false)
    private var hasEnteredMoveLocationMapMode = false

    private val defaultMapZoom = 13 + (Math.random() * 1e-3).toFloat()
    private var zoomCache = defaultMapZoom
    private var _mapCameraZoom = MutableStateFlow(MapViewCameraZoomDefault)
    val mapCameraZoom = _mapCameraZoom.asStateFlow()

    val mapMarkerIcon = mutableStateOf<BitmapDescriptor?>(null)

    /**
     * Indicates if the map was manually moved (since last checked)
     */
    private val isMapMoved = AtomicBoolean(false)

    val showExplainPermissionLocation = mutableStateOf(false)

    init {
        val formFields = worksiteProvider.formFields
        var worksite = worksiteProvider.editableWorksite.value
        if (worksite.id == EmptyWorksite.id) {
            worksite = worksite.copy(
                latitude = DefaultCoordinates.latitude,
                longitude = DefaultCoordinates.longitude,
            )
        }
        locationInputData = LocationInputData(
            worksite,
            resourceProvider,
        )

        locationSearchManager = LocationSearchManager(
            worksite.incidentId,
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

        searchResults.onEach {
            logger.logDebug("query ${it.query} ${it.addresses.size} ${it.worksites.size}")

        }.launchIn(viewModelScope)

        permissionManager.permissionChanges.map {
            if (it == locationPermissionGranted && !isMapMoved.getAndSet(false)) {
                setMyLocationCoordinates()
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            mapMarkerIcon.value = drawableResourceBitmapProvider.getIcon(
                commonR.drawable.cc_foreground_pin,
                Pair(32f, 48f),
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

    private fun setDefaultMapCamera(coordinates: LatLng) {
        if (coordinates == DefaultCoordinates) {
            setMyLocationCoordinates()
        } else {
            _mapCameraZoom.value = MapViewCameraZoom(coordinates, defaultMapZoom)
        }
    }

    private fun validateSaveWorksite(): Boolean {
//        val updatedWorksite = .updateCase()
//        if (updatedWorksite != null) {
//            worksiteProvider.editableWorksite.value = updatedWorksite
//            return true
//        }
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
        if (isSearchResultSelected.compareAndSet(true, false)) {
            _mapCameraZoom.value = centerCoordinatesZoom(500)
        }
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

    private fun onSearchResultSelect(coordinates: LatLng) {
        locationInputData.coordinates.value = coordinates
        isSearchResultSelected.set(true)
        clearQuery()
    }

    fun onExistingWorksiteSelected(caseLocation: ExistingCaseLocation) {
        // TODO This should load/prompt (to edit) the existing case. If load clear nav backstack as well.
        with(caseLocation) {
            onSearchResultSelect(coordinates)
            // TODO Address data
        }
    }

    fun onGeocodeAddressSelected(address: LocationAddress) {
        with(address) {
            onSearchResultSelect(LatLng(latitude, longitude))
            // TODO Address data
        }
    }

    fun onSystemBack(): Boolean {
        return validateSaveWorksite()
    }

    fun onNavigateBack(): Boolean {
        return validateSaveWorksite()
    }

    fun onNavigateCancel(): Boolean {
        return true
    }
}
