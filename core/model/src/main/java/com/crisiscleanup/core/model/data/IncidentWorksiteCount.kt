package com.crisiscleanup.core.model.data

data class IncidentIdWorksiteCount(
    val id: Long,
    val totalCount: Int,
    /**
     * Number of worksites after filter is applied
     */
    val filteredCount: Int,
)