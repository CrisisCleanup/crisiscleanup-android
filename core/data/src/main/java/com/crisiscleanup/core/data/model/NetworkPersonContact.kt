package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.PersonContactEntity
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