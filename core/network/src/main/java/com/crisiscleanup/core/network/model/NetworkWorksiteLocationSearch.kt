package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkWorksiteLocationSearch(
    @SerialName("incident")
    val incidentId: Long,
    val id: Long,
    val address: String,
    @SerialName("case_number")
    val caseNumber: String,
    val city: String,
    val county: String,
    @SerialName("key_work_type")
    val keyWorkType: NetworkWorkType,
    val location: NetworkLocation.LocationPoint,
    val name: String,
    @SerialName("postal_code")
    val postalCode: String?,
    val state: String,
)
