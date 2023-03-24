package com.crisiscleanup.feature.caseeditor.util


internal fun Collection<String?>.filterNotBlankTrim(): List<String> {
    val notBlank = filter { it?.isNotBlank() == true }.filterNotNull()
    return notBlank.map(String::trim)
}

internal fun Collection<String>.combineTrimText() = filterNotBlankTrim().joinToString(", ")

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
