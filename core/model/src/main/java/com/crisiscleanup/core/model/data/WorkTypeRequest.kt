package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class WorkTypeRequest(
    val workType: String,
    val byOrg: Long,
    val createdAt: Instant,
    val approvedAt: Instant?,
    val rejectedAt: Instant?,
    val approvedRejectedReason: String,
)
