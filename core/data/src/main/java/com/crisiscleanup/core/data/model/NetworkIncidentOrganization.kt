package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.OrganizationAffiliateEntity
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.network.model.NetworkIncidentOrganization
import com.crisiscleanup.core.network.model.NetworkPersonContact

fun NetworkIncidentOrganization.asEntity() = IncidentOrganizationEntity(
    id = id,
    name = name,
)

fun NetworkIncidentOrganization.primaryContactCrossReferences() =
    primaryContacts?.map { OrganizationPrimaryContactCrossRef(id, it.id) } ?: emptyList()

fun NetworkIncidentOrganization.affiliateOrganizationCrossReferences() =
    affiliates.map { OrganizationAffiliateEntity(id, it) }

fun Collection<NetworkIncidentOrganization>.asEntities(
    getContacts: Boolean,
    getReferences: Boolean,
): OrganizationEntities {
    val organizations = map { it.asEntity() }
    val primaryContacts =
        if (getContacts) flatMap {
            it.primaryContacts?.map(NetworkPersonContact::asEntity) ?: emptyList()
        }
        else emptyList()
    val organizationContactCrossRefs =
        if (getReferences) flatMap(NetworkIncidentOrganization::primaryContactCrossReferences)
        else emptyList()
    val organizationAffiliates =
        if (getReferences) flatMap(NetworkIncidentOrganization::affiliateOrganizationCrossReferences)
        else emptyList()
    return OrganizationEntities(
        organizations,
        primaryContacts,
        organizationContactCrossRefs,
        organizationAffiliates,
    )
}

data class OrganizationEntities(
    val organizations: List<IncidentOrganizationEntity>,
    val primaryContacts: List<PersonContactEntity>,
    val organizationContactCrossRefs: List<OrganizationPrimaryContactCrossRef>,
    val orgAffiliates: List<OrganizationAffiliateEntity>,
)