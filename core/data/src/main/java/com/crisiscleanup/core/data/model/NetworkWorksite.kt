package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.WorksiteEntities
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import com.crisiscleanup.core.network.model.KeyDynamicValuePair
import com.crisiscleanup.core.network.model.NetworkFlag
import com.crisiscleanup.core.network.model.NetworkNote
import com.crisiscleanup.core.network.model.NetworkWorkType
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksiteShort

fun NetworkWorksiteFull.asEntity(incidentId: Long) = WorksiteEntity(
    id = 0,
    networkId = id,
    incidentId = incidentId,
    address = address,
    autoContactFrequencyT = autoContactFrequencyT,
    caseNumber = caseNumber,
    city = city,
    county = county,
    email = email,
    favoriteId = favorite?.id,
    keyWorkTypeType = newestKeyWorkType?.workType ?: "",
    keyWorkTypeOrgClaim = newestKeyWorkType?.orgClaim,
    keyWorkTypeStatus = newestKeyWorkType?.status ?: "",
    latitude = location.coordinates[1],
    longitude = location.coordinates[0],
    name = name,
    phone1 = phone1,
    phone2 = phone2,
    plusCode = plusCode,
    postalCode = postalCode ?: "",
    reportedBy = reportedBy,
    state = state,
    svi = svi,
    what3Words = what3words,
    updatedAt = updatedAt,
)

fun NetworkWorksiteShort.asEntity(incidentId: Long) = WorksiteEntity(
    id = 0,
    networkId = id,
    incidentId = incidentId,
    address = address,
    caseNumber = caseNumber,
    city = city,
    county = county,
    createdAt = createdAt,
    favoriteId = favoriteId,
    keyWorkTypeType = newestKeyWorkType?.workType ?: "",
    keyWorkTypeOrgClaim = newestKeyWorkType?.orgClaim,
    keyWorkTypeStatus = newestKeyWorkType?.status ?: "",
    latitude = location.coordinates[1],
    longitude = location.coordinates[0],
    name = name,
    postalCode = postalCode ?: "",
    state = state,
    svi = svi,
    updatedAt = updatedAt,

    autoContactFrequencyT = null,
    email = null,
    phone1 = null,
    phone2 = null,
    plusCode = null,
    reportedBy = null,
    what3Words = null,
)

fun KeyDynamicValuePair.asWorksiteEntity() = WorksiteFormDataEntity(
    worksiteId = 0,
    fieldKey = key,
    isBoolValue = value.isBoolean,
    valueString = value.valueString,
    valueBool = value.valueBoolean,
)

fun NetworkFlag.asEntity() = WorksiteFlagEntity(
    id = 0,
    worksiteId = 0,
    // Incoming network ID is always defined
    networkId = id!!,
    action = action,
    createdAt = createdAt,
    isHighPriority = isHighPriority,
    notes = notes,
    reasonT = reasonT,
    requestedAction = requestedAction,
)

fun NetworkNote.asEntity() = WorksiteNoteEntity(
    id = 0,
    localGlobalUuid = "",
    worksiteId = 0,
    // Incoming network ID is always defined
    networkId = id!!,
    createdAt = createdAt,
    isSurvivor = isSurvivor,
    note = note ?: "",
)

fun NetworkWorksiteFull.asEntities(incidentId: Long): WorksiteEntities {
    val core = asEntity(incidentId)
    val workTypes = newestWorkTypes.map(NetworkWorkType::asEntity)
    val formData = formData.map(KeyDynamicValuePair::asWorksiteEntity)
    val flags = flags.map(NetworkFlag::asEntity)
    val notes = notes.map(NetworkNote::asEntity)
    return WorksiteEntities(
        core,
        flags,
        formData,
        notes,
        workTypes,
    )
}
