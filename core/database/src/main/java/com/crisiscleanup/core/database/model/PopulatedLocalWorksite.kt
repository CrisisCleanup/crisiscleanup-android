package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.model.data.LocalChange
import com.crisiscleanup.core.model.data.LocalWorksite
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFormValue

data class PopulatedLocalWorksite(
    @Embedded
    val entity: WorksiteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val workTypes: List<WorkTypeEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
    )
    val root: WorksiteRootEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val formData: List<WorksiteFormDataEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val flags: List<WorksiteFlagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val notes: List<WorksiteNoteEntity>,
)

fun PopulatedLocalWorksite.asExternalModel(translator: KeyTranslator? = null): LocalWorksite {
    val validWorkTypes = workTypes.filter { !it.isInvalid }
    val validFlags = flags.filter { !it.isInvalid }
    val formDataMap = formData.associate {
        it.fieldKey to WorksiteFormValue(
            isBoolean = it.isBoolValue,
            valueString = it.valueString,
            valueBoolean = it.valueBool,
        )
    }
    return LocalWorksite(
        Worksite(

            // Be sure to copy changes from PopulatedWorksite.asExternalModel to here

            id = entity.id,
            address = entity.address,
            autoContactFrequencyT = entity.autoContactFrequencyT ?: "",
            caseNumber = entity.caseNumber,
            city = entity.city,
            county = entity.county,
            createdAt = entity.createdAt,
            email = entity.email,
            favoriteId = entity.favoriteId,
            flags = validFlags.map { it.asExternalModel(translator) },
            formData = formDataMap,
            incidentId = entity.incidentId,
            keyWorkType = validWorkTypes.find { it.workType == entity.keyWorkTypeType }
                ?.asExternalModel(),
            latitude = entity.latitude,
            longitude = entity.longitude,
            name = entity.name,
            notes = notes.map(WorksiteNoteEntity::asExternalModel),
            networkId = entity.networkId,
            phone1 = entity.phone1 ?: "",
            phone2 = entity.phone2 ?: "",
            plusCode = entity.plusCode,
            postalCode = entity.postalCode,
            reportedBy = entity.reportedBy,
            state = entity.state,
            svi = entity.svi,
            updatedAt = entity.updatedAt,
            what3words = entity.what3Words ?: "",
            workTypes = validWorkTypes.map(WorkTypeEntity::asExternalModel),
        ),
        LocalChange(
            isLocalModified = root.isLocalModified,
            localModifiedAt = root.localModifiedAt,
            syncedAt = root.syncedAt,
        ),
    )
}