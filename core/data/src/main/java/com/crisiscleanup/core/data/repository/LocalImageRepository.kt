package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.LocalImageDao
import com.crisiscleanup.core.database.dao.LocalImageDaoPlus
import com.crisiscleanup.core.database.dao.NetworkFileDao
import com.crisiscleanup.core.model.data.WorksiteLocalImage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface LocalImageRepository {
    fun streamNetworkImageUrl(id: Long): Flow<String>
    fun getImageRotation(id: Long, isNetworkImage: Boolean): Int

    suspend fun setNetworkImageRotation(id: Long, rotationDegrees: Int)

    suspend fun save(image: WorksiteLocalImage)
}

class CrisisCleanupLocalImageRepository @Inject constructor(
    private val networkFileDao: NetworkFileDao,
    private val localImageDao: LocalImageDao,
    private val localImageDaoPlus: LocalImageDaoPlus,
) : LocalImageRepository {
    override fun streamNetworkImageUrl(id: Long) = networkFileDao.streamNetworkImageUrl(id)
    override fun getImageRotation(id: Long, isNetworkImage: Boolean): Int {
        return if (isNetworkImage) localImageDao.getNetworkFileLocalImage(id)?.rotateDegrees ?: 0
        else 0
    }

    override suspend fun setNetworkImageRotation(id: Long, rotationDegrees: Int) =
        localImageDaoPlus.setNetworkImageRotation(id, rotationDegrees)

    override suspend fun save(image: WorksiteLocalImage) =
        localImageDaoPlus.upsertLocalImage(image.asEntity())
}