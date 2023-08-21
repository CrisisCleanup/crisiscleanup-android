package com.crisiscleanup.core.database.model

import com.crisiscleanup.core.common.UuidGenerator
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import kotlinx.datetime.Clock

fun Worksite.asEntities(
    uuidGenerator: UuidGenerator,
    primaryWorkType: WorkType,
    flagIdLookup: Map<Long, Long>,
    noteIdLookup: Map<Long, Long>,
    workTypeIdLookup: Map<Long, Long>,
): EditWorksiteEntities {
    val modifiedAt = updatedAt ?: Clock.System.now()

    val coreEntity = WorksiteEntity(
        id = id,
        networkId = networkId,
        incidentId = incidentId,
        address = address,
        autoContactFrequencyT = autoContactFrequencyT,
        caseNumber = caseNumber,
        caseNumberOrder = parseCaseNumberOrder(caseNumber),
        city = city,
        county = county,
        createdAt = createdAt,
        email = email,
        favoriteId = favoriteId,
        keyWorkTypeType = primaryWorkType.workTypeLiteral,
        keyWorkTypeOrgClaim = primaryWorkType.orgClaim,
        keyWorkTypeStatus = primaryWorkType.statusLiteral,
        latitude = latitude,
        longitude = longitude,
        name = name,
        phone1 = phone1,
        phone2 = phone2,
        plusCode = plusCode,
        postalCode = postalCode,
        reportedBy = reportedBy,
        state = state,
        svi = svi,
        what3Words = what3Words ?: "",
        updatedAt = modifiedAt,
        isLocalFavorite = isLocalFavorite,
    )

    val flagsEntities = flags?.map { flag ->
        val networkId = flagIdLookup[flag.id] ?: -1
        WorksiteFlagEntity(
            id = flag.id,
            networkId = networkId,
            worksiteId = id,
            action = flag.action,
            createdAt = flag.createdAt,
            isHighPriority = flag.isHighPriority,
            notes = flag.notes,
            reasonT = flag.reasonT,
            requestedAction = flag.requestedAction,
        )
    }

    val formDataEntities = formData?.map { entry ->
        val formDataValue = entry.value
        WorksiteFormDataEntity(
            worksiteId = id,
            fieldKey = entry.key,
            isBoolValue = formDataValue.isBoolean,
            valueString = formDataValue.valueString,
            valueBool = formDataValue.valueBoolean,
        )
    }

    val notesEntities = notes.map { note ->
        val networkId = noteIdLookup[note.id] ?: -1
        val isNew = networkId < 0
        WorksiteNoteEntity(
            id = note.id,
            localGlobalUuid = if (isNew) uuidGenerator.uuid() else "",
            networkId = networkId,
            worksiteId = id,
            createdAt = note.createdAt,
            isSurvivor = note.isSurvivor,
            note = note.note,
        )
    }

    val workTypesEntities = workTypes.map { workType ->
        val networkId = workTypeIdLookup[workType.id] ?: -1
        WorkTypeEntity(
            id = workType.id,
            networkId = networkId,
            worksiteId = id,
            createdAt = workType.createdAt,
            orgClaim = workType.orgClaim,
            nextRecurAt = workType.nextRecurAt,
            phase = workType.phase,
            recur = workType.recur,
            status = workType.statusLiteral,
            workType = workType.workTypeLiteral,
        )
    }

    return EditWorksiteEntities(
        coreEntity,
        flagsEntities ?: emptyList(),
        formDataEntities ?: emptyList(),
        notesEntities,
        workTypesEntities,
    )
}

data class EditWorksiteEntities(
    val core: WorksiteEntity,
    val flags: Collection<WorksiteFlagEntity>,
    val formData: Collection<WorksiteFormDataEntity>,
    val notes: Collection<WorksiteNoteEntity>,
    val workTypes: Collection<WorkTypeEntity>,
)
