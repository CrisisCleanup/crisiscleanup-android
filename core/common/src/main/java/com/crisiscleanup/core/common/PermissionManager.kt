package com.crisiscleanup.core.common

import android.Manifest
import kotlinx.coroutines.flow.StateFlow

interface PermissionManager {
    /**
     * Only statuses requested, granted, or denied will be represented
     */
    val permissionChanges: StateFlow<Pair<String, PermissionStatus>>
    fun requestLocationPermission(): PermissionStatus
}

val locationPermissionGranted = Pair(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    PermissionStatus.Granted,
)

enum class PermissionStatus {
    Granted,
    Denied,
    ShowRationale,
    Requesting,
    Undefined,
}