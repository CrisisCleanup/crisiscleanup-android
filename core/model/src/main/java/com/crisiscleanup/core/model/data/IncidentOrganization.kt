package com.crisiscleanup.core.model.data

data class IncidentOrganization(
    val id: Long,
    val name: String,
    val primaryContacts: List<PersonContact>,
)
