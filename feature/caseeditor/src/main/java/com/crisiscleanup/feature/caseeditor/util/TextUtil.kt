package com.crisiscleanup.feature.caseeditor.util


internal fun combineTrimText(vararg texts: String) =
    texts.filter(String::isNotBlank).joinToString(", ", transform = String::trim)

internal fun summarizeAddress(
    streetAddress: String,
    zipCode: String,
    county: String,
    city: String,
    state: String,
) = listOf(
    combineTrimText(streetAddress),
    combineTrimText(city, state, county),
    combineTrimText(zipCode),
).filter(String::isNotBlank)
