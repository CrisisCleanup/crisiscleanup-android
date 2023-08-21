package com.crisiscleanup.core.commoncase.model

import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.model.data.Worksite

val Worksite.fullAddress: String
    get() = listOf(
        address,
        city,
        state,
        postalCode,
    ).combineTrimText()

/**
 * @returns (Human readable address text, Query for maps navigation)
 */
val Worksite.addressQuery: Triple<String, String, String>
    get() {
        val coordinates = "$latitude,$longitude"
        val geoQuery = "geo:0,0?q=$coordinates"
        val addressText = fullAddress
        val locationQuery = "geo:$coordinates?q=$addressText"
        return Triple(addressText, geoQuery, locationQuery)
    }
