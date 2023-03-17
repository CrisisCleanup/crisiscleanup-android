package com.crisiscleanup

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : LocationProvider {
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    override var coordinates: Pair<Double, Double>? = null

    override suspend fun getLocation(): Pair<Double, Double>? {
        val locationPermission = ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION)
        if (locationPermission == PackageManager.PERMISSION_GRANTED) {
            try {
                var location = locationClient.lastLocation.await()
                location?.let {
                    if (Clock.System.now() - Instant.fromEpochMilliseconds(location.time) < 5.minutes) {
                        return Pair(it.latitude, it.longitude)
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
}