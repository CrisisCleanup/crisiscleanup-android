package com.crisiscleanup.core.model.data

data class WorksiteLocalImage(
    val id: Long,
    val worksiteId: Long,
    val documentId: String,
    val uri: String,
    val tag: String,
    val rotateDegrees: Int = 0,
)
