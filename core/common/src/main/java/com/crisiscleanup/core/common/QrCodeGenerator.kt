package com.crisiscleanup.core.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import qrcode.QRCode

interface QrCodeGenerator {
    fun generate(payload: String): Bitmap?
}

class QrCodeKotlinGenerator : QrCodeGenerator {
    override fun generate(payload: String): Bitmap? {
        val code = QRCode.ofSquares()
            .build(payload)

        val pngBytes = code.renderToBytes()
        return BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
    }
}