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
    
    networkId = id,
    incidentId = incidentId,
    address = address,
    autoContactFrequencyT = autoContactFrequencyT,
    caseNumber = caseNumber,
    city = city,
    county = county,
    email = email,
    favoriteId = favorite,
    keyWorkTypeType = keyWorkType?.workType ?: "",
    latitude = location.coordinates[0],
    longitude = location.coordinates[1],
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