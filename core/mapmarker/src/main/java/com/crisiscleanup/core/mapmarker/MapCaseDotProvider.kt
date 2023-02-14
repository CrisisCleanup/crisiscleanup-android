package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.collection.LruCache
import androidx.compose.ui.graphics.Color
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.CaseStatus.ClaimedNotStarted
import com.crisiscleanup.core.model.data.CaseStatus.Completed
import com.crisiscleanup.core.model.data.CaseStatus.DoneByOthersNhwPc
import com.crisiscleanup.core.model.data.CaseStatus.InProgress
import com.crisiscleanup.core.model.data.CaseStatus.Incomplete
import com.crisiscleanup.core.model.data.CaseStatus.NeedsFollowUp
import com.crisiscleanup.core.model.data.CaseStatus.OutOfScopeDu
import com.crisiscleanup.core.model.data.CaseStatus.PartiallyCompleted
import com.crisiscleanup.core.model.data.CaseStatus.Unclaimed
import com.crisiscleanup.core.model.data.CaseStatus.Unknown
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedCompleted
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedDoneByOthers
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedIncomplete
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedOutOfScope
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenAssigned
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenNeedsFollowUp
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenPartiallyCompleted
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenUnassigned
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenUnresponsive
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import javax.inject.Inject
import javax.inject.Singleton

interface MapCaseDotProvider {
    val centerSizePx: Float

    fun setDotProperties(dotDrawProperties: DotDrawProperties)

    fun getDotIcon(statusClaim: WorkTypeStatusClaim): BitmapDescriptor?

    fun getDotBitmap(statusClaim: WorkTypeStatusClaim): Bitmap?
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
        val colors = caseDotMarkerColors[status] ?: caseDotMarkerColors[Unknown]!!
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

    override fun getDotIcon(statusClaim: WorkTypeStatusClaim): BitmapDescriptor? {
        synchronized(cacheDotDrawProperties) {
            cache.get(statusClaim)?.let {
                return it
            }
        }

        return cacheDotBitmap(statusClaim, cacheDotDrawProperties)
    }

    override fun getDotBitmap(statusClaim: WorkTypeStatusClaim): Bitmap? {
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
        colors: DotMarkerColors,
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

private data class DotMarkerColors(
    val fill: Color,
    val stroke: Color,
)

private fun makeDotColor(fill: Long, stroke: Long) = DotMarkerColors(
    Color(fill),
    Color(stroke),
)

private val statusClaimToStatus = mapOf(
    WorkTypeStatusClaim(WorkTypeStatus.Unknown, true) to Unknown,
    WorkTypeStatusClaim(OpenAssigned, true) to InProgress,
    WorkTypeStatusClaim(OpenUnassigned, true) to ClaimedNotStarted,
    WorkTypeStatusClaim(OpenPartiallyCompleted, true) to PartiallyCompleted,
    WorkTypeStatusClaim(OpenNeedsFollowUp, true) to NeedsFollowUp,
    WorkTypeStatusClaim(OpenUnresponsive, true) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedCompleted, true) to Completed,
    WorkTypeStatusClaim(ClosedIncomplete, true) to Incomplete,
    WorkTypeStatusClaim(ClosedOutOfScope, true) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedDoneByOthers, true) to DoneByOthersNhwPc,
    WorkTypeStatusClaim(WorkTypeStatus.Unknown, false) to Unknown,
    WorkTypeStatusClaim(OpenAssigned, false) to Unclaimed,
    WorkTypeStatusClaim(OpenUnassigned, false) to Unclaimed,
    WorkTypeStatusClaim(OpenPartiallyCompleted, false) to PartiallyCompleted,
    WorkTypeStatusClaim(OpenNeedsFollowUp, false) to NeedsFollowUp,
    WorkTypeStatusClaim(OpenUnresponsive, false) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedCompleted, false) to Completed,
    WorkTypeStatusClaim(ClosedIncomplete, false) to Incomplete,
    WorkTypeStatusClaim(ClosedOutOfScope, false) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedDoneByOthers, false) to DoneByOthersNhwPc,
)

private val caseDotMarkerColors = mapOf(
    Unknown to makeDotColor(0xFF000000, 0xFFFFFFFF),
    Unclaimed to makeDotColor(0xFFD0021B, 0xFFE30001),
    ClaimedNotStarted to makeDotColor(0xFFFAB92E, 0xFFF79820),
    InProgress to makeDotColor(0xFFF0F032, 0xFF85863F),
    PartiallyCompleted to makeDotColor(0xFF0054BB, 0xFF0054BB),
    NeedsFollowUp to makeDotColor(0xFFEA51EB, 0xFFE018E1),
    Completed to makeDotColor(0xFF82D78C, 0xFF51AC7C),
    DoneByOthersNhwPc to makeDotColor(0xFF0fa355, 0xFF0fa355),
    OutOfScopeDu to makeDotColor(0xFF787878, 0xFF5d5d5d),
    Incomplete to makeDotColor(0xFF1d1d1d, 0xFF1d1d1d),
)
