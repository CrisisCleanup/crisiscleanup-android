package com.crisiscleanup.core.addresssearch.model

import android.location.Address
import com.crisiscleanup.core.model.data.LocationAddress

fun Address.asLocationAddress(): LocationAddress {
    val addressLine = getAddressLine(0) ?: ""
    val streetAddress =
        if (addressLine.isNotBlank()) {
            addressLine.split(",")[0]
        } else {
            thoroughfare ?: ""
        }

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
