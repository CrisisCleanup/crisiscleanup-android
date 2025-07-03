package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.alpha
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.red
import androidx.core.graphics.set
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.crisiscleanup.core.model.data.WorkTypeType
import com.crisiscleanup.core.model.data.WorkTypeType.AnimalServices
import com.crisiscleanup.core.model.data.WorkTypeType.Ash
import com.crisiscleanup.core.model.data.WorkTypeType.CatchmentGutters
import com.crisiscleanup.core.model.data.WorkTypeType.ChimneyCapping
import com.crisiscleanup.core.model.data.WorkTypeType.ConstructionConsultation
import com.crisiscleanup.core.model.data.WorkTypeType.CoreReliefItems
import com.crisiscleanup.core.model.data.WorkTypeType.Debris
import com.crisiscleanup.core.model.data.WorkTypeType.DeferredMaintenance
import com.crisiscleanup.core.model.data.WorkTypeType.Demolition
import com.crisiscleanup.core.model.data.WorkTypeType.DomesticServices
import com.crisiscleanup.core.model.data.WorkTypeType.Erosion
import com.crisiscleanup.core.model.data.WorkTypeType.Escort
import com.crisiscleanup.core.model.data.WorkTypeType.Favorite
import com.crisiscleanup.core.model.data.WorkTypeType.Fence
import com.crisiscleanup.core.model.data.WorkTypeType.Fire
import com.crisiscleanup.core.model.data.WorkTypeType.Food
import com.crisiscleanup.core.model.data.WorkTypeType.Heating
import com.crisiscleanup.core.model.data.WorkTypeType.Important
import com.crisiscleanup.core.model.data.WorkTypeType.Landslide
import com.crisiscleanup.core.model.data.WorkTypeType.Leak
import com.crisiscleanup.core.model.data.WorkTypeType.Meals
import com.crisiscleanup.core.model.data.WorkTypeType.MoldRemediation
import com.crisiscleanup.core.model.data.WorkTypeType.MuckOut
import com.crisiscleanup.core.model.data.WorkTypeType.Other
import com.crisiscleanup.core.model.data.WorkTypeType.Oxygen
import com.crisiscleanup.core.model.data.WorkTypeType.Pipe
import com.crisiscleanup.core.model.data.WorkTypeType.Ppe
import com.crisiscleanup.core.model.data.WorkTypeType.Prescription
import com.crisiscleanup.core.model.data.WorkTypeType.Rebuild
import com.crisiscleanup.core.model.data.WorkTypeType.RebuildTotal
import com.crisiscleanup.core.model.data.WorkTypeType.RetardantCleanup
import com.crisiscleanup.core.model.data.WorkTypeType.Sandbagging
import com.crisiscleanup.core.model.data.WorkTypeType.Shelter
import com.crisiscleanup.core.model.data.WorkTypeType.Shopping
import com.crisiscleanup.core.model.data.WorkTypeType.SmokeDamage
import com.crisiscleanup.core.model.data.WorkTypeType.SnowGround
import com.crisiscleanup.core.model.data.WorkTypeType.SnowRoof
import com.crisiscleanup.core.model.data.WorkTypeType.Structure
import com.crisiscleanup.core.model.data.WorkTypeType.Tarp
import com.crisiscleanup.core.model.data.WorkTypeType.TemporaryHousing
import com.crisiscleanup.core.model.data.WorkTypeType.Trees
import com.crisiscleanup.core.model.data.WorkTypeType.TreesHeavyEquipment
import com.crisiscleanup.core.model.data.WorkTypeType.Unknown
import com.crisiscleanup.core.model.data.WorkTypeType.WaterBottles
import com.crisiscleanup.core.model.data.WorkTypeType.WellnessCheck
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
    private val cache = LruCache<WorkTypeIconCacheKey, BitmapDescriptor>(64)
    private val bitmapCache = LruCache<WorkTypeIconCacheKey, Bitmap>(64)

    private val argbEvaluator = ArgbEvaluatorCompat()

    // TODO Parameterize values

    private val shadowRadiusDp = 3f
    private val shadowRadius: Int
    private val shadowColor = (0xFF666666).toInt()

    private val bitmapSizeDp = 40f + 2 * shadowRadiusDp
    private val bitmapSize: Int
    private var bitmapCenterOffset = Offset(0f, 0f)

    override val iconOffset: Offset
        get() = bitmapCenterOffset

    private val plusDrawable: Drawable
    private val plusDrawableTransparent: Drawable

    private val cameraDrawable: Drawable
    private val cameraDrawableTransparent: Drawable
    private val cameraDrawableVerticalOffset: Int

    init {
        bitmapSize = resourceProvider.dpToPx(bitmapSizeDp).toInt()
        val centerOffset = bitmapSizeDp * 0.5f
        bitmapCenterOffset = Offset(centerOffset, centerOffset)

        shadowRadius = resourceProvider.dpToPx(shadowRadiusDp).toInt()

        val overlayAlpha = (255 * FILTERED_OUT_MARKER_ALPHA).toInt()
        plusDrawable = resourceProvider.getDrawable(R.drawable.ic_work_type_plus)
        plusDrawableTransparent = resourceProvider.getDrawable(R.drawable.ic_work_type_plus).also {
            it.alpha = overlayAlpha
        }

        cameraDrawable = resourceProvider.getDrawable(R.drawable.ic_work_type_photos)
        cameraDrawableTransparent =
            resourceProvider.getDrawable(R.drawable.ic_work_type_photos).also {
                it.alpha = overlayAlpha
            }
        cameraDrawableVerticalOffset = resourceProvider.dpToPx(2f).toInt()
    }

    private fun cacheIconBitmap(cacheKey: WorkTypeIconCacheKey): BitmapDescriptor {
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
        hasPhotos: Boolean,
    ): BitmapDescriptor {
        val cacheKey = WorkTypeIconCacheKey(
            statusClaim,
            workType,
            hasMultipleWorkTypes,
            isFavorite = isFavorite,
            isImportant = isImportant,
            isDuplicate = isDuplicate,
            isFilteredOut = isFilteredOut,
            isVisited = isVisited,
            hasPhotos = hasPhotos,
        )
        synchronized(cache) {
            cache.get(cacheKey)?.let {
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
        hasPhotos: Boolean,
    ): Bitmap? {
        val cacheKey = WorkTypeIconCacheKey(
            statusClaim,
            workType,
            hasMultipleWorkTypes,
            isDuplicate,
            isFilteredOut,
            isVisited,
            hasPhotos,
        )
        synchronized(cache) {
            bitmapCache.get(cacheKey)?.let {
                return it
            }
        }

        cacheIconBitmap(cacheKey)
        synchronized(cache) {
            bitmapCache.get(cacheKey)?.let {
                return it
            }
            return null
        }
    }

    private fun drawIcon(cacheKey: WorkTypeIconCacheKey): Bitmap {
        val iconResId = if (cacheKey.isFavorite) {
            statusIcons[Favorite]!!
        } else if (cacheKey.isImportant) {
            statusIcons[Important]!!
        } else {
            statusIcons[cacheKey.workType] ?: R.drawable.ic_work_type_unknown
        }

        val drawable = resourceProvider.getDrawable(iconResId)
        val output = createBitmap(bitmapSize, bitmapSize)
        val canvas = Canvas(output)

        // TODO Keep bounds squared and icon centered
        val rightBounds = canvas.width - shadowRadius
        val bottomBounds = canvas.height - shadowRadius
        drawable.setBounds(
            shadowRadius,
            shadowRadius,
            rightBounds,
            bottomBounds,
        )
        drawable.draw(canvas)

        val colors = getMapMarkerColors(
            cacheKey.statusClaim,
            cacheKey.isDuplicate,
            cacheKey.isFilteredOut,
            cacheKey.isVisited,
            isDot = false,
        )
        val fillAlpha = if (colors.fill.alpha < 1) (colors.fill.alpha * 255).toInt() else 255

        for (w in shadowRadius until rightBounds) {
            for (h in shadowRadius until bottomBounds) {
                val p = output[w, h]
                val alpha = p.alpha
                if (alpha > 0) {
                    val grayscale = p.red.toFloat() / 255f
                    val colorValue =
                        argbEvaluator.evaluate(
                            grayscale,
                            colors.fillInt,
                            colors.strokeInt,
                        )
                    val color = Color.argb(
                        fillAlpha,
                        (colorValue and 0xFF0000) shr 16,
                        (colorValue and 0x00FF00) shr 8,
                        (colorValue and 0x0000FF),
                    )
                    output[w, h] = color
                }
            }
        }

        var blurred = output
        if (!cacheKey.isFilteredOut) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = PorterDuffColorFilter(shadowColor, PorterDuff.Mode.SRC_IN)
            }
            val flatShadow = createBitmap(bitmapSize, bitmapSize)
            Canvas(flatShadow).apply {
                drawBitmap(output, Matrix(), paint)
            }
            blurred = Toolkit.blur(flatShadow, shadowRadius)
        }

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

        Canvas(blurred).apply {
            drawBitmap(output, 0f, 0f, null)
        }

        return blurred
    }

    private val statusIcons = mapOf(
        AnimalServices to R.drawable.ic_work_type_animal_services,
        Ash to R.drawable.ic_work_type_ash,
        CatchmentGutters to R.drawable.ic_work_type_catchment_gutters,
        ChimneyCapping to R.drawable.ic_work_type_chimney_capping,
        ConstructionConsultation to R.drawable.ic_work_type_construction_consultation,
        CoreReliefItems to R.drawable.ic_work_type_core_relief_items,
        Debris to R.drawable.ic_work_type_debris,
        DeferredMaintenance to R.drawable.ic_work_type_deferred_maintenance,
        Demolition to R.drawable.ic_work_type_demolition,
        DomesticServices to R.drawable.ic_work_type_domestic_services,
        Erosion to R.drawable.ic_work_type_erosion,
        Escort to R.drawable.ic_work_type_escort,
        Favorite to R.drawable.ic_work_type_favorite,
        Fence to R.drawable.ic_work_type_fence,
        Fire to R.drawable.ic_work_type_fire,
        Food to R.drawable.ic_work_type_food,
        Heating to R.drawable.ic_work_type_heat,
        Important to R.drawable.ic_work_type_important,
        Landslide to R.drawable.ic_work_type_landslide,
        Leak to R.drawable.ic_work_type_leak,
        Meals to R.drawable.ic_work_type_meals,
        MoldRemediation to R.drawable.ic_work_type_mold_remediation,
        MuckOut to R.drawable.ic_work_type_muck_out,
        Other to R.drawable.ic_work_type_other,
        Oxygen to R.drawable.ic_work_type_oxygen,
        Pipe to R.drawable.ic_work_type_pipe,
        Ppe to R.drawable.ic_work_type_ppe,
        Prescription to R.drawable.ic_work_type_prescription,
        Rebuild to R.drawable.ic_work_type_rebuild,
        RebuildTotal to R.drawable.ic_work_type_rebuild_total,
        RetardantCleanup to R.drawable.ic_work_type_retardant_cleanup,
        Sandbagging to R.drawable.ic_work_type_sandbagging,
        Shelter to R.drawable.ic_work_type_shelter,
        Shopping to R.drawable.ic_work_type_shopping,
        SmokeDamage to R.drawable.ic_work_type_smoke_damage,
        SnowGround to R.drawable.ic_work_type_snow_ground,
        SnowRoof to R.drawable.ic_work_type_snow_roof,
        Structure to R.drawable.ic_work_type_structure,
        Tarp to R.drawable.ic_work_type_tarp,
        TemporaryHousing to R.drawable.ic_work_type_temporary_housing,
        Trees to R.drawable.ic_work_type_trees,
        TreesHeavyEquipment to R.drawable.ic_work_type_trees_heavy_equipment,
        Unknown to R.drawable.ic_work_type_unknown,
        WaterBottles to R.drawable.ic_work_type_water_bottles,
        WellnessCheck to R.drawable.ic_work_type_wellness_check,
    )
}

private data class WorkTypeIconCacheKey(
    val statusClaim: WorkTypeStatusClaim,
    val workType: WorkTypeType,
    val hasMultipleWorkTypes: Boolean,
    val isFavorite: Boolean = false,
    val isImportant: Boolean = false,
    val isDuplicate: Boolean = false,
    val isFilteredOut: Boolean = false,
    val isVisited: Boolean = false,
    val hasPhotos: Boolean = false,
)
