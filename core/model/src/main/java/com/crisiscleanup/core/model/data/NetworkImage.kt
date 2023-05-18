package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

enum class ImageCategory(val literal: String) {
    Before("before"),
    After("after"),
}

private val imageCategoryLookup = ImageCategory.values().associateBy(ImageCategory::literal)

data class NetworkImage(
    val id: Long,
    val createdAt: Instant,
    val title: String,
    val thumbnailUrl: String,
    val imageUrl: String,
    val tag: String,
    val rotateDegrees: Int,
    val category: ImageCategory = imageCategoryLookup[tag.lowercase()] ?: ImageCategory.Before,
    val isAfter: Boolean = category == ImageCategory.After,
)
