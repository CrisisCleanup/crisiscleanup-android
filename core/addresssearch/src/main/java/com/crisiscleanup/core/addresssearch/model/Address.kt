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

fun Address.toKeyLocationAddress(key: String): KeyLocationAddress {
    val addressLine = getAddressLine(0) ?: ""
    val streetAddress =
        if (addressLine.isNotEmpty()) addressLine.split(",")[0]
        else thoroughfare ?: ""

    val countyLine = subAdminArea ?: ""
    val county = if (countyLine.contains(" County")) {
        countyLine.subSequence(0, countyLine.indexOf(" County")).toString()
    } else countyLine

    val locationAddress = LocationAddress(
        latitude = latitude,
        longitude = longitude,
        address = streetAddress,
        city = locality ?: "",
        county = county,
        state = adminArea ?: "",
        country = countryName ?: "",
        zipCode = postalCode ?: "",
    )

    return KeyLocationAddress(
        key = key,
        address = locationAddress,
    )
}