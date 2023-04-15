package com.crisiscleanup.core.common

import kotlinx.coroutines.flow.Flow

/**
 * Utility for reporting app connectivity status
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
    val isNotOnline: Flow<Boolean>
}
