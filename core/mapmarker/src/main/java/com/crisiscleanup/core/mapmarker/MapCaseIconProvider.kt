package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.crisiscleanup.core.model.data.WorkTypeType
import com.google.android.gms.maps.model.BitmapDescriptor

interface MapCaseIconProvider {
    /**
     * Offset to the center of the icon (in pixels)
     */
    val iconOffset: Offset

    fun getIcon(
        statusClaim: WorkTypeStatusClaim,
        workType: WorkTypeType,
        hasMultipleWorkTypes: Boolean,
    ): BitmapDescriptor?

    fun getIconBitmap(
        statusClaim: WorkTypeStatusClaim,
        workType: WorkTypeType,
        hasMultipleWorkTypes: Boolean,
    ): Bitmap?
}