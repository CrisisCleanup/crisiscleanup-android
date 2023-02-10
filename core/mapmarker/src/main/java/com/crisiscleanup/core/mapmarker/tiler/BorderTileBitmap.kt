package com.crisiscleanup.core.mapmarker.tiler

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

// Use as visual helper in tiling
fun makeTileBorderBitmap(tileSizePx: Int): Bitmap {
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    borderPaint.style = Paint.Style.STROKE
    val borderTile = Bitmap.createBitmap(
        tileSizePx,
        tileSizePx,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(borderTile)
    canvas.drawRect(
        0f, 0f, tileSizePx.toFloat(), tileSizePx.toFloat(), borderPaint
    )
    return borderTile
}

fun copyBorderTile(borderTile: Bitmap): Bitmap? {
    var copy: Bitmap?
    synchronized(borderTile) {
        copy = borderTile.copy(Bitmap.Config.ARGB_8888, true)
    }
    return copy
}
