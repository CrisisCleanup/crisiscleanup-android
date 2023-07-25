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
val Worksite.addressQuery: Pair<String, String>
    get() {
        // TODO Alert if wrong address is checked? Just for context?
        val addressText = fullAddress
        val locationQuery = "geo:${latitude},${longitude}?q=$addressText"
        return Pair(addressText, locationQuery)
    }