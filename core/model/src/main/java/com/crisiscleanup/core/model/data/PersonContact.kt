package com.crisiscleanup.core.model.data

data class PersonContact(
    val id: Long,
    private val firstName: String,
    private val lastName: String,
    val email: String,
    val mobile: String,
    val profilePictureUri: String,
    val activeRoles: List<Int>,
) {
    val fullName = "$firstName $lastName".trim()
}

data class PersonOrganization(
    val person: PersonContact,
    val organization: OrganizationIdName,
)
