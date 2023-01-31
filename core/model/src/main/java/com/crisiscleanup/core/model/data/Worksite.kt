package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class Worksite(
    val id: Long,
    val address: String,
    val caseNumber: String,
    val city: String,
    val county: String,
    val createdAt: Instant?,
    val email: String? = null,
    val favoriteId: Long?,
    val incident: Long,
    val keyWorkType: WorkType?,
    val location: LatLng,
    val name: String,
    val phone1: String,
    val phone2: String,
    val plusCode: String? = null,
    val postalCode: String,
    val reportedBy: Long?,
    val state: String,
    val svi: Float?,
    val updatedAt: Instant?,
    val what3words: String? = null,
    val workTypes: List<WorkType>,
)

data class WorkType(
    val id: Long,
    val createdAt: Instant? = null,
    val orgClaim: Long? = null,
    val nextRecurAt: Instant? = null,
    val phase: Int? = null,
    val recur: String? = null,
    val status: String?,
    val workType: String,
)