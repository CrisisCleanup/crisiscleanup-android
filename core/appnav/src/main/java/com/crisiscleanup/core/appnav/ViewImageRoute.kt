package com.crisiscleanup.core.appnav

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController

class ViewImageArgs(
    val imageId: Long,
    val imageUrl: String,
    val isNetworkImage: Boolean,
) {
    companion object {
        const val imageIdArg = "imageId-"
        const val imageUrlArg = "imageUrl"
        const val isNetworkImageArg = "isNetworkImage"

        fun queryString(imageId: Long, imageUrl: String, isNetworkImage: Boolean = true) = listOf(
            "$imageIdArg=$imageId",
            "$imageUrlArg=$imageUrl",
            "$isNetworkImageArg=$isNetworkImage"
        ).joinToString("&")
    }

    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[imageIdArg]),
        checkNotNull(savedStateHandle[imageUrlArg]),
        savedStateHandle.get<Boolean>(isNetworkImageArg) ?: true,
    )
}

fun NavController.navigateToViewImage(imageId: Long, imageUrl: String, isNetworkImage: Boolean) {
    val queryString = ViewImageArgs.queryString(imageId, imageUrl, isNetworkImage)
    this.navigate("${RouteConstant.viewImageRoute}?$queryString")
}
