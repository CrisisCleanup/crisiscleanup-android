package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.database.model.PersonOrganizationCrossRef
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.network.model.NetworkPersonContact

fun NetworkPersonContact.asEntity() = PersonContactEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    mobile = mobile,
)

fun NetworkPersonContact.asExternalModel() = PersonContact(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    mobile = mobile,
)

fun NetworkPersonContact.asEntities(): PersonContactEntities {
    val organizationEntity = IncidentOrganizationEntity(
        id = organization!!.id,
        name = organization!!.name,
    )
    val personContact = asEntity()
    val personToOrganization = PersonOrganizationCrossRef(id, organization!!.id)
    return PersonContactEntities(
        organization = organizationEntity,
        organizationAffiliates = organization!!.affiliates,
        personContact = personContact,
        personToOrganization = personToOrganization,
    )
}

data class PersonContactEntities(
    val organization: IncidentOrganizationEntity,
    val organizationAffiliates: Collection<Long>,
    val personContact: PersonContactEntity,
    val personToOrganization: PersonOrganizationCrossRef,
)