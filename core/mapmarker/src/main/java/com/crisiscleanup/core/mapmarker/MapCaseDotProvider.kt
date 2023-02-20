package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.collection.LruCache
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.CaseStatus.Unknown
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import javax.inject.Inject
import javax.inject.Singleton

interface MapCaseDotProvider : MapCaseIconProvider {
    val centerSizePx: Float

    fun setDotProperties(dotDrawProperties: DotDrawProperties)
}

@Singleton
class InMemoryDotProvider @Inject constructor(
    resourceProvider: AndroidResourceProvider,
) : MapCaseDotProvider {
    private val cache = LruCache<WorkTypeStatusClaim, BitmapDescriptor>(16)
    private val bitmapCache = LruCache<WorkTypeStatusClaim, Bitmap>(16)

    private var cacheDotDrawProperties: DotDrawProperties
    override val centerSizePx: Float
        get() = cacheDotDrawProperties.centerSizePx

    init {
        cacheDotDrawProperties = DotDrawProperties.make(resourceProvider)
    }

    override fun setDotProperties(dotDrawProperties: DotDrawProperties) {
        synchronized(cacheDotDrawProperties) {
            if (cacheDotDrawProperties != dotDrawProperties) {
                cache.evictAll()
                bitmapCache.evictAll()
            }
            cacheDotDrawProperties = dotDrawProperties
        }
    }

    private fun cacheDotBitmap(
        statusClaim: WorkTypeStatusClaim,
        dotDrawProperties: DotDrawProperties,
    ): BitmapDescriptor? {
        val status = statusClaimToStatus[statusClaim]
        val colors = mapMarkerColors[status] ?: mapMarkerColors[Unknown]!!
        val bitmap = drawDot(colors, dotDrawProperties)
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        synchronized(cacheDotDrawProperties) {
            if (cacheDotDrawProperties != dotDrawProperties) {
                return null
            }

            bitmapCache.put(statusClaim, bitmap)
            cache.put(statusClaim, bitmapDescriptor)
            return bitmapDescriptor
        }
    }

    override fun getIcon(statusClaim: WorkTypeStatusClaim): BitmapDescriptor? {
        synchronized(cacheDotDrawProperties) {
            cache.get(statusClaim)?.let {
                return it
            }
        }

        return cacheDotBitmap(statusClaim, cacheDotDrawProperties)
    }

    override fun getIconBitmap(statusClaim: WorkTypeStatusClaim): Bitmap? {
        synchronized(cacheDotDrawProperties) {
            bitmapCache.get(statusClaim)?.let {
                return it
            }
        }

        val dotDrawProperties = cacheDotDrawProperties
        cacheDotBitmap(statusClaim, dotDrawProperties)
        synchronized(cacheDotDrawProperties) {
            if (cacheDotDrawProperties == dotDrawProperties) {
                bitmapCache.get(statusClaim)?.let {
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
    val strokeWidthPx: Float = 2f,
) {
    companion object {
        fun make(
            resourceProvider: AndroidResourceProvider,
            bitmapSizeDp: Float = 8f,
            dotDiameterDp: Float = 4f,
            strokeWidthDp: Float = 1f,
        ) = DotDrawProperties(
            bitmapSizePx = resourceProvider.dpToPx(bitmapSizeDp),
            dotDiameterPx = resourceProvider.dpToPx(dotDiameterDp),
            strokeWidthPx = resourceProvider.dpToPx(strokeWidthDp),
        )
    }
}
