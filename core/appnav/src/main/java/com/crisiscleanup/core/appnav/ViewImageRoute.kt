package com.crisiscleanup.core.appnav

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.crisiscleanup.core.appnav.RouteConstant.viewImageRoute
import com.crisiscleanup.core.common.urlDecode

class ViewImageArgs(
    val imageId: Long,
    val encodedUri: String = "",
    val isNetworkImage: Boolean,
    val encodedTitle: String,
) {
    val imageUri = encodedUri.urlDecode()
    val title = encodedTitle.urlDecode()

    companion object {
        const val imageIdArg = "imageId"
        const val encodedUriArg = "encodedUri"
        const val isNetworkImageArg = "isNetworkImage"
        const val encodedTitleArg = "encodedTitle"

        fun queryString(args: ViewImageArgs) = listOf(
            "$imageIdArg=${args.imageId}",
            "$encodedUriArg=${args.encodedUri}",
            "$isNetworkImageArg=${args.isNetworkImage}",
            "$encodedTitleArg=${args.encodedTitle}",
        ).joinToString("&")
    }

    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[imageIdArg]),
        checkNotNull(savedStateHandle[encodedUriArg]),
        checkNotNull(savedStateHandle[isNetworkImageArg]),
        checkNotNull(savedStateHandle[encodedTitleArg]),
    )
}

fun NavController.navigateToViewImage(viewImageArgs: ViewImageArgs) {
    val queryString = ViewImageArgs.queryString(viewImageArgs)
    this.navigate("$viewImageRoute?$queryString")
}
