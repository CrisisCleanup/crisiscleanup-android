package com.crisiscleanup.feature.caseeditor.model

import android.graphics.Bitmap
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.network.model.NetworkWorksiteLocationSearch
import com.google.android.gms.maps.model.LatLng

data class ExistingCaseLocation(
    val networkWorksiteId: Long,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val county: String,
    val caseNumber: String,
    val workType: WorkType?,
    val coordinates: LatLng,
    val icon: Bitmap?
)

fun NetworkWorksiteLocationSearch.asCaseLocation(iconProvider: MapCaseIconProvider): ExistingCaseLocation {
    var workType: WorkType? = null
    keyWorkType?.apply {
        workType = WorkType(
            id = id,
            orgClaim = orgClaim,
            statusLiteral = status,
            workTypeLiteral = this.workType,
        )
    }

    var icon: Bitmap? = null
    workType?.let {
        icon = iconProvider.getIconBitmap(
            it.statusClaim,
            it.workType,
            false,
        )
    }

    val (longitude, latitude) = location.coordinates
    return ExistingCaseLocation(
        networkWorksiteId = id,
        name = name,
        address = address,
        city = city,
        state = state,
        zipCode = postalCode ?: "",
        county = county,
        caseNumber = caseNumber,
        workType = workType,
        coordinates = LatLng(latitude, longitude),
        icon = icon,
    )
}