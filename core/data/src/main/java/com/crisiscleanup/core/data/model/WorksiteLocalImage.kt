package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.WorksiteLocalImageEntity
import com.crisiscleanup.core.model.data.WorksiteLocalImage

fun WorksiteLocalImage.asEntity() = WorksiteLocalImageEntity(
    id = id,
    documentId = documentId,
    worksiteId = worksiteId,
    uri = uri,
    tag = tag,
    rotateDegrees = rotateDegrees,
)
