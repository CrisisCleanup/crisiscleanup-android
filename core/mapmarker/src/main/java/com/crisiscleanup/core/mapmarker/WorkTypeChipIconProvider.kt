package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import androidx.collection.LruCache
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.crisiscleanup.core.model.data.WorkTypeType
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.material.animation.ArgbEvaluatorCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkTypeChipIconProvider @Inject constructor(
    private val resourceProvider: AndroidResourceProvider,
) {
    private val bitmapCache = LruCache<CacheKey, Bitmap>(64)

    private val argbEvaluator = ArgbEvaluatorCompat()

    // TODO Parameterize values

    private val bitmapSizeDp = 20f
    private val bitmapSize: Int

    init {
        bitmapSize = resourceProvider.dpToPx(bitmapSizeDp).toInt()
    }

    fun getIconBitmap(
        statusClaim: WorkTypeStatusClaim,
        workType: WorkTypeType,
    ): Bitmap? {
        val cacheKey = CacheKey(
            statusClaim,
            workType,
        )
        synchronized(bitmapCache) {
            bitmapCache[cacheKey]?.let {
                return it
            }
        }

        cacheIconBitmap(cacheKey)
        synchronized(bitmapCache) {
            bitmapCache[cacheKey]?.let {
                return it
            }
            return null
        }
    }

    private fun cacheIconBitmap(cacheKey: CacheKey): BitmapDescriptor {
        val bitmap = drawIcon(cacheKey)
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        synchronized(bitmapCache) {
            bitmapCache.put(cacheKey, bitmap)
            return bitmapDescriptor
        }
    }

    private fun drawIcon(cacheKey: CacheKey): Bitmap {
        val iconResId =
            statusIconLookup[cacheKey.workType] ?: R.drawable.ic_work_type_unknown

        val rightBounds = bitmapSize
        val bottomBounds = bitmapSize
        val (output, _) = resourceProvider.createDrawableBitmap(
            iconResId,
            bitmapSize,
            0,
            rightBounds,
            0,
            bottomBounds,
        )

        val colors = getMapMarkerColors(cacheKey.statusClaim)
        output.applyColors(
            colors,
            0,
            rightBounds,
            0,
            bottomBounds,
            argbEvaluator,
        )

        return output
    }

    private data class CacheKey(
        val statusClaim: WorkTypeStatusClaim,
        val workType: WorkTypeType,
    )
}
