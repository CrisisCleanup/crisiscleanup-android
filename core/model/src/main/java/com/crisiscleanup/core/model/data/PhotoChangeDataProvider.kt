package com.crisiscleanup.core.model.data

interface PhotoChangeDataProvider {
    suspend fun getDeletedPhotoNetworkFileIds(worksiteId: Long): Pair<Long, List<Long>>
}
