package com.crisiscleanup

import android.graphics.Bitmap
import android.graphics.Color
import com.crisiscleanup.core.common.QrCodeGenerator
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject

class ZxingQrCodeGenerator @Inject constructor() : QrCodeGenerator {
    override fun generate(payload: String, size: Int): Bitmap? {
        if (payload.isBlank()) {
            return null
        }

        val writer = QRCodeWriter()
        // Removing margin in image should add padding around image display
        val hints = mapOf(
            EncodeHintType.MARGIN to 0,
        )
        val bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (h in 0 until height) {
            val offset = h * width
            for (w in 0 until width) {
                pixels[offset + w] = if (bitMatrix.get(w, h)) Color.BLACK else Color.TRANSPARENT
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
