package com.crisiscleanup.core.model.data

import kotlin.time.Instant

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
    val pastTenseDescription: String,
    val actorLocationName: String,
    val recipientLocationName: String?,
)
