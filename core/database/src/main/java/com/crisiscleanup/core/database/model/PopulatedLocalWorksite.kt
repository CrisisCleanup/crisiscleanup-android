package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
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
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val workTypeRequests: List<WorkTypeTransferRequestEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorksiteNetworkFileCrossRef::class,
            parentColumn = "worksite_id",
            entityColumn = "network_file_id",
        )
    )
    val files: List<NetworkFileEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorksiteNetworkFileCrossRef::class,
            parentColumn = "worksite_id",
            entityColumn = "network_file_id",
        )
    )
    val fileImages: List<NetworkFileLocalImageEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val localImages: List<WorksiteLocalImageEntity>,
)

fun PopulatedLocalWorksite.asExternalModel(
    orgId: Long,
    translator: KeyTranslator? = null,
): LocalWorksite {
    val validWorkTypes = workTypes
    val formDataMap = formData.associate {
        it.fieldKey to WorksiteFormValue(
            isBoolean = it.isBoolValue,
            valueString = it.valueString,
            valueBoolean = it.valueBool,
        )
    }
    val localFileImageLookup = fileImages.associateBy(NetworkFileLocalImageEntity::id)
    val hasImagesPendingDelete = fileImages.any(NetworkFileLocalImageEntity::isDeleted)
    return with(entity) {
        LocalWorksite(
            Worksite(

                // Be sure to copy changes from PopulatedWorksite.asExternalModel to here

                id = id,
                address = address,
                autoContactFrequencyT = autoContactFrequencyT ?: "",
                caseNumber = caseNumber,
                city = city,
                county = county,
                createdAt = createdAt,
                email = email,
                favoriteId = favoriteId,
                files = files
                    .filter { localFileImageLookup[it.id]?.isDeleted != true }
                    .filter { it.fullUrl?.isNotBlank() == true }
                    .map {
                        val rotateDegrees = localFileImageLookup[it.id]?.rotateDegrees ?: 0
                        it.asImageModel(rotateDegrees)
                    },
                flags = flags.map { it.asExternalModel(translator) },
                formData = formDataMap,
                incidentId = incidentId,
                keyWorkType = validWorkTypes.find { it.workType == keyWorkTypeType }
                    ?.asExternalModel(),
                latitude = latitude,
                longitude = longitude,
                name = name,
                notes = notes
                    .filter { it.note.isNotBlank() }
                    .sortedWith { a, b ->
                        if (a.networkId == b.networkId) {
                            if (a.createdAt < b.createdAt) 1 else -1
                        } else {
                            if (a.networkId < 0) -1
                            else if (b.networkId < 0) 1
                            else if (a.networkId > b.networkId) -1 else 1
                        }
                    }
                    .map(WorksiteNoteEntity::asExternalModel),
                networkId = networkId,
                phone1 = phone1 ?: "",
                phone2 = phone2 ?: "",
                plusCode = plusCode,
                postalCode = postalCode,
                reportedBy = reportedBy,
                state = state,
                svi = svi,
                updatedAt = updatedAt,
                what3Words = what3Words ?: "",
                workTypes = validWorkTypes.map(WorkTypeEntity::asExternalModel),
                workTypeRequests = workTypeRequests.filter { it.byOrg == orgId }
                    .map(WorkTypeTransferRequestEntity::asExternalModel),
                isAssignedToOrgMember = if (root.isLocalModified) isLocalFavorite else favoriteId != null,
            ),
            localImages.map(WorksiteLocalImageEntity::asExternalModel),
            LocalChange(
                isLocalModified = root.isLocalModified || hasImagesPendingDelete,
                localModifiedAt = root.localModifiedAt,
                syncedAt = root.syncedAt,
            ),
        )
    }
}