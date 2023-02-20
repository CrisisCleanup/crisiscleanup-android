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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.material.animation.ArgbEvaluatorCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkTypeIconProvider @Inject constructor(
    private val resourceProvider: AndroidResourceProvider,
    private val logger: AppLogger,
) : MapCaseIconProvider {
    private val cache = LruCache<WorkTypeStatusClaim, BitmapDescriptor>(32)
    private val bitmapCache = LruCache<WorkTypeStatusClaim, Bitmap>(32)

    private val argbEvaluator = ArgbEvaluatorCompat()

    // TODO Make configurable
    private val bitmapSizeDp = 48f
    private val bitmapSize: Int

    init {
        logger.tag = "map-icon"

        bitmapSize = resourceProvider.dpToPx(bitmapSizeDp).toInt()
    }

    private fun cacheIconBitmap(statusClaim: WorkTypeStatusClaim): BitmapDescriptor {
        val bitmap = drawIcon(statusClaim)
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        synchronized(cache) {
            bitmapCache.put(statusClaim, bitmap)
            cache.put(statusClaim, bitmapDescriptor)
            return bitmapDescriptor
        }
    }

    override fun getIcon(statusClaim: WorkTypeStatusClaim): BitmapDescriptor? {
        synchronized(cache) {
            cache.get(statusClaim)?.let {
                return it
            }
        }

        return cacheIconBitmap(statusClaim)
    }

    override fun getIconBitmap(statusClaim: WorkTypeStatusClaim): Bitmap? {
        synchronized(cache) {
            bitmapCache.get(statusClaim)?.let {
                return it
            }
        }

        cacheIconBitmap(statusClaim)
        synchronized(cache) {
            bitmapCache.get(statusClaim)?.let {
                return it
            }
            return null
        }
    }

    private fun drawIcon(statusClaim: WorkTypeStatusClaim): Bitmap {
        val status = statusClaimToStatus[statusClaim]

        val drawable = resourceProvider.getDrawable(R.drawable.ic_work_type_trees)
        val output = Bitmap.createBitmap(
            bitmapSize,
            bitmapSize,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)

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
                            colors.strokeInt,
                            colors.fillInt,
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
}