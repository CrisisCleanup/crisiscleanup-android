package com.crisiscleanup.core.mapmarker.tiler

import android.graphics.Bitmap

fun squareBitmap(sizePx: Int): Bitmap = Bitmap.createBitmap(
    sizePx,
    sizePx,
    Bitmap.Config.ARGB_8888,
)
