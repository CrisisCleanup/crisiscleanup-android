package com.crisiscleanup.core.addresssearch

import android.content.Context
import android.location.Geocoder
import android.util.LruCache
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.model.data.LocationAddress
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

class GooglePlaceAddressSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : AddressSearchRepository {
    private val geocoder = Geocoder(context)

    private val placesClientMutex = Mutex()
    private var placesClient: PlacesClient? = null

    private val staleResultDuration = 1.hours

    // TODO Use configurable maxSize
    private val placeAutocompleteResultCache =
        LruCache<String, Pair<Instant, Collection<AutocompletePrediction>>>(30)
    private val addressResultCache =
        LruCache<String, Pair<Instant, Collection<LocationAddress>>>(30)

    override fun clearCache() {
        addressResultCache.evictAll()
    }

    private suspend fun mapPredictionsToAddress(predictions: Collection<AutocompletePrediction>) =
        coroutineScope {
            return@coroutineScope predictions.mapNotNull {
                val placeText = it.getPrimaryText(null).toString()
                val addresses = geocoder.getFromLocationName(placeText, 1)

                ensureActive()

                if (addresses?.isNotEmpty() != true) {
                    return@mapNotNull null
                }

                val address = addresses[0]

                val addressLine = address.getAddressLine(0) ?: ""
                val streetAddress =
                    if (addressLine.isNotEmpty()) addressLine.split(",")[0]
                    else address.thoroughfare ?: ""

                val countyLine = address.subAdminArea ?: ""
                val county = if (countyLine.contains(" County")) {
                    countyLine.subSequence(0, countyLine.indexOf(" County")).toString()
                } else countyLine

                LocationAddress(
                    latitude = address.latitude,
                    longitude = address.longitude,
                    address = streetAddress,
                    city = address.locality ?: "",
                    county = county,
                    state = address.adminArea ?: "",
                    zipCode = address.postalCode ?: "",
                )
            }
        }

    override suspend fun searchAddresses(
        query: String,
        countryCodes: List<String>,
        center: LatLng?,
        southwest: LatLng?,
        northeast: LatLng?
    ): Collection<LocationAddress> = coroutineScope {
        val now = Clock.System.now()

        addressResultCache.get(query)?.let {
            if (now - it.first < staleResultDuration) {
                return@coroutineScope it.second
            }
        }

        placeAutocompleteResultCache.get(query)?.let {
            if (now - it.first < staleResultDuration) {
                val searchResults = mapPredictionsToAddress(it.second)
                addressResultCache.put(query, Pair(it.first, searchResults))
                return@coroutineScope searchResults
            }
        }

        val hasBounds = southwest != null && northeast != null
        val bounds =
            if (hasBounds) RectangularBounds.newInstance(southwest!!, northeast!!) else null
        val request = FindAutocompletePredictionsRequest.builder()
            // Call either setLocationBias() OR setLocationRestriction().
            .setLocationBias(bounds)
            //.setLocationRestriction(bounds)
            .setOrigin(center)
            .setCountries(countryCodes)
            .setTypesFilter(listOf(TypeFilter.ADDRESS.toString().lowercase()))
            .setQuery(query)
            .build()
        try {
            placesClientMutex.withLock {
                if (placesClient == null) {
                    Places.initialize(context, BuildConfig.MAPS_API_KEY)
                    placesClient = Places.createClient(context)
                }
            }
            val response = placesClient!!.findAutocompletePredictions(request).await()
            val predictions = response.autocompletePredictions
            placeAutocompleteResultCache.put(query, Pair(now, predictions))

            val searchResults = mapPredictionsToAddress(predictions)
            addressResultCache.put(query, Pair(now, searchResults))

            return@coroutineScope searchResults
        } catch (e: Exception) {
            if (e !is ApiException && e !is CancellationException) {
                logger.logException(e)
            }
        }

        return@coroutineScope emptyList()
    }
}