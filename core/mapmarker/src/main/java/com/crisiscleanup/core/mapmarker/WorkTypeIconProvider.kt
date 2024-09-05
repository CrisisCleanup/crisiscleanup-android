package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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

    private val bitmapSizeDp = 36f + 2 * shadowRadiusDp
    private val bitmapSize: Int
    private var bitmapCenterOffset = Offset(0f, 0f)

    override val iconOffset: Offset
        get() = bitmapCenterOffset

    private val plusDrawable: Drawable
    private val plusDrawableTransparent: Drawable

    init {
        bitmapSize = resourceProvider.dpToPx(bitmapSizeDp).toInt()
        val centerOffset = bitmapSizeDp * 0.5f
        bitmapCenterOffset = Offset(centerOffset, centerOffset)

        shadowRadius = resourceProvider.dpToPx(shadowRadiusDp).toInt()

        plusDrawable = resourceProvider.getDrawable(R.drawable.ic_work_type_plus)
        plusDrawableTransparent = resourceProvider.getDrawable(R.drawable.ic_work_type_plus).also {
            it.alpha = (255 * FILTERED_OUT_MARKER_ALPHA).toInt()
        }
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
    ): BitmapDescriptor {
        val cacheKey = CacheKey(
            statusClaim,
            workType,
            hasMultipleWorkTypes,
            isFavorite = isFavorite,
            isImportant = isImportant,
            isDuplicate = isDuplicate,
            isFilteredOut = isFilteredOut,
            isVisited = isVisited,
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
    ): Bitmap? {
        val cacheKey = CacheKey(
            statusClaim,
            workType,
            hasMultipleWorkTypes,
            isDuplicate,
            isFilteredOut,
            isVisited,
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

        var blurred = output
        if (!cacheKey.isFilteredOut) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = PorterDuffColorFilter(shadowColor, PorterDuff.Mode.SRC_IN)
            }
            val flatShadow = Bitmap.createBitmap(
                bitmapSize,
                bitmapSize,
                Bitmap.Config.ARGB_8888,
            )
            Canvas(flatShadow).apply {
                drawBitmap(output, Matrix(), paint)
            }
            blurred = Toolkit.blur(flatShadow, shadowRadius)
        }

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

        Canvas(blurred).apply {
            drawBitmap(output, 0f, 0f, null)
        }

        return blurred
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
    )
}
