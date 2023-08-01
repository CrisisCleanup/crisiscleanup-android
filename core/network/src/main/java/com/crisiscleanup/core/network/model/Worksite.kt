package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.network.worksitechange.CoreSnapshot
import com.crisiscleanup.core.network.worksitechange.FlagSnapshot
import com.crisiscleanup.core.network.worksitechange.NoteSnapshot
import com.crisiscleanup.core.network.worksitechange.WorkTypeSnapshot
import com.crisiscleanup.core.network.worksitechange.WorksiteSnapshot

fun Worksite.asSnapshotModel(
    // ID maps are local to network
    flagIdLookup: Map<Long, Long>,
    noteIdLookup: Map<Long, Long>,
    workTypeIdLookup: Map<Long, Long>,
): WorksiteSnapshot {
    return WorksiteSnapshot(
        CoreSnapshot(
            id = id,
            address = address,
            autoContactFrequencyT = autoContactFrequencyT,
            caseNumber = caseNumber,
            city = city,
            county = county,
            createdAt = createdAt,
            email = email,
            favoriteId = favoriteId,
            // flags = flags,
            formData = formData
                ?.map {
                    val dynamicValue = with(it.value) {
                        DynamicValue(valueString, isBoolean = isBoolean, valueBoolean)
                    }
                    Pair(it.key, dynamicValue)
                }
                ?.associate { it.first to it.second }
                ?: emptyMap(),
            incidentId = incidentId,
            // Keys to a work type in workTypes (by local ID).
            keyWorkTypeId = keyWorkType?.id,
            latitude = latitude,
            longitude = longitude,
            name = name,
            networkId = networkId,
            // notes = notes,
            phone1 = phone1,
            phone2 = phone2,
            plusCode = plusCode,
            postalCode = postalCode,
            reportedBy = reportedBy,
            state = state,
            svi = svi,
            updatedAt = updatedAt,
            what3Words = what3Words,
            // workTypes = workTypes,
            isAssignedToOrgMember = isAssignedToOrgMember,
        ),
        flags?.map { flag ->
            val attr = flag.attr
            FlagSnapshot(
                flag.id,
                FlagSnapshot.Flag(
                    id = flagIdLookup[flag.id] ?: -1,
                    action = flag.action,
                    createdAt = flag.createdAt,
                    isHighPriority = flag.isHighPriority,
                    notes = flag.notes,
                    reasonT = flag.reasonT,
                    reason = flag.reason,
                    requestedAction = flag.requestedAction,
                    involvesMyOrg = attr?.involvesMyOrg,
                    haveContactedOtherOrg = attr?.haveContactedOtherOrg,
                    organizationIds = attr?.organizations ?: emptyList(),
                )
            )
        } ?: emptyList(),
        notes.map { note ->
            NoteSnapshot(
                note.id,
                NoteSnapshot.Note(
                    id = noteIdLookup[note.id] ?: -1,
                    createdAt = note.createdAt,
                    isSurvivor = note.isSurvivor,
                    note = note.note,
                )
            )
        },
        workTypes.map { workType ->
            WorkTypeSnapshot(
                workType.id,
                WorkTypeSnapshot.WorkType(
                    id = workTypeIdLookup[workType.id] ?: -1,
                    createdAt = workType.createdAt,
                    orgClaim = workType.orgClaim,
                    nextRecurAt = workType.nextRecurAt,
                    phase = workType.phase,
                    recur = workType.recur,
                    status = workType.statusLiteral,
                    workType = workType.workTypeLiteral,
                )
            )
        },
    )
}
