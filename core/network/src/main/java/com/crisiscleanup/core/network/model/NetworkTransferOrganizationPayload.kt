package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkTransferOrganizationPayload(
    @SerialName("transfer_action")
    val action: String,
    @SerialName("invitation_token")
    val token: String,
)

@Serializable
data class NetworkTransferOrganizationResult(
    val status: String,
)
