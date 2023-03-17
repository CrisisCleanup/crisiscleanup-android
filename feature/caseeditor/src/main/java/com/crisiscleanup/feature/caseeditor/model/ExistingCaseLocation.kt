package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.network.model.NetworkWorksiteLocationSearch

data class ExistingCaseLocation(
    val networkWorksiteId: Long,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val county: String,
)

fun NetworkWorksiteLocationSearch.asCaseLocation() = ExistingCaseLocation(
    networkWorksiteId = id,
    name = name,
    address = address,
    city = city,
    state = state,
    zipCode = postalCode ?: "",
    county = county,
)