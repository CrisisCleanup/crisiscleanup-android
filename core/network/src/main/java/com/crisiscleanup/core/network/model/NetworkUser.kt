package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkUser(
    val id: Long,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val organization: Long,
    val files: List<NetworkFile>,
) {
    val profilePictureUrl by lazy {
        files.profilePictureUrl
    }
}

@Serializable
data class NetworkOrganizationUser(
    val id: Long,
    val email: String,
    val mobile: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val files: List<NetworkFile>,
    @SerialName("active_roles")
    val activeRoles: List<Int>?,
) {
    val profilePictureUrl by lazy {
        files.profilePictureUrl
    }
}

// UPDATE NetworkAccountTest in conjunction with changes here
@Serializable
data class NetworkUserProfile(
    val id: Long,
    val email: String,
    val mobile: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("approved_incidents")
    val approvedIncidents: Set<Long>,
    @SerialName("accepted_terms")
    val hasAcceptedTerms: Boolean?,
    @SerialName("accepted_terms_timestamp")
    val acceptedTermsTimestamp: Instant?,
    val files: List<NetworkFile>?,
    val organization: NetworkOrganizationShort,
    @SerialName("active_roles")
    val activeRoles: Set<Int>,
) {
    val profilePictureUrl by lazy {
        files?.profilePictureUrl
    }
}
