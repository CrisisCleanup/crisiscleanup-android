package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkCaseHistoryResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val events: List<NetworkCaseHistoryEvent>? = null
)

@Serializable
data class NetworkCaseHistoryEvent(
    val id: Long,
    @SerialName("event_key")
    val eventKey: String,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("past_tense_t")
    val pastTenseT: String,
    @SerialName("actor_location_name")
    val actorLocationName: String,
    val attr: NetworkCaseHistoryAttrs,
)

@Serializable
data class NetworkCaseHistoryAttrs(
    @SerialName("patient_case_number")
    val patientCaseNumber: String,
    @SerialName("incident_name")
    val incidentName: String,
    @SerialName("patient_label_t")
    val patientLabelT: String? = null,
    @SerialName("recipient_case_number")
    val recipientCaseNumber: String? = null,
)