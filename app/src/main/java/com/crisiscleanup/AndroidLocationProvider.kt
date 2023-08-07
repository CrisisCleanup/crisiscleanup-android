package com.crisiscleanup

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.locationPermissionGranted
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class AndroidLocationProvider @Inject constructor(
    private val permissionManager: PermissionManager,
    @ApplicationContext private val context: Context,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
    @ApplicationScope externalScope: CoroutineScope,
) : LocationProvider, LocationListener {
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    override var coordinates: Pair<Double, Double>? = null

    override var intervalMillis: Long = 600_000

    override val cachedLocationTime = MutableStateFlow<Triple<Double, Double, Instant>?>(null)
    override var cachedLocation =
        cachedLocationTime.map { it?.let { c -> Pair(c.first, c.second) } }
            .stateIn(
                scope = externalScope,
                initialValue = null,
                started = SharingStarted.WhileSubscribed(),
            )

    init {
        permissionManager.permissionChanges.onEach {
            if (it == locationPermissionGranted) {
                requestLocationUpdates()
            }
        }
            .launchIn(externalScope)
    }

    override suspend fun getLocation(): Pair<Double, Double>? {
        val locationPermission = ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION)
        if (locationPermission == PackageManager.PERMISSION_GRANTED) {
            try {
                var location = locationClient.lastLocation.await()
                location?.apply {
                    if (Clock.System.now() - Instant.fromEpochMilliseconds(time) < 5.minutes) {
                        return Pair(latitude, longitude)
                    }
                }
                // TODO Manage this token as necessary
                val cancellationToken = CancellationTokenSource().token
                location = locationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationToken,
                ).await()
                location?.let {
                    val coordinates = Pair(it.latitude, it.longitude)
                    this.coordinates = coordinates
                    return coordinates
                }
            } catch (e: Exception) {
                logger.logException(e)
            }
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (permissionManager.hasLocationPermission) {
            val interval = intervalMillis.coerceAtLeast(1000)
            val locationRequest = LocationRequest.Builder(interval)
                .setDurationMillis(200)
                .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                .setPriority(Priority.PRIORITY_LOW_POWER)
                .build()
            try {
                locationClient.requestLocationUpdates(locationRequest, this, Looper.getMainLooper())
            } catch (e: Exception) {
                logger.logException(e)
            }
        }
    }

    override fun startObservingLocation() {
        requestLocationUpdates()
    }

    override fun stopObservingLocation() {
        locationClient.removeLocationUpdates(this)
    }

    // LocationListener
    override fun onLocationChanged(location: Location) {
        val timestamp = Instant.fromEpochMilliseconds(location.time)
        cachedLocationTime.value = Triple(location.latitude, location.longitude, timestamp)
    }
}