package com.crisiscleanup.feature.qrcode

import android.content.pm.PackageManager
import android.net.Uri
import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.cameraPermissionGranted
import com.crisiscleanup.core.common.event.ExternalEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.queryParamMap
import com.crisiscleanup.feature.qrcode.navigation.ScanQrCodeArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ScanQrCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val permissionManager: PermissionManager,
    packageManager: PackageManager,
    private val externalEventBus: ExternalEventBus,
    @Logger(CrisisCleanupLoggers.Onboarding) private val logger: AppLogger,
) : ViewModel(), QrCodeListener {
    private val scanQrCodeArgs = ScanQrCodeArgs(savedStateHandle)
    private val isJoinTeamScan = scanQrCodeArgs.isJoinTeam

    val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    var showExplainPermissionCamera by mutableStateOf(false)
    var isCameraPermissionGranted by mutableStateOf(false)

    val qrCodeAnalyzer: ImageAnalysis.Analyzer = QrCodeAnalyzer(this)

    var isTeamQrCodeScanned by mutableStateOf(false)
        private set

    init {
        permissionManager.permissionChanges.map {
            if (it == cameraPermissionGranted) {
                isCameraPermissionGranted = true
            }
        }.launchIn(viewModelScope)

        if (requestCameraPermission()) {
            isCameraPermissionGranted = true
        }

        val showPersistentInvite = if (isJoinTeamScan) {
            externalEventBus.showTeamPersistentInvite
        } else {
            externalEventBus.showOrgPersistentInvite
        }
        showPersistentInvite
            .onEach {
                isQrCodeListening = !it
            }
            .launchIn(viewModelScope)

        // Org code scan is processed at the start of the navigation graph
        // Team code scan is processed from the previous route
        if (isJoinTeamScan) {
            externalEventBus.teamPersistentInvites
                .filter { it.isValidInvite }
                .onEach {
                    if (!isTeamQrCodeScanned) {
                        isTeamQrCodeScanned = true
                    }
                }
                .launchIn(viewModelScope)
        }
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

    private val orgScanAllowedPaths = setOf(
        "mobile_app_user_team_invite",
        "mobile_app_user_invite",
    )

    override fun onQrCode(payload: String) {
        try {
            val url = Uri.parse(payload)
            url.path?.split("/")?.lastOrNull()?.let { lastPath ->
                if (isJoinTeamScan) {
                    if (lastPath == "mobile_app_user_team_invite") {
                        externalEventBus.onTeamPersistentInvite(url.queryParamMap)
                    }
                } else if (orgScanAllowedPaths.contains(lastPath)) {
                    // TODO Test join team from org scan
                    externalEventBus.onOrgPersistentInvite(url.queryParamMap)
                }
            }
        } catch (e: Exception) {
            logger.logDebug(e.message ?: "QR code is not a URL")
        }
    }
}
