package com.crisiscleanup.core.commoncase.model

import android.graphics.Bitmap
import com.crisiscleanup.core.model.data.WorksiteSummary

data class CaseSummaryResult(
    val summary: WorksiteSummary,
    val icon: Bitmap?,
    val networkWorksiteId: Long = summary.networkId,
    val listItemKey: Any = networkWorksiteId,
)
