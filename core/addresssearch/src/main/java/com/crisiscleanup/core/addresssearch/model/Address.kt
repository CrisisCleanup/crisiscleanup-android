package com.crisiscleanup.core.addresssearch.model

import android.location.Address
import com.crisiscleanup.core.model.data.LocationAddress

fun List<Address?>.filterLatLng(): List<Address> {
    return mapNotNull {
        if (it != null && it.hasLatitude() && it.hasLongitude()) {
            it
        } else {
            null
        }
    }
}

fun Address.asLocationAddress(): LocationAddress {
    val addressLine = getAddressLine(0) ?: ""
    val streetAddress =
        if (addressLine.isNotBlank()) addressLine.split(",")[0]
        else thoroughfare ?: ""

    val county = subAdminArea ?: ""

    return LocationAddress(
        latitude = latitude,
        longitude = longitude,
        address = streetAddress,
        city = locality ?: "",
        county = county,
        state = adminArea ?: "",
        country = countryName ?: "",
        zipCode = postalCode ?: "",
    )
}

fun Address.asKeyLocationAddress(key: String) = KeyLocationAddress(
    key = key,
    address = asLocationAddress(),
)
