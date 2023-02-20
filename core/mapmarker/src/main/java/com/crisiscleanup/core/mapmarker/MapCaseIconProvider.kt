package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.google.android.gms.maps.model.BitmapDescriptor

interface MapCaseIconProvider {
    fun getIcon(statusClaim: WorkTypeStatusClaim): BitmapDescriptor?

    fun getIconBitmap(statusClaim: WorkTypeStatusClaim): Bitmap?
}