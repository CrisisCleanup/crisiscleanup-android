package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkOrganizationsResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkIncidentOrganization>? = null,
)

@Serializable
data class NetworkIncidentOrganization(
    val id: Long,
    val name: String,
    val affiliates: Collection<Long>,
    @SerialName("primary_location")
    val primaryLocation: Long?,
    @SerialName("type_t")
    val typeT: String?,
    @SerialName("primary_contacts")
    val primaryContacts: Collection<NetworkPersonContact>?,
)

@Serializable
data class NetworkOrganizationShort(
    val id: Long,
    val name: String,
)