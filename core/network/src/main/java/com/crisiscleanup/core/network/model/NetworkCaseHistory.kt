package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NetworkCaseHistoryResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val events: List<NetworkCaseHistoryEvent>? = null,
)

@Serializable
data class NetworkCaseHistoryEvent(
    val id: Long,
    @SerialName("event_key")
    val eventKey: String,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("created_by")
    val createdBy: Long,
    @SerialName("past_tense_t")
    val pastTenseT: String,
    @SerialName("actor_location_name")
    val actorLocationName: String? = null,
    @SerialName("recipient_location_name")
    val recipientLocationName: String? = null,
    val attr: NetworkCaseHistoryAttrs,
)

@Serializable
data class NetworkCaseHistoryAttrs(
    @SerialName("incident_name")
    val incidentName: String,
    @SerialName("patient_case_number")
    val patientCaseNumber: String? = null,
    @SerialName("patient_id")
    val patientId: Long? = null,
    @SerialName("patient_label_t")
    val patientLabelT: String? = null,
    @SerialName("patient_location_name")
    val patientLocationName: String? = null,
    @SerialName("patient_name_t")
    val patientNameT: String? = null,
    @SerialName("patient_reason_t")
    val patientReasonT: String? = null,
    @SerialName("patient_status_name_t")
    val patientStatusNameT: String? = null,
    @SerialName("recipient_case_number")
    val recipientCaseNumber: String? = null,
    @SerialName("recipient_id")
    val recipientId: Long? = null,
    @SerialName("recipient_name")
    val recipientName: String? = null,
    @SerialName("recipient_name_t")
    val recipientNameT: String? = null,
)
