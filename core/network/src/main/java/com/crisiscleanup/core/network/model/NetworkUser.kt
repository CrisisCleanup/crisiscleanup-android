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
)

@Serializable
data class NetworkUserProfile(
    val id: Long,
    val email: String,
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
) {
    val profilePicUrl: String?
        get() = files?.profilePictureUrl
}
