package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.addresssearch.AddressSearchRepository
import com.crisiscleanup.core.addresssearch.model.KeySearchAddress
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.locationPermissionGranted
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.Default
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.mapmarker.model.DefaultCoordinates
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.util.smallOffset
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.LocationAddress
import com.crisiscleanup.feature.caseeditor.model.LocationInputData
import com.crisiscleanup.feature.caseeditor.model.coordinates
import com.crisiscleanup.feature.caseeditor.model.toLatLng
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import com.crisiscleanup.core.common.R as commonR
import com.crisiscleanup.core.mapmarker.R as mapMarkerR

interface CaseLocationDataEditor {
    val locationInputData: LocationInputData

    val searchResults: StateFlow<LocationSearchResults>

    val isProcessingAction: StateFlow<Boolean>
    val isAddressCommitted: StateFlow<Boolean>

    val editIncidentWorksite: StateFlow<ExistingWorksiteIdentifier>
    val isLocationCommitted: StateFlow<Boolean>

    val takeClearSearchInputFocus: Boolean

    val defaultMapZoom: Float

    val mapCameraZoom: StateFlow<MapViewCameraZoom>

    val showExplainPermissionLocation: MutableState<Boolean>

    val isLocationSearching: Flow<Boolean>

    val isShortQuery: StateFlow<Boolean>

    val mapMarkerIcon: StateFlow<BitmapDescriptor?>

    val isCheckingOutOfBounds: StateFlow<Boolean>
    val locationOutOfBounds: StateFlow<LocationOutOfBounds?>
    val locationOutOfBoundsMessage: StateFlow<String>

    var isMapLoaded: Boolean

    /**
     * Encourages presenting a search bar for searching location/address by text query
     *
     * FALSE indicates location and address have been sufficiently defined and full search support isn't necessary.
     */
    val isSearchSuggested: Boolean

    val onSetMyLocationAddress: StateFlow<LocationAddress?>

    fun useMyLocation()

    fun onQueryChange(q: String)

    fun onMapLoaded()

    fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean,
    )

    fun setMoveLocationOnMap(moveOnMap: Boolean)
    fun onSaveMoveLocationCoordinates(): Boolean

    fun centerOnLocation()

    fun onExistingWorksiteSelected(result: CaseSummaryResult)

    fun onGeocodeAddressSelected(searchAddress: KeySearchAddress)

    fun clearAddress()
    fun onEditAddress()

    fun cancelOutOfBounds()
    fun changeIncidentOutOfBounds(locationOutOfBounds: LocationOutOfBounds)
    fun acceptOutOfBounds(locationOutOfBounds: LocationOutOfBounds)

    fun onBackValidateSaveWorksite(): Boolean

    fun commitChanges()
}

internal class EditableLocationDataEditor(
    private val worksiteProvider: EditableWorksiteProvider,
    private val permissionManager: PermissionManager,
    private val locationProvider: LocationProvider,
    boundsProvider: IncidentBoundsProvider,
    searchWorksitesRepository: SearchWorksitesRepository,
    private val addressSearchRepository: AddressSearchRepository,
    caseIconProvider: MapCaseIconProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    private val existingWorksiteSelector: ExistingWorksiteSelector,
    translator: KeyResourceTranslator,
    private val logger: AppLogger,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScope: CoroutineScope,
) : CaseLocationDataEditor {
    private val incidentId: Long

    override val locationInputData: LocationInputData

    private val locationSearchManager: LocationSearchManager
    override val searchResults: StateFlow<LocationSearchResults>
    private val isLocationAddressFound = AtomicBoolean(false)

    private val isSelectingWorksite = MutableStateFlow(false)
    private val isSelectingAddress = MutableStateFlow(false)
    override val isProcessingAction = kotlinx.coroutines.flow.combine(
        isSelectingWorksite,
        isSelectingAddress,
        ::Pair,
    )
        .map { (b0, b1) -> b0 || b1 }
        .stateIn(
            scope = coroutineScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    override val isAddressCommitted = MutableStateFlow(false)

    private val outOfBoundsManager = LocationOutOfBoundsManager(
        worksiteProvider,
        boundsProvider,
        coroutineDispatcher,
        coroutineScope,
    )
    override val isCheckingOutOfBounds = outOfBoundsManager.isCheckingOutOfBounds
    override val locationOutOfBounds = outOfBoundsManager.locationOutOfBounds

    override val editIncidentWorksite = existingWorksiteSelector.selected
    override val isLocationCommitted = MutableStateFlow(false)

    private val clearSearchInputFocus = AtomicBoolean(false)
    override val takeClearSearchInputFocus: Boolean
        get() = clearSearchInputFocus.getAndSet(false)

    private val isMoveLocationOnMapMode = AtomicBoolean(false)
    private var hasEnteredMoveLocationMapMode = false

    override val defaultMapZoom: Float
        get() {
            val zoom = if (worksiteProvider.editableWorksite.value.address.isBlank()) {
                7
            } else if (isMoveLocationOnMapMode.get()) {
                19
            } else {
                13
            }
            return zoom + (Math.random() * 1e-3).toFloat()
        }
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

    override var isMapLoaded = false

    override val isSearchSuggested: Boolean
        get() = with(locationInputData) {
            !(
                wasGeocodeAddressSelected ||
                    isEditingAddress ||
                    isLocationAddressFound.get()
                ) || isBlankAddress
        }

    override val onSetMyLocationAddress = MutableStateFlow<LocationAddress?>(null)

    init {
        val worksite = worksiteProvider.editableWorksite.value
        incidentId = worksite.incidentId

        locationInputData = LocationInputData(
            translator,
            worksite,
        )

        locationSearchManager = LocationSearchManager(
            incidentId,
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

        permissionManager.permissionChanges
            .onEach {
                if (it == locationPermissionGranted && !isMapMoved.getAndSet(false)) {
                    setMyLocationCoordinates()
                }
            }
            .launchIn(coroutineScope)

        addressSearchRepository.startSearchSession()

        // TODO Common dimensions
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

        setDefaultMapCamera(worksite.coordinates)
    }

    override val isLocationSearching = locationSearchManager.isSearching

    override val isShortQuery = locationInputData.locationQuery
        .map { it.trim().length < locationSearchManager.querySearchThresholdLength }
        .stateIn(
            scope = coroutineScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(),
        )

    @OptIn(FlowPreview::class)
    private val isLocationInBounds = locationInputData.coordinates
        .debounce(100)
        .map {
            val isInBounds = worksiteProvider.incidentBounds.containsLocation(it)

            isInBounds
        }
        .flowOn(ioDispatcher)

    override val mapMarkerIcon = isLocationInBounds
        .map {
            if (it) {
                inBoundsPinIcon
            } else {
                outOfBoundsPinIcon
            }
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    override val locationOutOfBoundsMessage = isLocationInBounds
        .map {
            if (it) {
                ""
            } else {
                translator("caseForm.case_outside_incident_name")
                    .replace("{incident_name}", worksiteProvider.incident.name)
            }
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
        updatedWorksite?.let {
            worksiteProvider.editableWorksite.value = it
            return true
        }
        return false
    }

    private fun setMyLocationCoordinates(isUserAction: Boolean = false) {
        coroutineScope.launch(coroutineDispatcher) {
            locationProvider.getLocation()?.let {
                val coordinates = it.toLatLng().smallOffset()
                locationInputData.coordinates.value = coordinates
                _mapCameraZoom.value = MapViewCameraZoom(coordinates, defaultMapZoom)
                // TODO Is isUserAction correct when permission must be granted?
                if (isUserAction) {
                    locationSearchManager.queryAddress(coordinates)?.let { address ->
                        setSearchedLocationAddress(address)
                        onSetMyLocationAddress.value = address
                    }
                }
            }
        }
    }

    override fun useMyLocation() {
        when (permissionManager.requestLocationPermission()) {
            PermissionStatus.Granted -> {
                setMyLocationCoordinates(true)
            }

            PermissionStatus.ShowRationale -> {
                showExplainPermissionLocation.value = true
            }

            PermissionStatus.Requesting -> {
                isMapMoved.set(false)
            }

            PermissionStatus.Denied,
            PermissionStatus.Undefined,
            -> {
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
        val isResultSelected = isLocationAddressFound.compareAndSet(true, false)
        val duration = if (isResultSelected) 500 else 0
        _mapCameraZoom.value = centerCoordinatesZoom(duration)
        isMapLoaded = true
    }

    override fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean,
    ) {
        zoomCache = cameraPosition.zoom

        if (isMoveLocationOnMapMode.get()) {
            projection?.let {
                if (hasEnteredMoveLocationMapMode) {
                    if (isMapLoaded) {
                        val center = it.visibleRegion.latLngBounds.center
                        locationInputData.coordinates.value = center
                    }
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

    private fun toggleMoveLocationOnMap() {
        _mapCameraZoom.value = centerCoordinatesZoom()
        val isMoveMode = isMoveLocationOnMapMode.get()
        if (!isMoveMode) {
            hasEnteredMoveLocationMapMode = false
        }
        isMoveLocationOnMapMode.set(!isMoveMode)
    }

    override fun setMoveLocationOnMap(moveOnMap: Boolean) {
        if (isMoveLocationOnMapMode.get() == moveOnMap) {
            return
        }
        toggleMoveLocationOnMap()
    }

    private fun commitLocationCoordinates(coordinates: LatLng) {
        locationInputData.coordinates.value = coordinates
        commitChanges()
    }

    private fun isCoordinatesInBounds(coordinates: LatLng) =
        worksiteProvider.incidentBounds.containsLocation(coordinates)

    override fun onSaveMoveLocationCoordinates(): Boolean {
        val coordinates = locationInputData.coordinates.value

        if (isCoordinatesInBounds(coordinates)) {
            commitLocationCoordinates(coordinates)
            return true
        }

        outOfBoundsManager.onLocationOutOfBounds(coordinates)
        return false
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
        isLocationAddressFound.set(true)
        clearSearchInputFocus.set(true)
        clearQuery()
    }

    override fun onExistingWorksiteSelected(result: CaseSummaryResult) {
        isSelectingWorksite.value = true
        coroutineScope.launch(ioDispatcher) {
            try {
                existingWorksiteSelector.onNetworkWorksiteSelected(result.networkWorksiteId)
            } finally {
                isSelectingWorksite.value = false
            }
        }
    }

    private fun setSearchedLocationAddress(locationAddress: LocationAddress) {
        with(locationAddress) {
            onSearchResultSelect(
                locationAddress.toLatLng(),
                address,
                zipCode,
                county,
                city,
                state,
            )
        }
    }

    override fun onGeocodeAddressSelected(searchAddress: KeySearchAddress) {
        with(outOfBoundsManager) {
            if (isPendingOutOfBounds) {
                return
            }

            isSelectingAddress.value = true
            coroutineScope.launch(ioDispatcher) {
                try {
                    addressSearchRepository.getPlaceAddress(searchAddress.key)?.let { address ->
                        val latLng = LatLng(address.latitude, address.longitude)
                        if (!isCoordinatesInBounds(latLng)) {
                            onLocationOutOfBounds(latLng, address)
                            return@launch
                        }

                        setSearchedLocationAddress(address)
                        commitChanges()
                        isAddressCommitted.value = true
                    }
                } catch (e: Exception) {
                    logger.logException(e)
                } finally {
                    isSelectingAddress.value = false
                }
            }
        }
    }

    override fun cancelOutOfBounds() {
        outOfBoundsManager.clearOutOfBounds()
    }

    override fun changeIncidentOutOfBounds(locationOutOfBounds: LocationOutOfBounds) {
        with(locationOutOfBounds) {
            recentIncident?.let {
                if (address == null) {
                    locationInputData.coordinates.value = coordinates
                } else {
                    setSearchedLocationAddress(address)
                }
                val worksiteChange = locationInputData.addressChangeWorksite
                worksiteProvider.setIncidentAddressChanged(recentIncident, worksiteChange)
            }
        }
        outOfBoundsManager.clearOutOfBounds()
        isLocationCommitted.value = true
    }

    override fun acceptOutOfBounds(locationOutOfBounds: LocationOutOfBounds) {
        with(locationOutOfBounds) {
            if (address != null) {
                setSearchedLocationAddress(address)
                commitChanges()
            } else {
                commitLocationCoordinates(coordinates)
            }
        }
        outOfBoundsManager.clearOutOfBounds()
        isLocationCommitted.value = true
    }

    override fun clearAddress() {
        with(locationInputData) {
            streetAddress = ""
            city = ""
            zipCode = ""
            county = ""
            state = ""
        }
    }

    override fun onEditAddress() {
        locationInputData.isEditingAddress = true
    }

    override fun onBackValidateSaveWorksite(): Boolean {
        if (isMoveLocationOnMapMode.compareAndSet(true, false)) {
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
    boundsProvider: IncidentBoundsProvider,
    searchWorksitesRepository: SearchWorksitesRepository,
    addressSearchRepository: AddressSearchRepository,
    caseIconProvider: MapCaseIconProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    existingWorksiteSelector: ExistingWorksiteSelector,
    networkMonitor: NetworkMonitor,
    translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
    @Dispatcher(Default) coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val editor: CaseLocationDataEditor = EditableLocationDataEditor(
        worksiteProvider,
        permissionManager,
        locationProvider,
        boundsProvider,
        searchWorksitesRepository,
        addressSearchRepository,
        caseIconProvider,
        drawableResourceBitmapProvider,
        existingWorksiteSelector,
        translator,
        logger,
        coroutineDispatcher,
        ioDispatcher,
        viewModelScope,
    )

    val isOnline = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(),
        )

    private fun onBackValidateSaveWorksite() = editor.onBackValidateSaveWorksite()

    override fun onSystemBack() = onBackValidateSaveWorksite()

    override fun onNavigateBack() = onBackValidateSaveWorksite()
}

internal class LocationOutOfBoundsManager(
    private val worksiteProvider: EditableWorksiteProvider,
    private val boundsProvider: IncidentBoundsProvider,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val coroutineScope: CoroutineScope,
) {
    val locationOutOfBounds = MutableStateFlow<LocationOutOfBounds?>(null)

    val isCheckingOutOfBounds = MutableStateFlow(false)

    val isPendingOutOfBounds: Boolean
        get() = isCheckingOutOfBounds.value || locationOutOfBounds.value != null

    fun clearOutOfBounds() {
        locationOutOfBounds.value = null
    }

    fun onLocationOutOfBounds(
        coordinates: LatLng,
        selectedAddress: LocationAddress? = null,
    ) {
        coroutineScope.launch(coroutineDispatcher) {
            val outOfBoundsData = LocationOutOfBounds(
                worksiteProvider.incident,
                coordinates,
                selectedAddress,
            )

            isCheckingOutOfBounds.value = true
            try {
                val recentIncident = boundsProvider.isInRecentIncidentBounds(coordinates)
                locationOutOfBounds.value = if (recentIncident == null) {
                    outOfBoundsData
                } else {
                    outOfBoundsData.copy(recentIncident = recentIncident)
                }
            } finally {
                isCheckingOutOfBounds.value = false
            }
        }
    }
}

data class LocationOutOfBounds(
    val incident: Incident,
    val coordinates: LatLng,
    val address: LocationAddress? = null,
    val recentIncident: Incident? = null,
)
