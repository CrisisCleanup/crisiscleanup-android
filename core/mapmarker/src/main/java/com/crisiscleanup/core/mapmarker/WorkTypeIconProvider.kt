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
import androidx.core.graphics.createBitmap
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
    private val shadowRadius = resourceProvider.dpToPx(shadowRadiusDp).toInt()
    private val shadowColor = (0xFF666666).toInt()

    private val bitmapSizeDp = 40f + 2 * shadowRadiusDp
    private val bitmapSize = resourceProvider.dpToPx(bitmapSizeDp).toInt()
    private val contentRadius: Float
    private val bitmapCenterPx: Float

    override val mapTileIconOffset = Offset.Zero

    private val plusDrawable: Drawable
    private val plusDrawableTransparent: Drawable

    private val whiteCircle: Bitmap
    private val centerCircleBounds: RectF

    private val cameraDrawable: Drawable
    private val cameraDrawableTransparent: Drawable
    private val cameraDrawableVerticalOffset: Int

    init {
        val centerOffsetDp = bitmapSizeDp * 0.5f
        val contentHalfSize = (bitmapSizeDp - 2 * shadowRadiusDp) * 0.5f
        contentRadius = resourceProvider.dpToPx(contentHalfSize)
        bitmapCenterPx = resourceProvider.dpToPx(centerOffsetDp)

        val overlayAlpha = (255 * FILTERED_OUT_MARKER_ALPHA).toInt()
        plusDrawable = resourceProvider.getDrawable(R.drawable.ic_work_type_plus)
        plusDrawableTransparent = resourceProvider.getDrawable(R.drawable.ic_work_type_plus).also {
            it.alpha = overlayAlpha
        }

        val whitePaint = getAntiAliasPaint(Color.WHITE)
        val flatCircle = createBitmap(bitmapSize, bitmapSize)
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

        cameraDrawable = resourceProvider.getDrawable(R.drawable.ic_work_type_photos)
        cameraDrawableTransparent =
            resourceProvider.getDrawable(R.drawable.ic_work_type_photos).also {
                it.alpha = overlayAlpha
            }
        cameraDrawableVerticalOffset = resourceProvider.dpToPx(2f).toInt()
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
        isMarkedForDelete: Boolean,
        isFilteredOut: Boolean,
        isVisited: Boolean,
        hasPhotos: Boolean,
        isAssignedTeam: Boolean,
    ): BitmapDescriptor {
        val cacheKey = CacheKey(
            statusClaim,
            workType,
            hasMultipleWorkTypes = hasMultipleWorkTypes,
            isFavorite = isFavorite,
            isImportant = isImportant,
            isDuplicate = isDuplicate,
            isMarkedForDelete = isMarkedForDelete,
            isFilteredOut = isFilteredOut,
            isVisited = isVisited,
            hasPhotos = hasPhotos,
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
        isMarkedForDelete: Boolean,
        isFilteredOut: Boolean,
        isVisited: Boolean,
        hasPhotos: Boolean,
        isAssignedTeam: Boolean,
    ): Bitmap? {
        val cacheKey = CacheKey(
            statusClaim,
            workType,
            hasMultipleWorkTypes = hasMultipleWorkTypes,
            isDuplicate = isDuplicate,
            isMarkedForDelete = isMarkedForDelete,
            isFilteredOut = isFilteredOut,
            isVisited = isVisited,
            hasPhotos = hasPhotos,
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
        val flatShadow = createBitmap(bitmapSize, bitmapSize)
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
            isDuplicate = cacheKey.isDuplicate,
            isMarkedForDelete = cacheKey.isMarkedForDelete,
            isFilteredOut = cacheKey.isFilteredOut,
            isVisited = cacheKey.isVisited,
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

        fun drawOverlay(
            transparentDrawable: Drawable,
            drawable: Drawable,
            isLeftAligned: Boolean,
            verticalOffset: Int = 0,
        ) {
            val pd = if (cacheKey.isFilteredOut) transparentDrawable else drawable

            val horizontalOffsetStart = if (isLeftAligned) {
                0
            } else {
                rightBounds - pd.intrinsicWidth
            }
            val horizontalOffsetEnd = if (isLeftAligned) {
                pd.intrinsicWidth
            } else {
                rightBounds
            }
            val overlayBottom = bottomBounds - verticalOffset
            pd.setBounds(
                horizontalOffsetStart,
                overlayBottom - pd.intrinsicHeight,
                horizontalOffsetEnd,
                overlayBottom,
            )
            pd.draw(canvas)
        }

        if (cacheKey.hasMultipleWorkTypes) {
            synchronized(plusDrawable) {
                drawOverlay(
                    transparentDrawable = plusDrawableTransparent,
                    drawable = plusDrawable,
                    isLeftAligned = false,
                )
            }
        }

        if (cacheKey.hasPhotos) {
            synchronized(cameraDrawable) {
                drawOverlay(
                    transparentDrawable = cameraDrawableTransparent,
                    drawable = cameraDrawable,
                    isLeftAligned = true,
                    verticalOffset = cameraDrawableVerticalOffset,
                )
            }
        }

        if (isBlurred) {
            Canvas(blurred).apply {
                drawBitmap(output, 0f, 0f, null)
            }
        }

        var teamAssigned = blurred
        if (cacheKey.isAssignedTeam) {
            val composite = createBitmap(bitmapSize, bitmapSize)
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
        val isMarkedForDelete: Boolean = false,
        val isFilteredOut: Boolean = false,
        val isVisited: Boolean = false,
        val hasPhotos: Boolean = false,
        val isAssignedTeam: Boolean = false,
    )
}
