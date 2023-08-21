package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.NetworkFileEntity
import com.crisiscleanup.core.database.model.NetworkFileLocalImageEntity
import com.crisiscleanup.core.database.model.PopulatedLocalImageDescription
import com.crisiscleanup.core.database.model.WorksiteLocalImageEntity
import com.crisiscleanup.core.database.model.WorksiteNetworkFileCrossRef
import com.crisiscleanup.core.model.data.PhotoChangeDataProvider
import javax.inject.Inject

class LocalImageDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) : PhotoChangeDataProvider {
    suspend fun deleteNetworkImage(id: Long) = db.withTransaction {
        with(db.localImageDao()) {
            insertIgnore(NetworkFileLocalImageEntity(id))
            markNetworkImageForDelete(id)
        }
    }

    suspend fun setNetworkImageRotation(id: Long, rotationDegrees: Int) = db.withTransaction {
        with(db.localImageDao()) {
            insertIgnore(NetworkFileLocalImageEntity(id))
            updateNetworkImageRotation(id, rotationDegrees)
        }
    }

    suspend fun setLocalImageRotation(id: Long, rotationDegrees: Int) = db.withTransaction {
        with(db.localImageDao()) {
            updateLocalImageRotation(id, rotationDegrees)
        }
    }

    suspend fun upsertLocalImage(image: WorksiteLocalImageEntity) = db.withTransaction {
        with(db.localImageDao()) {
            val insertId = insertIgnore(image)
            if (insertId <= 0) {
                update(image.worksiteId, image.documentId, image.tag)
            }
        }
    }

    suspend fun saveUploadedFile(
        worksiteId: Long,
        localImage: PopulatedLocalImageDescription,
        networkFile: NetworkFileEntity,
    ) = db.withTransaction {
        with(db.networkFileDao()) {
            upsert(networkFile)
            insertIgnoreCrossReference(WorksiteNetworkFileCrossRef(worksiteId, networkFile.id))
        }
        db.localImageDao().deleteLocalImage(localImage.id)
    }

    // PhotoChangeDataProvider

    override suspend fun getDeletedPhotoNetworkFileIds(worksiteId: Long): Pair<Long, List<Long>> =
        db.withTransaction {
            val networkWorksiteId = db.worksiteDao().getWorksiteNetworkId(worksiteId)
            val deletedFileIds = db.networkFileDao().getDeletedPhotoNetworkFileIds(worksiteId)
            Pair(networkWorksiteId, deletedFileIds)
        }
}
