package com.crisiscleanup.feature.authentication

import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.cameraPermissionGranted
import com.crisiscleanup.core.common.event.ExternalEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Onboarding
import com.crisiscleanup.core.common.log.Logger
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ScanQrCodeViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    packageManager: PackageManager,
    private val externalEventBus: ExternalEventBus,
    @Logger(Onboarding) private val logger: AppLogger,
) : ViewModel(), QrCodeListener {
    val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    var showExplainPermissionCamera by mutableStateOf(false)
    var isCameraPermissionGranted by mutableStateOf(false)

    val qrCodeAnalyzer: ImageAnalysis.Analyzer = QrCodeAnalyzer(this)

    init {
        permissionManager.permissionChanges.map {
            if (it == cameraPermissionGranted) {
                isCameraPermissionGranted = true
            }
        }.launchIn(viewModelScope)

        if (requestCameraPermission()) {
            isCameraPermissionGranted = true
        }

        externalEventBus.showOrgPersistentInvite
            .onEach {
                isQrCodeListening = !it
            }
            .launchIn(viewModelScope)
    }

    fun requestCameraPermission(): Boolean {
        when (permissionManager.requestCameraPermission()) {
            PermissionStatus.Granted -> {
                return true
            }

            PermissionStatus.ShowRationale -> {
                showExplainPermissionCamera = true
            }

            PermissionStatus.Requesting,
            PermissionStatus.Denied,
            PermissionStatus.Undefined,
            -> {
                // Ignore these statuses as they're not important
            }
        }
        return false
    }

    // QrCodeListener

    override var isQrCodeListening: Boolean = true
        private set

    override fun onQrCode(payload: String) {
        try {
            val url = Uri.parse(payload)
            url.path?.split("/")?.lastOrNull()?.let { lastPath ->
                if (lastPath == "mobile_app_user_invite") {
                    val params = mutableMapOf<String, String>()
                    for (key in url.queryParameterNames) {
                        url.getQueryParameter(key)?.let { value ->
                            params[key] = value
                        }
                    }
                    if (params.isNotEmpty()) {
                        externalEventBus.onOrgPersistentInvite(params)
                    }
                }
            }
        } catch (e: Exception) {
            logger.logDebug(e.message ?: "QR code is not a URL")
        }
    }
}

private interface QrCodeListener {
    val isQrCodeListening: Boolean

    fun onQrCode(payload: String)
}

private class QrCodeAnalyzer(
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
