package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.NetworkOrganizationShortDeserializer
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
    @Serializable(NetworkOrganizationShortDeserializer::class)
    val organization: NetworkOrganizationShort?,
    @SerialName("active_roles")
    val activeRoles: Set<Int>?,
    @SerialName("internal_state")
    val internalState: NetworkProfileInternalState?,
)

@Serializable
data class NetworkProfileInternalState(
    @SerialName("incidents")
    val incidentThresholdLookup: Map<String, NetworkIncidentClaimThreshold>,
)

@Serializable
data class NetworkIncidentClaimThreshold(
    @SerialName("claimed_work_type_count")
    val claimedCount: Int?,
    @SerialName("claimed_work_type_closed_ratio")
    val closedRatio: Float?,
)
