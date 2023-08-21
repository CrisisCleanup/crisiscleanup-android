package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import javax.inject.Inject
import javax.inject.Singleton

interface DrawableResourceBitmapProvider {
    fun getIcon(
        @DrawableRes drawableResId: Int,
        whDpDimensions: Pair<Float, Float>,
    ): BitmapDescriptor
}

@Singleton
class CrisisCleanupDrawableResourceBitmapProvider @Inject constructor(
    private val resourceProvider: AndroidResourceProvider,
) : DrawableResourceBitmapProvider {
    private var drawableResIdCache = 0
    private var whDimensionsCache = Pair(0f, 0f)
    private var bitmapDescriptor: BitmapDescriptor? = null

    override fun getIcon(
        @DrawableRes drawableResId: Int,
        whDpDimensions: Pair<Float, Float>,
    ): BitmapDescriptor {
        synchronized(this) {
            if (drawableResId == drawableResIdCache &&
                whDpDimensions == whDimensionsCache
            ) {
                bitmapDescriptor?.let {
                    return it
                }
            }
        }

        val bitmap = drawIcon(drawableResId, whDpDimensions.first, whDpDimensions.second)

        synchronized(this) {
            drawableResIdCache = drawableResId
            whDimensionsCache = whDpDimensions
            bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
            return bitmapDescriptor!!
        }
    }

    private fun drawIcon(
        @DrawableRes drawableResId: Int,
        width: Float,
        height: Float,
    ): Bitmap {
        val widthPx = resourceProvider.dpToPx(width).toInt()
        val heightPx = resourceProvider.dpToPx(height).toInt()

        val drawable = resourceProvider.getDrawable(drawableResId)
        val output = Bitmap.createBitmap(
            widthPx,
            heightPx,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return output
    }
}
