package com.crisiscleanup.feature.incidentcache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.locationPermissionGranted
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.common.subscribedReplay
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentCacheRepository
import com.crisiscleanup.core.data.repository.IncidentCacheStage
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.model.data.BOUNDED_REGION_RADIUS_MILES_DEFAULT
import com.crisiscleanup.core.model.data.InitialIncidentWorksitesCachePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@HiltViewModel
class IncidentWorksitesCacheViewModel @Inject constructor(
    incidentSelector: IncidentSelector,
    private val incidentCacheRepository: IncidentCacheRepository,
    permissionManager: PermissionManager,
    locationProvider: LocationProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    private val syncPuller: SyncPuller,
    appEnv: AppEnv,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Logger(CrisisCleanupLoggers.Sync) private val logger: AppLogger,
) : ViewModel() {
    val isNotProduction = appEnv.isNotProduction

    val incident = incidentSelector.incident

    val boundedRegionDataEditor: BoundedRegionDataEditor =
        IncidentCacheBoundedRegionDataEditor(
            permissionManager,
            locationProvider,
            drawableResourceBitmapProvider,
            viewModelScope,
            ioDispatcher,
        )

    val syncStage = incidentCacheRepository.cacheStage
        .stateIn(
            scope = viewModelScope,
            initialValue = IncidentCacheStage.Inactive,
            started = subscribedReplay(),
        )

    val isSyncing = incidentCacheRepository.isSyncingActiveIncident
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = subscribedReplay(),
        )

    val lastSynced = incident
        .flatMapLatest { incident ->
            incidentCacheRepository.streamSyncStats(incident.id)
                .mapNotNull {
                    it?.lastUpdated?.relativeTime
                }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = subscribedReplay(),
        )

    private var isUserActed = AtomicBoolean(false)

    val editingPreferences = MutableStateFlow(InitialIncidentWorksitesCachePreferences)

    private val hasUserInteracted: Boolean
        get() = isUserActed.get() || boundedRegionDataEditor.isUserActed

    private val epochZero = Instant.fromEpochSeconds(0)
    private val locationPermissionExpiration = AtomicReference(epochZero)

    init {
        incidentCacheRepository.cachePreferences
            .onEach {
                if (editingPreferences.compareAndSet(
                        InitialIncidentWorksitesCachePreferences,
                        it,
                    )
                ) {
                    val syncingRegionParameters = it.boundedRegionParameters
                    with(syncingRegionParameters) {
                        if (regionLatitude != 0.0 || regionLongitude != 0.0) {
                            boundedRegionDataEditor.setCoordinates(regionLatitude, regionLongitude)
                        }
                    }

                    if (syncingRegionParameters.isRegionMyLocation &&
                        !permissionManager.hasLocationPermission.value
                    ) {
                        editingPreferences.compareAndSet(
                            it,
                            editingPreferences.value.copy(
                                boundedRegionParameters = syncingRegionParameters.copy(
                                    isRegionMyLocation = false,
                                ),
                            ),
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        boundedRegionDataEditor.centerCoordinates
            .throttleLatest(100)
            .onEach { coordinates ->
                if (hasUserInteracted) {
                    val preferences = editingPreferences.value
                    editingPreferences.value = preferences.copy(
                        boundedRegionParameters = preferences.boundedRegionParameters.copy(
                            regionLatitude = coordinates.latitude,
                            regionLongitude = coordinates.longitude,
                        ),
                    )
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(externalScope)

        editingPreferences
            .throttleLatest(300)
            .onEach {
                if (!hasUserInteracted) {
                    return@onEach
                }

                incidentCacheRepository.updateCachePreferences(it)
            }
            .flowOn(ioDispatcher)
            .launchIn(externalScope)

        permissionManager.permissionChanges
            .onEach {
                if (it == locationPermissionGranted) {
                    val expirationTimestamp = locationPermissionExpiration.getAndSet(epochZero)
                    if (expirationTimestamp > Clock.System.now()) {
                        boundCachingCases(true)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updatePreferences(
        isPaused: Boolean,
        isRegionBounded: Boolean,
        isNearMe: Boolean = false,
        onPreferencesSent: () -> Unit = {},
    ) {
        val preferences = editingPreferences.value
        val boundedRegionParameters = with(editingPreferences.value.boundedRegionParameters) {
            if (isRegionBounded && regionRadiusMiles <= 0) {
                copy(
                    regionRadiusMiles = BOUNDED_REGION_RADIUS_MILES_DEFAULT,
                )
            } else {
                this
            }
        }.copy(
            isRegionMyLocation = isNearMe,
        )

        editingPreferences.value = preferences.copy(
            isPaused = isPaused,
            isRegionBounded = isRegionBounded,
            boundedRegionParameters,
        )

        onPreferencesSent()
    }

    private fun pullIncidentData() {
        syncPuller.appPullIncidentData(cancelOngoing = true)
    }

    fun resumeCachingCases() {
        isUserActed.set(true)

        updatePreferences(isPaused = false, isRegionBounded = false)

        pullIncidentData()
    }

    fun pauseCachingCases() {
        isUserActed.set(true)

        updatePreferences(isPaused = true, isRegionBounded = false)

        syncPuller.stopPullWorksites()
    }

    // TODO Simplify state management
    fun boundCachingCases(
        isNearMe: Boolean,
        isUserAction: Boolean = false,
    ) {
        if (isUserAction) {
            isUserActed.set(true)
        }

        val permissionStatus = if (isNearMe) {
            boundedRegionDataEditor.checkMyLocation()
        } else {
            null
        }

        if (!isNearMe || permissionStatus == PermissionStatus.Granted) {
            updatePreferences(
                isPaused = false,
                isRegionBounded = true,
                isNearMe = isNearMe,
            ) {
                if (isNearMe) {
                    boundedRegionDataEditor.useMyLocation()
                }

                pullIncidentData()
            }
        } else {
            if (isUserAction) {
                val now = Clock.System.now()
                val expiration = when (permissionStatus) {
                    PermissionStatus.Requesting -> now + 20.seconds
                    PermissionStatus.ShowRationale -> now + 120.seconds
                    else -> epochZero
                }
                locationPermissionExpiration.set(expiration)
            }
        }
    }

    fun resync() {
        pullIncidentData()
    }

    fun resetCaching() {
        viewModelScope.launch(ioDispatcher) {
            incidentCacheRepository.resetIncidentSyncStats(incident.value.id)
        }
    }

    fun setBoundedRegionRadius(radius: Float) {
        isUserActed.set(true)

        val preferences = editingPreferences.value
        editingPreferences.value = preferences.copy(
            boundedRegionParameters = preferences.boundedRegionParameters.copy(
                regionRadiusMiles = radius.toDouble(),
            ),
        )
    }
}
