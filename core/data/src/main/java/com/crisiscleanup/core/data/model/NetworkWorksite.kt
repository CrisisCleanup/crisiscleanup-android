package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.datetime.Instant

fun NetworkWorksiteFull.asEntity(
    incidentId: Long,
    syncUuid: String,
    syncedAt: Instant,
) = WorksiteEntity(
    id = 0,
    syncUuid = syncUuid,
    localModifiedAt = syncedAt,
    syncedAt = syncedAt,
    localGlobalUuid = "",
    isLocalModified = false,
    syncAttempt = 0,

    networkId = id,
    incidentId = incidentId,
    address = address,
    autoContactFrequencyT = autoContactFrequencyT,
    caseNumber = caseNumber,
    city = city,
    county = county,
    email = email,
    // TODO Is this the correct interpretation?
    favoriteId = favorite?.id,
    keyWorkTypeType = keyWorkType?.workType ?: "",
    latitude = location.coordinates[1],
    longitude = location.coordinates[0],
    name = name,
    phone1 = phone1,
    phone2 = phone2,
    plusCode = plusCode,
    postalCode = postalCode,
    reportedBy = reportedBy,
    state = state,
    svi = svi,
    what3Words = what3words,
    updatedAt = updatedAt,
)