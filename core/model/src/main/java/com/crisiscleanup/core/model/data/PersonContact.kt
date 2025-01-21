package com.crisiscleanup.core.model.data

data class PersonContact(
    val id: Long,
    private val firstName: String,
    private val lastName: String,
    val email: String,
    val mobile: String,
    val profilePictureUri: String,
    private val fallbackAvatarUrl: String,
    val activeRoles: List<Int>,
    val fullName: String = "$firstName $lastName".trim(),
) {
    val avatarUrl: String
        get() = profilePictureUri.ifBlank { fallbackAvatarUrl }
}

val EmptyPersonContact = PersonContact(
    -1,
    "",
    "",
    "",
    "",
    "",
    "",
    emptyList(),
)

data class PersonOrganization(
    val person: PersonContact,
    val organization: OrganizationIdName,
)
