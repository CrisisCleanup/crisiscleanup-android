package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.core.network.model.KeyDynamicValuePair
import com.crisiscleanup.core.network.model.NetworkFlag
import com.crisiscleanup.core.network.model.NetworkNote
import com.crisiscleanup.core.network.model.NetworkType
import com.crisiscleanup.core.network.model.NetworkWorkType
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

internal val createdAtA = Clock.System.now().minus(10.days)
internal val updatedAtA = createdAtA.plus(1.hours)
internal val createdAtB = createdAtA.plus(3.days)
internal val updatedAtB = createdAtB.plus(2.hours)

internal fun testNetworkWorksite(
    flags: List<NetworkFlag> = emptyList(),
    favorite: NetworkType? = null,
    formData: List<KeyDynamicValuePair> = emptyList(),
    notes: List<NetworkNote> = emptyList(),
    keyWorkType: NetworkWorkType? = null,
    workTypes: List<NetworkWorkType> = emptyList(),
) = NetworkWorksiteFull(
    id = 0,
    address = "",
    autoContactFrequencyT = "",
    caseNumber = "",
    city = "",
    county = "",
    email = null,
    events = emptyList(),
    favorite = favorite,
    files = emptyList(),
    flags = flags,
    formData = formData,
    incident = 0,
    keyWorkType = keyWorkType,
    location = NetworkWorksiteFull.Location("", emptyList()),
    name = "",
    notes = notes,
    phone1 = "",
    phone2 = null,
    plusCode = null,
    postalCode = "",
    reportedBy = null,
    state = "",
    svi = null,
//    times = emptyList(),
    updatedAt = updatedAtA,
    what3words = null,
    workTypes = workTypes,
)

internal fun testCoreSnapshot(
    id: Long = 421,
    address: String = "",
    autoContactFrequencyT: String = "",
    caseNumber: String = "",
    city: String = "",
    county: String = "",
    createdAt: Instant? = null,
    email: String = "",
    favoriteId: Long? = null,
    formData: Map<String, DynamicValue> = emptyMap(),
    incidentId: Long = -1,
    keyWorkTypeId: Long? = null,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    name: String = "",
    networkId: Long = -1,
    phone1: String = "",
    phone2: String = "",
    plusCode: String = "",
    postalCode: String = "",
    reportedBy: Long? = null,
    state: String = "",
    svi: Float? = null,
    updatedAt: Instant? = null,
    what3Words: String? = null,
    isAssignedToOrgMember: Boolean = false,
) = CoreSnapshot(
    id = id,
    address = address,
    autoContactFrequencyT = autoContactFrequencyT,
    caseNumber = caseNumber,
    city = city,
    county = county,
    createdAt = createdAt,
    email = email,
    favoriteId = favoriteId,
    formData = formData,
    incidentId = incidentId,
    keyWorkTypeId = keyWorkTypeId,
    latitude = latitude,
    longitude = longitude,
    name = name,
    networkId = networkId,
    phone1 = phone1,
    phone2 = phone2,
    plusCode = plusCode,
    postalCode = postalCode,
    reportedBy = reportedBy,
    state = state,
    svi = svi,
    updatedAt = updatedAt,
    what3Words = what3Words,
    isAssignedToOrgMember = isAssignedToOrgMember,
)