package com.crisiscleanup.core.data.test

import com.crisiscleanup.core.common.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class AlwaysOnlineNetworkMonitor @Inject constructor() : NetworkMonitor {
    override val isOnline: Flow<Boolean> = flowOf(true)
    override val isNotOnline: Flow<Boolean> = flowOf(false)
}
