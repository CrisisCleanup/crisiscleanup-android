package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkEvent(
    val id: Long,
    @SerialName("attr")
    val attr: Map<String, String?>,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("created_by")
    val createdBy: Long,
    @SerialName("event_key")
    val eventKey: String,
    @SerialName("patient_id")
    val patientId: Long?,
    @SerialName("patient_model")
    val patientModel: String?,
    @SerialName("event")
    val event: Description,
) {
    @Serializable
    data class Description(
        @SerialName("event_key")
        val eventKey: String,
        @SerialName("event_description_t")
        val eventDescriptionT: String,
        @SerialName("event_name_t")
        val eventNameT: String,
    )
}