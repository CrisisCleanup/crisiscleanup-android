package com.crisiscleanup.feature.cases.model

import androidx.compose.ui.geometry.Offset
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerState

data class WorksiteGoogleMapMark(
    val source: WorksiteMapMark,
    val latLng: LatLng,
    val markerState: MarkerState,
    val mapIcon: BitmapDescriptor?,
    val mapIconOffset: Offset,
    val isFilteredOut: Boolean,
)

fun WorksiteMapMark.asWorksiteGoogleMapMark(
    iconProvider: MapCaseIconProvider,
    additionalScreenOffset: Pair<Float, Float>,
): WorksiteGoogleMapMark {
    val latLng = LatLng(latitude, longitude)
    val (xOffset, yOffset) = additionalScreenOffset
    return WorksiteGoogleMapMark(
        source = this,
        latLng = latLng,
        markerState = MarkerState(latLng),
        mapIcon = iconProvider.getIcon(
            statusClaim,
            workType,
            isFavorite = isFavorite,
            isImportant = isHighPriority,
            hasMultipleWorkTypes = workTypeCount > 1,
            isDuplicate = isDuplicate,
            isFilteredOut = isFilteredOut,
        ),
        mapIconOffset = Offset(0.5f + xOffset, 0.5f + yOffset),
        isFilteredOut = isFilteredOut,
    )
}
