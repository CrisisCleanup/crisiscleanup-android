package com.crisiscleanup.core.model.data

data class WorksiteMapMark(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val statusClaim: WorkTypeStatusClaim,
    val workType: WorkTypeType,
    val workTypeCount: Int,
    val isFavorite: Boolean = false,
    val isHighPriority: Boolean = false,
    val isDuplicate: Boolean = false,
    /**
     * Is this mark excluded by filters
     *
     * TRUE does not render the mark or renders it with less significance
     * FALSE renders the mark as usual
     */
    val isFilteredOut: Boolean = false,
)
