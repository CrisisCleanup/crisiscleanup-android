package com.crisiscleanup.core.mapmarker.tiler

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

// Helper for tiling visuals
class BorderTile(private val sizePx: Int) {
    private var borderTile: Bitmap? = null

    private fun drawTileBorder(): Bitmap {
        synchronized(this) {
            if (borderTile == null) {
                val bitmap = squareBitmap(sizePx)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                borderPaint.style = Paint.Style.STROKE
                val canvas = Canvas(bitmap)
                canvas.drawRect(
                    0f, 0f,
                    bitmap.width.toFloat(), bitmap.height.toFloat(),
                    borderPaint
                )
                borderTile = bitmap
            }
            return borderTile!!
        }
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
