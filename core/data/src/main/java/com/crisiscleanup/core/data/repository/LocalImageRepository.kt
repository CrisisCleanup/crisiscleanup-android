package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.database.dao.NetworkFileDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface LocalImageRepository {
    fun streamNetworkImageUrl(id: Long): Flow<String>

//    suspend fun deleteNetworkImage(id: Long)
//
//    suspend fun setNetworkImageRotation(id: Long, rotationDegrees: Int)
}

class CrisisCleanupLocalImageRepository @Inject constructor(
    private val networkFileDao: NetworkFileDao,
//    private val localImageDaoPlus: LocalImageDaoPlus,
) : LocalImageRepository {
    override fun streamNetworkImageUrl(id: Long) = networkFileDao.streamNetworkImageUrl(id)

//    override suspend fun deleteNetworkImage(id: Long) = localImageDaoPlus.deleteNetworkImage(id)
//
//    override suspend fun setNetworkImageRotation(id: Long, rotationDegrees: Int) =
//        localImageDaoPlus.setNetworkImageRotation(id, rotationDegrees)
}