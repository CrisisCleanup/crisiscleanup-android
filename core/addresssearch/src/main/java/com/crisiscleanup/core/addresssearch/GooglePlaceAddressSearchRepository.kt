package com.crisiscleanup.core.addresssearch

import android.content.Context
import android.location.Geocoder
import android.util.LruCache
import com.crisiscleanup.core.addresssearch.model.KeyLocationAddress
import com.crisiscleanup.core.addresssearch.model.asKeyLocationAddress
import com.crisiscleanup.core.addresssearch.model.filterLatLng
import com.crisiscleanup.core.addresssearch.util.sort
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
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
    private var _placesClient: PlacesClient? = null
    private suspend fun placesClient(): PlacesClient {
        placesClientMutex.withLock {
            if (_placesClient == null) {
                Places.initialize(context, BuildConfig.MAPS_API_KEY)
                _placesClient = Places.createClient(context)
            }
        }
        return _placesClient!!
    }

    private val staleResultDuration = 1.hours

    // TODO Use configurable maxSize
    private val placeAutocompleteResultCache =
        LruCache<String, Pair<Instant, List<AutocompletePrediction>>>(30)
    private val addressResultCache =
        LruCache<String, Pair<Instant, List<KeyLocationAddress>>>(30)

    override fun clearCache() {
        placeAutocompleteResultCache.evictAll()
        addressResultCache.evictAll()
    }

    private suspend fun mapPredictionsToAddress(predictions: Collection<AutocompletePrediction>) =
        coroutineScope {
            return@coroutineScope predictions.mapNotNull { prediction ->
                val placeText = prediction.getPrimaryText(null).toString()
                val addresses = geocoder.getFromLocationName(placeText, 1)

                ensureActive()

                addresses?.filterLatLng()
                    ?.firstOrNull()
                    ?.asKeyLocationAddress(prediction.placeId)
            }
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

        placeAutocompleteResultCache.get(query)?.let { (cacheTime, cacheResults) ->
            if (now - cacheTime < staleResultDuration) {
                return@coroutineScope mapPredictionsToAddress(cacheResults)
                    .sort(center)
                    .also { addressResultCache.put(query, Pair(cacheTime, it)) }
            }
        }

        val hasBounds = southwest != null && northeast != null
        val bounds =
            if (hasBounds) RectangularBounds.newInstance(southwest!!, northeast!!) else null
        val request = FindAutocompletePredictionsRequest.builder()
            // Call either setLocationBias() OR setLocationRestriction().
            .setLocationBias(bounds)
            // .setLocationRestriction(bounds)
            .setOrigin(center)
            .setCountries(countryCodes)
            .setTypesFilter(listOf(TypeFilter.ADDRESS.toString().lowercase()))
            .setQuery(query)
            .build()
        try {
            val response = placesClient().findAutocompletePredictions(request).await()
            val predictions = response.autocompletePredictions
            placeAutocompleteResultCache.put(query, Pair(now, predictions))

            ensureActive()

            return@coroutineScope mapPredictionsToAddress(predictions)
                .sort(center)
                .also { addressResultCache.put(query, Pair(now, it)) }
        } catch (e: Exception) {
            if (e !is ApiException && e !is CancellationException) {
                logger.logException(e)
            }
        }

        return@coroutineScope emptyList()
    }
}
