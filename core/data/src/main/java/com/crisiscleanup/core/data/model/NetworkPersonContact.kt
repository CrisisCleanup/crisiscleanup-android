package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.database.model.PersonOrganizationCrossRef
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.network.model.NetworkPersonContact
import com.crisiscleanup.core.network.model.profilePictureUrl

fun NetworkPersonContact.asEntity() = PersonContactEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    mobile = mobile,
    profilePictureUri = files?.profilePictureUrl ?: "",
    activeRoles = activeRoles.joinToString(","),
)

fun NetworkPersonContact.asExternalModel() = PersonContact(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    mobile = mobile,
    profilePictureUri = files?.profilePictureUrl ?: "",
    activeRoles = activeRoles,
)

fun NetworkPersonContact.asEntities() = organization?.let {
    val organizationEntity = IncidentOrganizationEntity(
        id = it.id,
        name = it.name,
        primaryLocation = null,
        secondaryLocation = null,
    )
    val personContact = asEntity()
    val personToOrganization = PersonOrganizationCrossRef(id, it.id)
    PersonContactEntities(
        organization = organizationEntity,
        organizationAffiliates = it.affiliates,
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
