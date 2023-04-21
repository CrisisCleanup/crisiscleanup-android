package com.crisiscleanup.feature.caseeditor.util

import com.crisiscleanup.core.common.combineTrimText

internal fun summarizeAddress(
    streetAddress: String,
    zipCode: String,
    county: String,
    city: String,
    state: String,
) = listOf(
    streetAddress,
    listOf(city, state, county).combineTrimText(),
    zipCode,
).filter(String::isNotBlank)
