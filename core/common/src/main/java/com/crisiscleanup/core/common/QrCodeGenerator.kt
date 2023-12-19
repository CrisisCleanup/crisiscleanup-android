package com.crisiscleanup.core.common

import android.graphics.Bitmap

interface QrCodeGenerator {
    fun generate(payload: String, size: Int = 512): Bitmap?
}
