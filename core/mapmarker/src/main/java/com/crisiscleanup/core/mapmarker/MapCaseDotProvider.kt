package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.collection.LruCache
import androidx.compose.ui.geometry.Offset
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.crisiscleanup.core.model.data.WorkTypeType
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import javax.inject.Inject
import javax.inject.Singleton

interface MapCaseDotProvider : MapCaseIconProvider {
    fun setDotProperties(dotDrawProperties: DotDrawProperties)
}

@Singleton
class InMemoryDotProvider @Inject constructor(
    resourceProvider: AndroidResourceProvider,
) : MapCaseDotProvider {
    private val cache = LruCache<DotCacheKey, BitmapDescriptor>(16)
    private val bitmapCache = LruCache<DotCacheKey, Bitmap>(16)

    private var cacheDotDrawProperties: DotDrawProperties
    private var dotOffsetPx = Offset(0f, 0f)

    override val iconOffset: Offset
        get() = dotOffsetPx

    init {
        cacheDotDrawProperties = DotDrawProperties.make(resourceProvider)
        setIconOffset()
    }

    override fun setDotProperties(dotDrawProperties: DotDrawProperties) {
        synchronized(cache) {
            if (cacheDotDrawProperties != dotDrawProperties) {
                cache.evictAll()
                bitmapCache.evictAll()
            }
            cacheDotDrawProperties = dotDrawProperties
            setIconOffset()
        }
    }

    private fun setIconOffset() {
        val centerSizePx = cacheDotDrawProperties.centerSizePx
        dotOffsetPx = Offset(centerSizePx, centerSizePx)
    }

    private fun cacheDotBitmap(
        cacheKey: DotCacheKey,
        dotDrawProperties: DotDrawProperties,
    ): BitmapDescriptor? {
        val colors = getMapMarkerColors(
            cacheKey.statusClaim,
            cacheKey.isDuplicate,
            cacheKey.isFilteredOut,
        )
        val bitmap = drawDot(colors, dotDrawProperties)
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        synchronized(cache) {
            if (cacheDotDrawProperties != dotDrawProperties) {
                return null
            }

            bitmapCache.put(cacheKey, bitmap)
            cache.put(cacheKey, bitmapDescriptor)
            return bitmapDescriptor
        }
    }

    override fun getIcon(
        statusClaim: WorkTypeStatusClaim,
        workType: WorkTypeType,
        isFavorite: Boolean,
        isImportant: Boolean,
        hasMultipleWorkTypes: Boolean,
        isDuplicate: Boolean,
        isFilteredOut: Boolean,
    ): BitmapDescriptor? {
        val cacheKey = DotCacheKey(statusClaim, isDuplicate, isFilteredOut)
        synchronized(cache) {
            cache.get(cacheKey)?.let {
                return it
            }
        }

        return cacheDotBitmap(cacheKey, cacheDotDrawProperties)
    }

    override fun getIconBitmap(
        statusClaim: WorkTypeStatusClaim,
        workType: WorkTypeType,
        hasMultipleWorkTypes: Boolean,
        isDuplicate: Boolean,
        isFilteredOut: Boolean,
    ): Bitmap? {
        val cacheKey = DotCacheKey(statusClaim, isDuplicate, isFilteredOut)
        synchronized(cache) {
            bitmapCache.get(cacheKey)?.let {
                return it
            }
        }

        val dotDrawProperties = cacheDotDrawProperties
        cacheDotBitmap(cacheKey, dotDrawProperties)
        synchronized(cache) {
            if (cacheDotDrawProperties == dotDrawProperties) {
                bitmapCache.get(cacheKey)?.let {
                    return it
                }
            }
            return null
        }
    }

    private fun drawDot(
        colors: MapMarkerColor,
        dotDrawProperties: DotDrawProperties,
    ): Bitmap {
        val bitmapSize = dotDrawProperties.bitmapSizePx.toInt()
        val output = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val radius = dotDrawProperties.dotDiameterPx * 0.5f
        val center = dotDrawProperties.centerSizePx
        val strokeWidthPx = dotDrawProperties.strokeWidthPx

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        dotPaint.isAntiAlias = true
        dotPaint.style = Paint.Style.FILL
        dotPaint.setColor(colors.fill.value.toLong())
        canvas.drawCircle(center, center, radius, dotPaint)

        dotPaint.style = Paint.Style.STROKE
        dotPaint.setColor(colors.stroke.value.toLong())
        dotPaint.strokeWidth = strokeWidthPx
        dotPaint.isAntiAlias = true
        canvas.drawCircle(center, center, radius + strokeWidthPx * 0.5f, dotPaint)

        return output
    }
}

data class DotDrawProperties(
    val bitmapSizePx: Float = 12f,
    val centerSizePx: Float = bitmapSizePx * 0.5f,
    val dotDiameterPx: Float = 6f,
    val strokeWidthPx: Float = 1f,
) {
    companion object {
        fun make(
            resourceProvider: AndroidResourceProvider,
            bitmapSizeDp: Float = 8f,
            dotDiameterDp: Float = 4f,
            strokeWidthDp: Float = 0.5f,
        ) = DotDrawProperties(
            bitmapSizePx = resourceProvider.dpToPx(bitmapSizeDp),
            dotDiameterPx = resourceProvider.dpToPx(dotDiameterDp),
            strokeWidthPx = resourceProvider.dpToPx(strokeWidthDp),
        )
    }
}

private data class DotCacheKey(
    val statusClaim: WorkTypeStatusClaim,
    val isDuplicate: Boolean = false,
    val isFilteredOut: Boolean = false,
)