package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.model.data.CaseImage
import com.crisiscleanup.core.model.data.asCaseImage

data class PopulatedWorksiteFiles(
    @Embedded
    val entity: WorksiteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorksiteNetworkFileCrossRef::class,
            parentColumn = "worksite_id",
            entityColumn = "network_file_id",
        ),
    )
    val files: List<NetworkFileEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorksiteNetworkFileCrossRef::class,
            parentColumn = "worksite_id",
            entityColumn = "network_file_id",
        ),
    )
    val fileImages: List<NetworkFileLocalImageEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val localImages: List<WorksiteLocalImageEntity>,
)

fun PopulatedWorksiteFiles.toCaseImages(): List<CaseImage> {
    val localFileImageLookup = fileImages.associateBy(NetworkFileLocalImageEntity::id)
    val networkImages = files
        .filter { localFileImageLookup[it.id]?.isDeleted != true }
        .filter { it.fullUrl?.isNotBlank() == true }
        .map {
            val rotateDegrees = localFileImageLookup[it.id]?.rotateDegrees ?: 0
            it.asImageModel(rotateDegrees).asCaseImage()
        }

    val localImages = localImages.map { it.asExternalModel().asCaseImage() }

    return localImages.filterNot(CaseImage::isAfter).toMutableList()
        .apply {
            addAll(networkImages.filterNot(CaseImage::isAfter))
            addAll(localImages.filter(CaseImage::isAfter))
            addAll(networkImages.filter(CaseImage::isAfter))
        }
}
