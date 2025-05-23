package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkAccountProfileResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    @SerialName("approved_incidents")
    val approvedIncidents: Set<Long>?,
    @SerialName("accepted_terms")
    val hasAcceptedTerms: Boolean?,
    @SerialName("accepted_terms_timestamp")
    val acceptedTermsTimestamp: Instant?,
    val files: List<NetworkFile>?,
    val organization: NetworkOrganizationShort,
    @SerialName("active_roles")
    val activeRoles: Set<Int>,
)
