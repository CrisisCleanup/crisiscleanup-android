package com.crisiscleanup.core.data.util

import kotlinx.coroutines.flow.Flow

/**
 * Utility for reporting app connectivity status
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
    val isNotOnline: Flow<Boolean>
}
