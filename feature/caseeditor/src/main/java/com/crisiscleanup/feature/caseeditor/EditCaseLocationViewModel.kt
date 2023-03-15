package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.*
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.Default
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.mapmarker.model.DefaultCoordinates
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.util.smallOffset
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.feature.caseeditor.model.LocationInputData
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class EditCaseLocationViewModel @Inject constructor(
    private val incidentsRepository: IncidentsRepository,
    private val worksiteProvider: EditableWorksiteProvider,
    private val permissionManager: PermissionManager,
    private val locationProvider: LocationProvider,
    resourceProvider: AndroidResourceProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @Dispatcher(Default) private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    val locationInputData: LocationInputData

    val navigateBack = mutableStateOf(false)

    var isMoveLocationOnMapMode = mutableStateOf(false)
    private var hasEnteredMoveLocationMapMode = false

    private val defaultMapZoom = 13 + (Math.random() * 1e-3).toFloat()
    private var zoomCache = defaultMapZoom
    private var _mapCameraZoom = MutableStateFlow(MapViewCameraZoomDefault)
    val mapCameraZoom = _mapCameraZoom.asStateFlow()

    /**
     * Indicates if the map was manually moved (since last checked)
     */
    private val isMapMoved = AtomicBoolean(false)

    init {
        val formFields = worksiteProvider.formFields
        val worksite = worksiteProvider.editableWorksite.value
        val coordinates =
            if (worksite.id == EmptyWorksite.id) DefaultCoordinates
            else LatLng(worksite.latitude, worksite.longitude)
        locationInputData = LocationInputData(
            worksite,
            coordinates,
            resourceProvider,
        )

        permissionManager.permissionChanges.map {
            if (it == locationPermissionGranted && !isMapMoved.getAndSet(false)) {
                setMyLocationCoordinates()
            }
        }.launchIn(viewModelScope)

        setDefaultMapCamera(coordinates)
    }

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
                // TODO Show dialog that user must manually enable permissions due to denying it previously
                logger.logDebug("Show rational for my location")
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

    fun onQueryChange(q: String) {
        locationInputData.locationQuery = q
        // TODO Update reactive variable and query local and backend results when connected to internet (and not expired token)
    }

    private fun centerCoordinatesZoom() = MapViewCameraZoom(
        locationInputData.coordinates.value.smallOffset(),
        defaultMapZoom,
        0,
    )

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