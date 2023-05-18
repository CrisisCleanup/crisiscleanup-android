package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.NetworkFileEntity
import com.crisiscleanup.core.network.model.NetworkFile

fun NetworkFile.asEntity() = NetworkFileEntity(
    id = id,
    createdAt = createdAt,
    fileId = file ?: 0,
    fileTypeT = fileTypeT,
    fullUrl = fullUrl,
    largeThumbnailUrl = largeThumbnailUrl,
    mimeContentType = mimeContentType,
    smallThumbnailUrl = smallThumbnailUrl,
    tag = tag,
    title = title,
    url = url,
)