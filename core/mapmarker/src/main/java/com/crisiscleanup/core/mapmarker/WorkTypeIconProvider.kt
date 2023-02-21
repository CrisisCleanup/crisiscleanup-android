package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.collection.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.alpha
import androidx.core.graphics.get
import androidx.core.graphics.red
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.model.data.CaseStatus
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.crisiscleanup.core.model.data.WorkTypeType
import com.crisiscleanup.core.model.data.WorkTypeType.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.material.animation.ArgbEvaluatorCompat
import javax.inject.Inject
import javax.inject.Singleton

typealias TypeStatusClaim = Pair<WorkTypeStatusClaim, WorkTypeType>

@Singleton
class WorkTypeIconProvider @Inject constructor(
    private val resourceProvider: AndroidResourceProvider,
    private val logger: AppLogger,
) : MapCaseIconProvider {
    private val cache = LruCache<TypeStatusClaim, BitmapDescriptor>(32)
    private val bitmapCache = LruCache<WorkTypeStatusClaim, Bitmap>(32)

    private val argbEvaluator = ArgbEvaluatorCompat()

    // TODO Make configurable
    private val bitmapSizeDp = 48f
    private val bitmapSize: Int
    private var bitmapCenterOffset: Offset = Offset(0f, 0f)

    override val iconOffset: Offset
        get() = bitmapCenterOffset

    init {
        logger.tag = "map-icon"

        bitmapSize = resourceProvider.dpToPx(bitmapSizeDp).toInt()
        val centerOffset = bitmapSizeDp * 0.5f
        bitmapCenterOffset = Offset(centerOffset, centerOffset)
    }

    private fun cacheIconBitmap(
        statusClaim: WorkTypeStatusClaim,
        workType: WorkTypeType,
    ): BitmapDescriptor {
        val bitmap = drawIcon(statusClaim, workType)
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        synchronized(cache) {
            bitmapCache.put(statusClaim, bitmap)
            cache.put(Pair(statusClaim, workType), bitmapDescriptor)
            return bitmapDescriptor
        }
    }

    override fun getIcon(
        statusClaim: WorkTypeStatusClaim,
        workType: WorkTypeType,
    ): BitmapDescriptor? {
        synchronized(cache) {
            cache.get(Pair(statusClaim, workType))?.let {
                return it
            }
        }

        return cacheIconBitmap(statusClaim, workType)
    }

    override fun getIconBitmap(
        statusClaim: WorkTypeStatusClaim,
        workType: WorkTypeType,
    ): Bitmap? {
        synchronized(cache) {
            bitmapCache.get(statusClaim)?.let {
                return it
            }
        }

        cacheIconBitmap(statusClaim, workType)
        synchronized(cache) {
            bitmapCache.get(statusClaim)?.let {
                return it
            }
            return null
        }
    }

    private fun drawIcon(
        statusClaim: WorkTypeStatusClaim,
        workType: WorkTypeType,
    ): Bitmap {
        val status = statusClaimToStatus[statusClaim]

        val iconResId = statusIcons[workType] ?: R.drawable.ic_work_type_unknown
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

        // TODO Draw plus over if there are multiple

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
