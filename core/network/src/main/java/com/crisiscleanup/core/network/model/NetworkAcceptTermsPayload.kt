package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NetworkAcceptTermsPayload(
    @SerialName("accepted_terms")
    val hasAcceptedTerms: Boolean,
    @SerialName("accepted_terms_timestamp")
    val acceptedTermsTimestamp: Instant,
)
