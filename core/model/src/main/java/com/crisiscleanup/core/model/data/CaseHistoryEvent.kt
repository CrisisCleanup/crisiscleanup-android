package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class CaseHistoryUserEvents(
    val userId: Long,
    val userName: String,
    val orgName: String,
    val userPhone: String,
    val userEmail: String,
    val events: List<CaseHistoryEvent>,
)

data class CaseHistoryEvent(
    val id: Long,
    val worksiteId: Long,
    val createdAt: Instant,
    val createdBy: Long,
    val eventKey: String,
    val pastTenseT: String,
    val actorLocationName: String,
    val recipientLocationName: String?,
    val attr: CaseHistoryEventAttr,
)

data class CaseHistoryEventAttr(
    val incidentName: String,
    val patientCaseNumber: String?,
    val patientId: Long,
    val patientLabelT: String?,
    val patientLocationName: String?,
    val patientNameT: String?,
    val patientStatusNameT: String?,
    val recipientCaseNumber: String?,
    val recipientId: Long?,
    val recipientName: String?,
)
