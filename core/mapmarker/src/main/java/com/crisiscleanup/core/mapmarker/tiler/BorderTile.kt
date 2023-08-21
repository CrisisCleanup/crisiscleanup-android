package com.crisiscleanup.core.mapmarker.tiler

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

// Helper for tiling visuals
class BorderTile(private val sizePx: Int) {
    private var borderTile: Bitmap? = null

    private fun drawTileBorder(): Bitmap {
        if (borderTile == null) {
            val bitmap = squareBitmap(sizePx)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
            }
            Canvas(bitmap).apply {
                drawRect(
                    0f,
                    0f,
                    bitmap.width.toFloat(),
                    bitmap.height.toFloat(),
                    borderPaint,
                )
            }
            synchronized(this) {
                borderTile = bitmap
            }
        }
        return borderTile!!
    }

    fun copy(): Bitmap {
        var copy: Bitmap
        synchronized(this) {
            val source = drawTileBorder()
            copy = source.copy(Bitmap.Config.ARGB_8888, true)
        }
        return copy
    }
}
