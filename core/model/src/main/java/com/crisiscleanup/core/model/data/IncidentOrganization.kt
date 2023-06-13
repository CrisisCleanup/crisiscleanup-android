package com.crisiscleanup.core.model.data

data class IncidentOrganization(
    val id: Long,
    val name: String,
    val primaryContacts: List<PersonContact>,
    val affiliateIds: Set<Long>,
)

data class OrganizationIdName(
    val id: Long,
    val name: String,
)
