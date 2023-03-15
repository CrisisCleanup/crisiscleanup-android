package com.crisiscleanup.core.common

import kotlinx.coroutines.flow.StateFlow

interface PermissionManager {
    /**
     * Only statuses requested, granted, or denied will be represented
     */
    val permissionChanges: StateFlow<Pair<String, PermissionStatus>>
    fun requestLocationPermission(): PermissionStatus
}

enum class PermissionStatus {
    Granted,
    Denied,
    ShowRationale,
    Requesting,
    Undefined,
}