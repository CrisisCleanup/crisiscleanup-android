package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.database.dao.LocalImageDao
import com.crisiscleanup.core.database.dao.LocalImageDaoPlus
import com.crisiscleanup.core.database.dao.NetworkFileDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface LocalImageRepository {
    fun streamNetworkImageUrl(id: Long): Flow<String>
    fun getLocalImageRotation(id: Long, isNetworkImage: Boolean): Int

    suspend fun deleteNetworkImage(id: Long)

    suspend fun setNetworkImageRotation(id: Long, rotationDegrees: Int)

}

class CrisisCleanupLocalImageRepository @Inject constructor(
    private val networkFileDao: NetworkFileDao,
    private val localImageDao: LocalImageDao,
    private val localImageDaoPlus: LocalImageDaoPlus,
) : LocalImageRepository {
    override fun streamNetworkImageUrl(id: Long) = networkFileDao.streamNetworkImageUrl(id)
    override fun getLocalImageRotation(id: Long, isNetworkImage: Boolean): Int {
        return if (isNetworkImage) localImageDao.getNetworkFileLocalImage(id)?.rotateDegrees ?: 0
        else 0
    }

    override suspend fun deleteNetworkImage(id: Long) = localImageDaoPlus.deleteNetworkImage(id)

    override suspend fun setNetworkImageRotation(id: Long, rotationDegrees: Int) =
        localImageDaoPlus.setNetworkImageRotation(id, rotationDegrees)
}