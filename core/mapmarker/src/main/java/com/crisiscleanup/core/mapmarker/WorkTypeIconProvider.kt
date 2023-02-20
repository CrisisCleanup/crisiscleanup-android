package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.collection.LruCache
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.CaseStatus
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkTypeIconProvider @Inject constructor(
    private val resourceProvider: AndroidResourceProvider,
) : MapCaseIconProvider {
    private val cache = LruCache<WorkTypeStatusClaim, BitmapDescriptor>(32)
    private val bitmapCache = LruCache<WorkTypeStatusClaim, Bitmap>(32)

    // TODO Make configurable
    private val bitmapSizeDp = 48f
    private val bitmapSize: Int

    init {
        bitmapSize = resourceProvider.dpToPx(bitmapSizeDp).toInt()
    }

    private fun cacheIconBitmap(statusClaim: WorkTypeStatusClaim): BitmapDescriptor? {
        val bitmap = drawIcon(statusClaim)
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        synchronized(cache) {
            bitmapCache.put(statusClaim, bitmap)
            cache.put(statusClaim, bitmapDescriptor)
            return bitmapDescriptor
        }
    }

    override fun getIcon(statusClaim: WorkTypeStatusClaim): BitmapDescriptor? {
        synchronized(cache) {
            cache.get(statusClaim)?.let {
                return it
            }
        }

        return cacheIconBitmap(statusClaim)
    }

    override fun getIconBitmap(statusClaim: WorkTypeStatusClaim): Bitmap? {
        synchronized(cache) {
            bitmapCache.get(statusClaim)?.let {
                return it
            }
        }

        cacheIconBitmap(statusClaim)
        synchronized(cache) {
            bitmapCache.get(statusClaim)?.let {
                return it
            }
            return null
        }
    }

    private fun drawIcon(statusClaim: WorkTypeStatusClaim): Bitmap {
        val status = statusClaimToStatus[statusClaim]

        val drawable = resourceProvider.getDrawable(R.drawable.ic_work_type_trees)
        val output = Bitmap.createBitmap(
            bitmapSize,
            bitmapSize,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        val colors = mapMarkerColors[status] ?: mapMarkerColors[CaseStatus.Unknown]!!

        // TODO Nothing if alpha is nothing.
        //      Lerp grayscale value between stroke and fill.

        // TODO Draw plus over if there are multiple

        return output
    }
}