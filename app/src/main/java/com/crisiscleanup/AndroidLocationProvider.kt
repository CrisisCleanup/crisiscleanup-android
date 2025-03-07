package com.crisiscleanup

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration

class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : LocationProvider {
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    override var coordinates: Pair<Double, Double>? = null

    override suspend fun getLocation(timeout: Duration) = withContext(ioDispatcher) {
        val locationPermission = ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION)
        if (locationPermission == PackageManager.PERMISSION_GRANTED) {
            val cancelSource = CancellationTokenSource()
            try {
                return@withContext withTimeoutOrNull(timeout) {
                    val task = locationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancelSource.token,
                    )
                    task.await()?.let {
                        val coordinates = Pair(it.latitude, it.longitude)
                        this@AndroidLocationProvider.coordinates = coordinates
                        coordinates
                    }
                }
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                cancelSource.cancel()
            }
        }

        null
    }
}
