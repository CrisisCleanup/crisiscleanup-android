package com.crisiscleanup.core.mapmarker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.core.graphics.alpha
import androidx.core.graphics.get
import androidx.core.graphics.red
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.google.android.material.animation.ArgbEvaluatorCompat

fun AndroidResourceProvider.createDrawableBitmap(
    @DrawableRes iconResId: Int,
    bitmapSize: Int,
    leftBounds: Int,
    rightBounds: Int,
    topBounds: Int,
    bottomBounds: Int,
): Pair<Bitmap, Canvas> {
    val drawable = getDrawable(iconResId)
    val output = Bitmap.createBitmap(
        bitmapSize,
        bitmapSize,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(output)

    drawable.setBounds(
        leftBounds,
        topBounds,
        rightBounds,
        bottomBounds,
    )
    drawable.draw(canvas)

    return Pair(output, canvas)
}

fun Bitmap.applyColors(
    colors: MapMarkerColor,
    leftBounds: Int,
    rightBounds: Int,
    topBounds: Int,
    bottomBounds: Int,
    argbEvaluator: ArgbEvaluatorCompat,
) {
    val fillAlpha = if (colors.fill.alpha < 1) (colors.fill.alpha * 255).toInt() else 255

    for (w in leftBounds until rightBounds) {
        for (h in topBounds until bottomBounds) {
            val p = this[w, h]
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
                setPixel(w, h, color)
            }
        }
    }
}