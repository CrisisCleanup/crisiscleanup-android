package com.crisiscleanup.core.model.data

data class WorksiteSummary(
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
