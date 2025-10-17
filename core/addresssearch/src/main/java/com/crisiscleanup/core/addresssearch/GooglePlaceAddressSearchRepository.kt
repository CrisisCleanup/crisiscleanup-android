package com.crisiscleanup.core.addresssearch

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.LruCache
import com.crisiscleanup.core.addresssearch.model.KeySearchAddress
import com.crisiscleanup.core.addresssearch.model.asLocationAddress
import com.crisiscleanup.core.common.AppSettingsProvider
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.model.data.LocationAddress
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class GooglePlaceAddressSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
    private val settingsProvider: AppSettingsProvider,
) : AddressSearchRepository {
    private val geocoder = Geocoder(context)

    private val placesClientMutex = Mutex()
    private var placesClientInternal: PlacesClient? = null
    private suspend fun placesClient(): PlacesClient {
        placesClientMutex.withLock {
            if (placesClientInternal == null) {
                Places.initializeWithNewPlacesApiEnabled(
                    context,
                    settingsProvider.mapsApiKey,
                )
                placesClientInternal = Places.createClient(context)
            }
        }
        return placesClientInternal!!
    }

    private val staleResultDuration = 1.hours

    private val sessionTokenAr = AtomicReference<AutocompleteSessionToken>()

    // TODO Use configurable maxSize
    private val placeAutocompleteResultCache =
        LruCache<String, Pair<Instant, List<AutocompletePrediction>>>(30)

    override fun clearCache() {
        placeAutocompleteResultCache.evictAll()
    }

    private fun mapPredictionsToAddress(predictions: Collection<AutocompletePrediction>) =
        predictions.map { prediction ->
            KeySearchAddress(
                prediction.placeId,
                prediction.getPrimaryText(null).toString(),
                prediction.getSecondaryText(null).toString(),
                prediction.getFullText(null).toString(),
            )
        }

    override suspend fun getAddress(coordinates: LatLng): LocationAddress? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val results = geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1)
            return results?.firstOrNull()?.asLocationAddress()?.copy(
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
            )
        } else {
            return suspendCancellableCoroutine { continuation ->
                val listener = Geocoder.GeocodeListener { results ->
                    val firstAddress = results.firstOrNull()?.asLocationAddress()?.copy(
                        latitude = coordinates.latitude,
                        longitude = coordinates.longitude,
                    )
                    continuation.resume(firstAddress)
                }
                try {
                    geocoder.getFromLocation(
                        coordinates.latitude,
                        coordinates.longitude,
                        1,
                        listener,
                    )
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    override fun startSearchSession() {
        val token = AutocompleteSessionToken.newInstance()
        sessionTokenAr.set(token)
    }

    private fun getSessionToken(): AutocompleteSessionToken {
        synchronized(sessionTokenAr) {
            sessionTokenAr.get()?.let {
                return it
            }
            val token = AutocompleteSessionToken.newInstance()
            sessionTokenAr.set(token)
            return token
        }
    }

    override suspend fun searchAddresses(
        query: String,
        countryCodes: List<String>,
        center: LatLng?,
        southwest: LatLng?,
        northeast: LatLng?,
        maxResults: Int,
    ): List<KeySearchAddress> = coroutineScope {
        val now = Clock.System.now()

        placeAutocompleteResultCache.get(query)?.let { (cacheTime, cacheResults) ->
            if (now - cacheTime < staleResultDuration) {
                return@coroutineScope mapPredictionsToAddress(cacheResults)
            }
        }

        val hasBounds = southwest != null && northeast != null
        val bounds = if (hasBounds) RectangularBounds.newInstance(southwest, northeast) else null
        val request = FindAutocompletePredictionsRequest.builder()
            // Call either setLocationBias() OR setLocationRestriction().
            .setLocationBias(bounds)
            // .setLocationRestriction(bounds)
            .setOrigin(center)
            .setCountries(countryCodes)
            .setTypesFilter(listOf(PlaceTypes.ADDRESS))
            .setQuery(query)
            .setSessionToken(getSessionToken())
            .build()
        try {
            val response = placesClient().findAutocompletePredictions(request).await()
            val predictions = response.autocompletePredictions
            placeAutocompleteResultCache.put(query, Pair(now, predictions))

            ensureActive()

            return@coroutineScope mapPredictionsToAddress(predictions)
        } catch (e: Exception) {
            if (e !is ApiException && e !is CancellationException) {
                logger.logException(e)
            }
        }

        return@coroutineScope emptyList()
    }

    override suspend fun getPlaceAddress(placeId: String): LocationAddress? {
        val sessionToken = getSessionToken()
        val placeFields = listOf(
            Place.Field.LOCATION,
            Place.Field.ADDRESS_COMPONENTS,
        )

        try {
            val request = FetchPlaceRequest.builder(placeId, placeFields)
                .setSessionToken(sessionToken)
                .build()
            val response = placesClient().fetchPlace(request).await()
            val addressTypeKeys = setOf(
                "subpremise",
                "street_number",
                "route",
                "locality",
                "administrative_area_level_2",
                "administrative_area_level_1",
                "country",
                "postal_code",
            )
            with(response.place) {
                location?.let { coordinates ->
                    addressComponents?.asList()?.let { components ->
                        val addressComponentLookup = mutableMapOf<String, String>()
                        components.forEach {
                            for (t in it.types) {
                                if (addressTypeKeys.contains(t)) {
                                    addressComponentLookup[t] = it.name
                                }
                            }
                        }

                        startSearchSession()

                        return LocationAddress(
                            latitude = coordinates.latitude,
                            longitude = coordinates.longitude,
                            address = listOf(
                                addressComponentLookup["street_number"],
                                addressComponentLookup["route"],
                                addressComponentLookup["subpremise"],
                            ).combineTrimText(),
                            city = addressComponentLookup["locality"] ?: "",
                            county = addressComponentLookup["administrative_area_level_2"] ?: "",
                            state = addressComponentLookup["administrative_area_level_1"] ?: "",
                            zipCode = addressComponentLookup["postal_code"] ?: "",
                            country = addressComponentLookup["country"] ?: "",
                        )
                    }
                }
            }
        } catch (e: Exception) {
            if (e !is ApiException && e !is CancellationException) {
                logger.logException(e)
            }
        }

        return null
    }
}
