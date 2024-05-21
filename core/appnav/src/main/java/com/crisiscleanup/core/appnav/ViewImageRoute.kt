package com.crisiscleanup.core.appnav

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.crisiscleanup.core.appnav.RouteConstant.VIEW_IMAGE_ROUTE
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
        const val IMAGE_ID_ARG = "imageId"
        const val ENCODED_URI_ARG = "encodedUri"
        const val IS_NETWORK_IMAGE_ARG = "isNetworkImage"
        const val ENCODED_TITLE_ARG = "encodedTitle"

        fun queryString(args: ViewImageArgs) = listOf(
            "$IMAGE_ID_ARG=${args.imageId}",
            "$ENCODED_URI_ARG=${args.encodedUri}",
            "$IS_NETWORK_IMAGE_ARG=${args.isNetworkImage}",
            "$ENCODED_TITLE_ARG=${args.encodedTitle}",
        ).joinToString("&")
    }

    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[IMAGE_ID_ARG]),
        checkNotNull(savedStateHandle[ENCODED_URI_ARG]),
        checkNotNull(savedStateHandle[IS_NETWORK_IMAGE_ARG]),
        checkNotNull(savedStateHandle[ENCODED_TITLE_ARG]),
    )

    fun toWorksiteImageArgs(worksiteId: Long) = WorksiteImagesArgs(
        worksiteId = worksiteId,
        imageId = imageId,
        encodedUri = encodedUri,
        encodedTitle = encodedTitle,
    )
}

fun NavController.navigateToViewImage(viewImageArgs: ViewImageArgs) {
    val queryString = ViewImageArgs.queryString(viewImageArgs)
    this.navigate("$VIEW_IMAGE_ROUTE?$queryString")
}
