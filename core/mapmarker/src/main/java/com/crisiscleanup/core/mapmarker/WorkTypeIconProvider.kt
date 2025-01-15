package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.ui.geometry.Offset
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.crisiscleanup.core.model.data.WorkTypeType
import com.crisiscleanup.core.model.data.WorkTypeType.Favorite
import com.crisiscleanup.core.model.data.WorkTypeType.Important
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.google.android.renderscript.Toolkit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkTypeIconProvider @Inject constructor(
    private val resourceProvider: AndroidResourceProvider,
) : MapCaseIconProvider {
    private val cache = LruCache<CacheKey, BitmapDescriptor>(64)
    private val bitmapCache = LruCache<CacheKey, Bitmap>(64)

    private val argbEvaluator = ArgbEvaluatorCompat()

    // TODO Parameterize values

    private val shadowRadiusDp = 3f
    private val shadowRadius: Int
    private val shadowColor = (0xFF666666).toInt()

    private val contentPaddingDp = 4f
    private val bitmapSizeDp = 36f + (contentPaddingDp + shadowRadiusDp) * 2
    private val bitmapSize = resourceProvider.dpToPx(bitmapSizeDp).toInt()
    private val contentRadius: Float
    private val bitmapCenterPx: Float

    override val mapTileIconOffset = Offset.Zero

    private val plusDrawable: Drawable
    private val plusDrawableTransparent: Drawable

    private val whiteCircle: Bitmap
    private val centerCircleBounds: RectF

    init {
        val centerOffsetDp = bitmapSizeDp * 0.5f
        shadowRadius = resourceProvider.dpToPx(shadowRadiusDp).toInt()
        val contentHalfSize = (bitmapSizeDp - 2 * shadowRadiusDp) * 0.5f
        contentRadius = resourceProvider.dpToPx(contentHalfSize)
        bitmapCenterPx = resourceProvider.dpToPx(centerOffsetDp)

        plusDrawable = resourceProvider.getDrawable(R.drawable.ic_work_type_plus)
        plusDrawableTransparent = resourceProvider.getDrawable(R.drawable.ic_work_type_plus).also {
            it.alpha = (255 * FILTERED_OUT_MARKER_ALPHA).toInt()
        }

        val whitePaint = getAntiAliasPaint(Color.WHITE)
        val flatCircle = Bitmap.createBitmap(
            bitmapSize,
            bitmapSize,
            Bitmap.Config.ARGB_8888,
        )
        Canvas(flatCircle).apply {
            drawCircle(bitmapCenterPx, bitmapCenterPx, contentRadius, whitePaint)
        }
        val blurCircle = flatCircle.blur(shadowRadius)
        Canvas(blurCircle).apply {
            drawBitmap(flatCircle, 0f, 0f, null)
        }
        whiteCircle = blurCircle

        val bitmapSizeStart = bitmapSize * 0.17f
        val bitmapSizeEnd = bitmapSize - bitmapSizeStart
        centerCircleBounds = RectF(
            bitmapSizeStart,
            bitmapSizeStart,
            bitmapSizeEnd,
            bitmapSizeEnd,
        )
    }

    private fun cacheIconBitmap(cacheKey: CacheKey): BitmapDescriptor {
        val bitmap = drawIcon(cacheKey)
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        synchronized(cache) {
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
        isVisited: Boolean,
        isAssignedTeam: Boolean,
    ): BitmapDescriptor {
        val cacheKey = CacheKey(
            statusClaim,
            workType,
            hasMultipleWorkTypes = hasMultipleWorkTypes,
            isFavorite = isFavorite,
            isImportant = isImportant,
            isDuplicate = isDuplicate,
            isFilteredOut = isFilteredOut,
            isVisited = isVisited,
            isAssignedTeam = isAssignedTeam,
        )
        synchronized(cache) {
            cache[cacheKey]?.let {
                return it
            }
        }

        return cacheIconBitmap(cacheKey)
    }

    override fun getIconBitmap(
        statusClaim: WorkTypeStatusClaim,
        workType: WorkTypeType,
        hasMultipleWorkTypes: Boolean,
        isDuplicate: Boolean,
        isFilteredOut: Boolean,
        isVisited: Boolean,
        isAssignedTeam: Boolean,
    ): Bitmap? {
        val cacheKey = CacheKey(
            statusClaim,
            workType,
            hasMultipleWorkTypes = hasMultipleWorkTypes,
            isDuplicate = isDuplicate,
            isFilteredOut = isFilteredOut,
            isVisited = isVisited,
            isAssignedTeam = isAssignedTeam,
        )
        synchronized(cache) {
            bitmapCache[cacheKey]?.let {
                return it
            }
        }

        cacheIconBitmap(cacheKey)
        synchronized(cache) {
            bitmapCache[cacheKey]?.let {
                return it
            }
            return null
        }
    }

    private fun getAntiAliasPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    private fun Bitmap.blur(radius: Int): Bitmap {
        val paint = getAntiAliasPaint(shadowColor)
        val flatShadow = Bitmap.createBitmap(
            bitmapSize,
            bitmapSize,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(flatShadow)
        canvas.drawBitmap(this, Matrix(), paint)
        return Toolkit.blur(flatShadow, radius)
    }

    private fun drawIcon(cacheKey: CacheKey): Bitmap {
        val iconResId = if (cacheKey.isFavorite) {
            statusIconLookup[Favorite]!!
        } else if (cacheKey.isImportant) {
            statusIconLookup[Important]!!
        } else {
            statusIconLookup[cacheKey.workType] ?: R.drawable.ic_work_type_unknown
        }

        // TODO Keep bounds squared and icon centered
        val rightBounds = bitmapSize - shadowRadius
        val bottomBounds = bitmapSize - shadowRadius
        val (output, canvas) = resourceProvider.createDrawableBitmap(
            iconResId,
            bitmapSize,
            shadowRadius,
            rightBounds,
            shadowRadius,
            bottomBounds,
        )

        val colors = getMapMarkerColors(
            cacheKey.statusClaim,
            cacheKey.isDuplicate,
            cacheKey.isFilteredOut,
            cacheKey.isVisited,
            isDot = false,
        )
        output.applyColors(
            colors,
            shadowRadius,
            rightBounds,
            shadowRadius,
            bottomBounds,
            argbEvaluator,
        )

        val isBlurred = !cacheKey.isFilteredOut
        val blurred = if (isBlurred) output.blur(shadowRadius) else output

        if (cacheKey.hasMultipleWorkTypes) {
            synchronized(plusDrawable) {
                val pd = if (cacheKey.isFilteredOut) plusDrawableTransparent else plusDrawable
                pd.setBounds(
                    rightBounds - pd.intrinsicWidth,
                    bottomBounds - pd.intrinsicHeight,
                    rightBounds,
                    bottomBounds,
                )
                pd.draw(canvas)
            }
        }

        if (isBlurred) {
            Canvas(blurred).apply {
                drawBitmap(output, 0f, 0f, null)
            }
        }

        var teamAssigned = blurred
        if (cacheKey.isAssignedTeam) {
            val composite = Bitmap.createBitmap(
                bitmapSize,
                bitmapSize,
                Bitmap.Config.ARGB_8888,
            )
            Canvas(composite).apply {
                drawBitmap(whiteCircle, Matrix(), null)
                drawBitmap(teamAssigned, null, centerCircleBounds, null)
            }
            teamAssigned = composite
        }

        return teamAssigned
    }

    private data class CacheKey(
        val statusClaim: WorkTypeStatusClaim,
        val workType: WorkTypeType,
        val hasMultipleWorkTypes: Boolean,
        val isFavorite: Boolean = false,
        val isImportant: Boolean = false,
        val isDuplicate: Boolean = false,
        val isFilteredOut: Boolean = false,
        val isVisited: Boolean = false,
        val isAssignedTeam: Boolean = false,
    )
}
