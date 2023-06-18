package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkShareDetails(
    val emails: List<String>,
    @SerialName("phone_numbers")
    val phoneNumbers: List<String>,
    @SerialName("share_message")
    val shareMessage: String,
    @SerialName("no_claim_reason_text")
    val noClaimReason: String?
)
