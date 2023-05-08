package com.crisiscleanup.core.model.data

data class WorksiteSummary(
    /**
     * Local worksite ID
     *
     * This may be set to 0 if lookup was not performed.
     * Perform lookup of worksite with certainty using [networkId].
     */
    val id: Long,
    val networkId: Long,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val county: String,
    val caseNumber: String,
    val workType: WorkType?,
)
