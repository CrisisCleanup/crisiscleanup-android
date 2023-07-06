package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.NetworkImage
import com.crisiscleanup.core.model.data.WorksiteLocalImage

enum class ImageCategory(val literal: String) {
    Before("before"),
    After("after"),
}

private val imageCategoryLookup = ImageCategory.values().associateBy(ImageCategory::literal)

data class CaseImage(
    val id: Long,
    val isNetworkImage: Boolean,
    val thumbnailUri: String,
    val imageUri: String,
    val tag: String,
    val title: String = "",
    val category: ImageCategory = imageCategoryLookup[tag.lowercase()] ?: ImageCategory.Before,
    val isAfter: Boolean = category == ImageCategory.After,
    val rotateDegrees: Int,
)

fun NetworkImage.asCaseImage() = CaseImage(
    id = id,
    true,
    thumbnailUri = thumbnailUrl,
    imageUri = imageUrl,
    tag = tag,
    title = title,
    rotateDegrees = rotateDegrees,
)

fun WorksiteLocalImage.asCaseImage() = CaseImage(
    id,
    false,
    thumbnailUri = uri,
    imageUri = uri,
    tag = tag,
    rotateDegrees = rotateDegrees,
)