package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.NetworkFileLocalImageEntity
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

    // PhotoChangeDataProvider

    override suspend fun getDeletedPhotoNetworkFileIds(worksiteId: Long): Pair<Long, List<Long>> =
        db.withTransaction {
            val networkWorksiteId = db.worksiteDao().getWorksiteNetworkId(worksiteId)
            val deletedFileIds = db.networkFileDao().getDeletedPhotoNetworkFileIds(worksiteId)
            Pair(networkWorksiteId, deletedFileIds)
        }
}