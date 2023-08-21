package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.addresssearch.AddressSearchRepository
import com.crisiscleanup.core.common.throttleLatest
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs

class WrongLocationFlagManager(
    addressSearchRepository: AddressSearchRepository,
    coroutineScope: CoroutineScope,
) {

    private val isParsingCoordinates = MutableStateFlow(false)
    private val isVerifyingCoordinates = MutableStateFlow(false)
    val isProcessingLocation = combine(
        isParsingCoordinates,
        isVerifyingCoordinates,
    ) { b0, b1 -> b0 || b1 }
        .stateIn(
            coroutineScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val coordinatesRegex = """(-?\d{1,2}(?:\.\d+)?),\s*(-?\d{1,3}(?:\.\d+)?)\b""".toRegex()
    val wrongLocationText = MutableStateFlow("")
    private val wrongLocationCoordinatesParse = wrongLocationText
        .throttleLatest(150)
        .mapLatest { s ->
            isParsingCoordinates.value = true
            try {
                coordinatesRegex.find(s)?.let { match ->
                    val (latitudeS, longitudeS) = match.destructured
                    val latitude = latitudeS.toDoubleOrNull()
                    val longitude = longitudeS.toDoubleOrNull()
                    if (latitude != null && abs(latitude) <= 90.0 &&
                        longitude != null && abs(longitude) < 180.0
                    ) {
                        return@mapLatest LatLng(latitude, longitude)
                    }
                }
            } finally {
                isParsingCoordinates.value = false
            }
            null
        }
        .stateIn(
            coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    val validCoordinates = wrongLocationCoordinatesParse.mapLatest {
        it?.let { latLng ->
            isVerifyingCoordinates.value = true
            try {
                val results = addressSearchRepository.getAddress(latLng)
                results?.let { address ->
                    return@mapLatest address
                }
            } finally {
                isVerifyingCoordinates.value = false
            }
        }
    }
        .stateIn(
            coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )
}
