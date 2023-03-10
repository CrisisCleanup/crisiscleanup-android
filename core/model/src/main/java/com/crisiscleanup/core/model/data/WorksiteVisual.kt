package com.crisiscleanup.core.model.data

data class WorksiteMapMark(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val statusClaim: WorkTypeStatusClaim,
    val workType: WorkTypeType,
    val workTypeCount: Int,
)
