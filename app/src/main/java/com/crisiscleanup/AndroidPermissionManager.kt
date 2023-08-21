package com.crisiscleanup

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPermissionManager @Inject constructor(
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : PermissionManager, DefaultLifecycleObserver {
    override val hasLocationPermission = MutableStateFlow(false)

    override val permissionChanges = MutableStateFlow(Pair("", PermissionStatus.Undefined))

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    private var activityWr: WeakReference<ComponentActivity> = WeakReference(null)

    private val screenshotReadPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            READ_MEDIA_IMAGES
        } else {
            READ_EXTERNAL_STORAGE
        }

    override fun onCreate(owner: LifecycleOwner) {
        (owner as? ComponentActivity)?.let { activity ->
            activityWr = WeakReference(activity)
            publishLocationPermission()

            requestPermissionLauncher =
                activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    // Assume unchanged permission since requested
                    val permission = permissionChanges.value.first
                    val status =
                        if (isGranted) {
                            PermissionStatus.Granted
                        } else {
                            PermissionStatus.Denied
                        }
                    permissionChanges.value = Pair(permission, status)

                    if (permission == ACCESS_COARSE_LOCATION || permission == ACCESS_FINE_LOCATION) {
                        publishLocationPermission()
                    }
                }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        requestPermissionLauncher?.unregister()
    }

    private fun publishLocationPermission() {
        hasLocationPermission.value = hasPermission(ACCESS_COARSE_LOCATION) ||
            hasPermission(ACCESS_FINE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        activityWr.get()?.let { activity ->
            return ContextCompat.checkSelfPermission(
                activity,
                permission,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    private fun requestPermission(permission: String): PermissionStatus {
        activityWr.get()?.let { activity ->
            val permissionStatus = ContextCompat.checkSelfPermission(activity, permission)
            return when {
                permissionStatus == PackageManager.PERMISSION_GRANTED -> PermissionStatus.Granted
                shouldShowRequestPermissionRationale(
                    activity,
                    permission,
                ) -> PermissionStatus.ShowRationale

                else -> {
                    permissionChanges.value = Pair(permission, PermissionStatus.Requesting)
                    requestPermissionLauncher?.launch(permission)
                    PermissionStatus.Requesting
                }
            }
        }
        return PermissionStatus.Undefined
    }

    override fun requestLocationPermission() = requestPermission(ACCESS_COARSE_LOCATION)
    override fun requestCameraPermission() = requestPermission(CAMERA)

    override fun requestScreenshotReadPermission() = requestPermission(screenshotReadPermission)
}
