package com.crisiscleanup.core.model.data

interface PhotoChangeDataProvider {
    suspend fun getDeletedPhotoNetworkFileIds(worksiteId: Long): NetworkWorksiteFileIds
}

data class NetworkWorksiteFileIds(
    val worksiteId: Long,
    val fileIds: List<Long>,
)
