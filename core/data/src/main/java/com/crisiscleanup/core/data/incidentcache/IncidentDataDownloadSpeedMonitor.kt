package com.crisiscleanup.core.data.incidentcache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

interface DataDownloadSpeedMonitor {
    val isSlowSpeed: Flow<Boolean>

    fun onSpeedChange(isSlow: Boolean)
}

@Singleton
class IncidentDataDownloadSpeedMonitor @Inject constructor() : DataDownloadSpeedMonitor {
    private val isSlowSpeedInternal = MutableSharedFlow<Boolean>(1)
    override val isSlowSpeed = isSlowSpeedInternal.distinctUntilChanged()

    override fun onSpeedChange(isSlow: Boolean) {
        isSlowSpeedInternal.tryEmit(isSlow)
    }
}
