package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.WorksiteSummary

fun WorksiteSummary.asCaseLocation(iconProvider: MapCaseIconProvider): CaseSummaryResult {
    val icon = workType?.let {
        iconProvider.getIconBitmap(
            it.statusClaim,
            it.workType,
            false,
        )
    }

    return CaseSummaryResult(
        this,
        icon = icon,
    )
}