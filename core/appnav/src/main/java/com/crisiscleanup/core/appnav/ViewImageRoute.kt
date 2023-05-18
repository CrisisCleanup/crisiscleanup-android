package com.crisiscleanup.core.appnav

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.crisiscleanup.core.appnav.RouteConstant.viewImageRoute
import com.crisiscleanup.core.common.urlDecode
import com.crisiscleanup.core.common.urlEncode

class ViewImageArgs(
    val imageId: Long,
    encodedImageUrl: String,
    val isNetworkImage: Boolean,
    encodedTitle: String,
) {
    val imageUrl = encodedImageUrl.urlDecode()
    val title = encodedTitle.urlDecode()

    companion object {
        const val imageIdArg = "imageId"
        const val imageUrlArg = "imageUrl"
        const val isNetworkImageArg = "isNetworkImage"
        const val titleArg = "title"

        fun queryString(
            imageId: Long,
            imageUrl: String,
            isNetworkImage: Boolean,
            title: String = "",
        ) = listOf(
            "$imageIdArg=$imageId",
            "$imageUrlArg=${imageUrl.urlEncode()}",
            "$isNetworkImageArg=$isNetworkImage",
            "$titleArg=${title.urlEncode()}",
        ).joinToString("&")
    }

    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[imageIdArg]),
        checkNotNull(savedStateHandle[imageUrlArg]),
        checkNotNull(savedStateHandle[isNetworkImageArg]),
        checkNotNull(savedStateHandle[titleArg]),
    )
}

fun NavController.navigateToViewImage(
    imageId: Long,
    imageUrl: String,
    isNetworkImage: Boolean,
    title: String,
) {
    val queryString = ViewImageArgs.queryString(imageId, imageUrl, isNetworkImage, title)
    this.navigate("$viewImageRoute?$queryString")
}
