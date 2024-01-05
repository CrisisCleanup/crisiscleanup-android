package com.crisiscleanup.core.model.data

data class IncidentOrganization(
    val id: Long,
    val name: String,
    val primaryContacts: List<PersonContact>,
    val affiliateIds: Set<Long>,
) {
    override fun equals(other: Any?): Boolean {
        (other as? IncidentOrganization)?.let {
            return id == it.id
        }
        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class OrganizationIdName(
    val id: Long,
    val name: String,
)
