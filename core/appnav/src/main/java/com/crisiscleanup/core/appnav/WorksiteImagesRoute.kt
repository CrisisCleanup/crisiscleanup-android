package com.crisiscleanup.core.appnav

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.ENCODED_TITLE_ARG
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.ENCODED_URI_ARG
import com.crisiscleanup.core.appnav.ViewImageArgs.Companion.IMAGE_ID_ARG
import com.crisiscleanup.core.common.urlDecode

class WorksiteImagesArgs(
    val worksiteId: Long,
    val imageId: Long,
    val encodedUri: String = "",
    val encodedTitle: String,
) {
    val imageUri = encodedUri.urlDecode()
    val title = encodedTitle.urlDecode()

    companion object {
        const val WORKSITE_ID_ARG = "worksiteId"

        fun queryString(args: WorksiteImagesArgs) = listOf(
            "$WORKSITE_ID_ARG=${args.worksiteId}",
            "$IMAGE_ID_ARG=${args.imageId}",
            "$ENCODED_URI_ARG=${args.encodedUri}",
            "$ENCODED_TITLE_ARG=${args.encodedTitle}",
        ).joinToString("&")
    }

    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[WORKSITE_ID_ARG]),
        checkNotNull(savedStateHandle[IMAGE_ID_ARG]),
        checkNotNull(savedStateHandle[ENCODED_URI_ARG]),
        checkNotNull(savedStateHandle[ENCODED_TITLE_ARG]),
    )
}

fun NavController.navigateToWorksiteImages(args: WorksiteImagesArgs) {
    val queryString = WorksiteImagesArgs.queryString(args)
    this.navigate("${RouteConstant.WORKSITE_IMAGES_ROUTE}?$queryString")
}
