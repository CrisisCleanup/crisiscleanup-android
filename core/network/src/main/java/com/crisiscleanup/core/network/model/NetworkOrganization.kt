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
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("primary_location")
    val primaryLocation: Long?,
    @SerialName("secondary_location")
    val secondaryLocation: Long?,
    @SerialName("type_t")
    val typeT: String?,
    @SerialName("primary_contacts")
    val primaryContacts: Collection<NetworkPersonContact>?,
    val incidents: Collection<Long>?,
)

@Serializable
data class NetworkOrganizationShort(
    val id: Long,
    val name: String,
    @SerialName("is_active")
    val isActive: Boolean? = null,
)

@Serializable
data class NetworkOrganizationsSearchResult(
    val errors: List<NetworkCrisisCleanupApiError>?,
    val count: Int?,
    val results: List<NetworkOrganizationShort>?,
)

@Serializable
data class NetworkRegisterOrganizationResult(
    val errors: List<NetworkCrisisCleanupApiError>?,
    val organization: NetworkOrganizationShort,
)

@Serializable
data class NetworkOrganizationRegistration(
    val name: String,
    val referral: String,
    val incident: Long,
    val contact: NetworkOrganizationContact,
)

@Serializable
data class NetworkOrganizationContact(
    val email: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val mobile: String,
    val title: String?,
    val organization: Long?,
)

@Serializable
data class NetworkOrganizationUsersResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkOrganizationUser>? = null,
)
