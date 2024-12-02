package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.network.model.NetworkOrganizationUser

fun NetworkOrganizationUser.asEntity() = PersonContactEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    mobile = mobile,
    profilePictureUri = profilePictureUrl ?: "",
    activeRoles = activeRoles?.joinToString(",") ?: "",
)
