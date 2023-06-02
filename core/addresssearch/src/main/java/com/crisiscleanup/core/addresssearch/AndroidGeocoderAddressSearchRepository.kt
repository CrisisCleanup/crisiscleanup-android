package com.crisiscleanup.core.addresssearch

import android.content.Context
import android.location.Geocoder
import android.util.LruCache
import com.crisiscleanup.core.addresssearch.model.KeyLocationAddress
import com.crisiscleanup.core.addresssearch.model.asKeyLocationAddress
import com.crisiscleanup.core.addresssearch.model.asLocationAddress
import com.crisiscleanup.core.addresssearch.model.filterLatLng
import com.crisiscleanup.core.addresssearch.util.sort
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.model.data.LocationAddress
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

// Built in geocoder is not that good
class AndroidGeocoderAddressSearchRepository @Inject constructor(
    @ApplicationContext
    private val context: Context,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : AddressSearchRepository {
    private val geocoder = Geocoder(context)

    private val staleResultDuration = 1.hours

    // TODO Use configurable maxSize
    private val addressResultCache =
        LruCache<String, Pair<Instant, List<KeyLocationAddress>>>(30)

    override fun clearCache() {
        addressResultCache.evictAll()
    }

    override suspend fun getAddress(coordinates: LatLng) = geocoder.getAddress(coordinates)

    override suspend fun searchAddresses(
        query: String,
        countryCodes: List<String>,
        center: LatLng?,
        southwest: LatLng?,
        northeast: LatLng?,
        maxResults: Int,
    ): List<KeyLocationAddress> = coroutineScope {
        val now = Clock.System.now()

        addressResultCache.get(query)?.let {
            if (now - it.first < staleResultDuration) {
                return@coroutineScope it.second
            }
        }

        val hasBounds = southwest != null && northeast != null
        try {
            val geocoderAddresses = if (hasBounds) geocoder.getFromLocationName(
                query,
                maxResults,
                southwest!!.latitude,
                southwest.longitude,
                northeast!!.latitude,
                northeast.longitude
            )
            else geocoder.getFromLocationName(query, maxResults)

            val addresses =
                geocoderAddresses
                    ?.filterLatLng()
                    ?.mapIndexed { index, address ->
                        // TODO Rely on unique key
                        val key = address.url ?: (address.getAddressLine(0) ?: "$query-$index")
                        address.asKeyLocationAddress(key)
                    }
                    ?.sort(center)
                    ?.also {
                        addressResultCache.put(query, Pair(now, it))
                    }

            return@coroutineScope addresses ?: emptyList()
        } catch (e: Exception) {
            if (e !is ApiException && e !is CancellationException) {
                logger.logException(e)
            }
        }

        return@coroutineScope emptyList()
    }
}

internal fun Geocoder.getAddress(coordinates: LatLng): LocationAddress? {
    val results = getFromLocation(coordinates.latitude, coordinates.longitude, 1)
    return results?.firstOrNull()?.asLocationAddress()?.copy(
        latitude = coordinates.latitude,
        longitude = coordinates.longitude,
    )
}