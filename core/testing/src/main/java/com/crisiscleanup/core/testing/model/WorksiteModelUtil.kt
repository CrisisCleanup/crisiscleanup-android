package com.crisiscleanup.core.testing.model

import com.crisiscleanup.core.model.data.*
import kotlinx.datetime.Instant

fun makeTestWorksite(
    prevUpdatedAt: Instant,
    noteCreatedAt: Instant,
    flags: List<WorksiteFlag>?,
    formData: Map<String, WorksiteFormValue>?,
) = Worksite(
    id = 0,
    address = "address",
    autoContactFrequencyT = AutoContactFrequency.Never.literal,
    caseNumber = "case-number",
    city = "city",
    county = "county",
    createdAt = null,
    email = "email",
    favoriteId = 14,
    flags = flags,
    formData = formData,
    incidentId = 1,
    keyWorkType = WorkType(
        id = 523,
        orgClaim = 513,
        statusLiteral = "status",
        workTypeLiteral = "work-type",
    ),
    latitude = 15.1421,
    longitude = -24.5918,
    name = "name",
    networkId = 2,
    notes = listOf(
        WorksiteNote(
            id = 51,
            createdAt = noteCreatedAt,
            isSurvivor = false,
            note = "worksite-note",
        )
    ),
    phone1 = "phone-1",
    phone2 = "phone-2",
    plusCode = "plus-code",
    postalCode = "postal-code",
    reportedBy = 523,
    state = "state",
    svi = 0.5f,
    updatedAt = prevUpdatedAt,
    what3Words = "what-three-words",
    workTypes = listOf(
        WorkType(
            id = 523,
            orgClaim = 513,
            statusLiteral = "status",
            workTypeLiteral = "work-type",
        )
    )
)

fun makeTestWorksiteFlag(
    createdAt: Instant,
    reasonT: String,
    id: Long = 531,
    isHighPriority: Boolean = false,
    reason: String = "reason-$reasonT",
) = WorksiteFlag(
    id = id,
    action = "",
    createdAt = createdAt,
    isHighPriority = isHighPriority,
    notes = "",
    reasonT = reasonT,
    reason = reason,
    requestedAction = "",
)