package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.alpha
import androidx.core.graphics.get
import androidx.core.graphics.red
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.CaseStatus
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.crisiscleanup.core.model.data.WorkTypeType
import com.crisiscleanup.core.model.data.WorkTypeType.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.material.animation.ArgbEvaluatorCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkTypeIconProvider @Inject constructor(
    private val resourceProvider: AndroidResourceProvider,
) : MapCaseIconProvider {
    private val cache = LruCache<CacheKey, BitmapDescriptor>(64)
    private val bitmapCache = LruCache<CacheKey, Bitmap>(64)

    private val argbEvaluator = ArgbEvaluatorCompat()

    // TODO Make configurable
    private val bitmapSizeDp = 48f
    private val bitmapSize: Int
    private var bitmapCenterOffset = Offset(0f, 0f)

    override val iconOffset: Offset
        get() = bitmapCenterOffset

    private val plusDrawable: Drawable

    init {
        bitmapSize = resourceProvider.dpToPx(bitmapSizeDp).toInt()
        val centerOffset = bitmapSizeDp * 0.5f
        bitmapCenterOffset = Offset(centerOffset, centerOffset)

        plusDrawable = resourceProvider.getDrawable(R.drawable.ic_work_type_plus)
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
        hasMultipleWorkTypes: Boolean,
    ): BitmapDescriptor? {
        val cacheKey = CacheKey(statusClaim, workType, hasMultipleWorkTypes)
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
    ): Bitmap? {
        val cacheKey = CacheKey(statusClaim, workType, hasMultipleWorkTypes)
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

    private fun drawIcon(cacheKey: CacheKey): Bitmap {
        val status = statusClaimToStatus[cacheKey.statusClaim]

        val iconResId = statusIcons[cacheKey.workType] ?: R.drawable.ic_work_type_unknown
        val drawable = resourceProvider.getDrawable(iconResId)
        val output = Bitmap.createBitmap(
            bitmapSize,
            bitmapSize,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)

        // TODO Keep bounds squared and icon centered
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        val colors = mapMarkerColors[status] ?: mapMarkerColors[CaseStatus.Unknown]!!

        for (w in 0 until canvas.width) {
            for (h in 0 until canvas.height) {
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
                    val color = android.graphics.Color.argb(
                        alpha,
                        colorValue shr 16,
                        colorValue shr 8,
                        colorValue,
                    )
                    output.setPixel(w, h, color)
                }
            }
        }

        if (cacheKey.hasMultipleWorkTypes) {
            synchronized(plusDrawable) {
                plusDrawable.setBounds(
                    canvas.width - plusDrawable.intrinsicWidth,
                    canvas.height - plusDrawable.intrinsicHeight,
                    canvas.width,
                    canvas.height,
                )
                plusDrawable.draw(canvas)

            }
        }

        return output
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
        Fence to R.drawable.ic_work_type_fence,
        Fire to R.drawable.ic_work_type_fire,
        Food to R.drawable.ic_work_type_food,
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

private data class CacheKey(
    val statusClaim: WorkTypeStatusClaim,
    val workType: WorkTypeType,
    val hasMultipleWorkTypes: Boolean,
)