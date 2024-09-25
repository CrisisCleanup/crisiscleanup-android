package com.crisiscleanup.feature.qrcode

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

internal interface QrCodeListener {
    val isQrCodeListening: Boolean

    fun onQrCode(payload: String)
}

internal class QrCodeAnalyzer(
    private val listener: QrCodeListener,
    private val throttleMillis: Long = 500,
) : ImageAnalysis.Analyzer {
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    private val scanner = BarcodeScanning.getClient(options)
    private var lastProcessedMillis = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (listener.isQrCodeListening &&
            System.currentTimeMillis() - lastProcessedMillis > throttleMillis
        ) {
            lastProcessedMillis = System.currentTimeMillis()

            image.image?.let { mediaImage ->
                val targetImage =
                    InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                scanner.process(targetImage)
                    .addOnSuccessListener { barcodes ->
                        var isFound = false
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { payload ->
                                listener.onQrCode(payload)
                                isFound = true
                            }
                            if (isFound) {
                                break
                            }
                        }
                    }
                    .addOnCompleteListener {
                        image.close()
                    }

                return
            }
        }

        image.close()
    }
}
