package com.crisiscleanup.core.common

import android.graphics.Color

fun String.hexColorToIntColor(fallbackColor: Int = Color.TRANSPARENT): Int {
    try {
        return Color.parseColor(this)
    } catch (e: Exception) {
        // Keep default color
    }
    return fallbackColor
}
