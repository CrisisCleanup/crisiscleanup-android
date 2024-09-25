package com.crisiscleanup.feature.qrcode.ui

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.OpenSettingsDialog
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.feature.qrcode.ScanQrCodeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrCodeRoute(
    onBack: () -> Unit,
    viewModel: ScanQrCodeViewModel = hiltViewModel(),
) {
    if (viewModel.isTeamQrCodeScanned) {
        onBack()
    }

    val t = LocalAppTranslator.current

    Column {
        TopAppBarBackAction(
            title = t("volunteerOrg.scan_qr_code"),
            onAction = onBack,
        )

        if (viewModel.hasCamera) {
            if (viewModel.isCameraPermissionGranted) {
                CameraView(viewModel.qrCodeAnalyzer)
            } else {
                Text(
                    t("info.camera_access_needed"),
                    listItemModifier.testTag("scanQrCodeCameraExplainer"),
                )
                CrisisCleanupButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .listItemPadding()
                        .testTag("scanQrCodeGrantCameraAccessAction"),
                    text = t("info.grant_camera_access"),
                    onClick = viewModel::requestCameraPermission,
                )
            }

            if (viewModel.showExplainPermissionCamera) {
                val closePermissionDialog =
                    remember(viewModel) { { viewModel.showExplainPermissionCamera = false } }
                ExplainCameraPermissionDialog(
                    showDialog = viewModel.showExplainPermissionCamera,
                    closeDialog = closePermissionDialog,
                    closeText = t("actions.close"),
                )
            }
        } else {
            Text(
                t("info.camera_not_found"),
                listItemModifier.testTag("scanQrCodeCameraNotFound"),
            )
        }
    }
}

@Composable
private fun ExplainCameraPermissionDialog(
    showDialog: Boolean,
    closeDialog: () -> Unit,
    closeText: String = "",
) {
    if (showDialog) {
        val t = LocalAppTranslator.current
        OpenSettingsDialog(
            t("info.allow_access_to_camera"),
            t("info.open_settings_to_grant_camera_access"),
            confirmText = t("info.app_settings"),
            dismissText = closeText,
            closeDialog = closeDialog,
        )
    }
}

@Composable
private fun CameraView(
    analyzer: ImageAnalysis.Analyzer,
) {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val preview = Preview.Builder().build()
        .also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    val resolutionStrategy = ResolutionStrategy(
        Size(1600, 1200),
        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
    )
    val resolutionSelector = ResolutionSelector.Builder()
        .setResolutionStrategy(resolutionStrategy)
        .build()
    val executor = ContextCompat.getMainExecutor(context)
    val imageAnalyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setResolutionSelector(resolutionSelector)
        .build()
        .also {
            it.setAnalyzer(executor, analyzer)
        }
    LaunchedEffect(lensFacing) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer,
        )
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("scanQrCodeCameraView"),
    ) {
        AndroidView(
            { previewView },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
