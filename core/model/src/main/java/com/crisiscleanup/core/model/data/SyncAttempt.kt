package com.crisiscleanup.core.model.data

import kotlin.math.ln
import kotlin.time.Clock

data class SyncAttempt(
    val successfulSeconds: Long,
    val attemptedSeconds: Long,
    val attemptedCounter: Int,
) {
    fun isRecent(
        recentIntervalSeconds: Int = 1800,
        nowSeconds: Long = Clock.System.now().epochSeconds,
    ): Boolean = nowSeconds - successfulSeconds < recentIntervalSeconds

    fun isBackingOff(
        backoffIntervalSeconds: Int = 15,
        nowSeconds: Long = Clock.System.now().epochSeconds,
    ): Boolean {
        if (attemptedCounter < 1) {
            return false
        }

        val deltaSeconds = (nowSeconds - attemptedSeconds).coerceAtLeast(1)
        if (deltaSeconds > 3600) {
            return false
        }

        val intervalSeconds = backoffIntervalSeconds.coerceAtLeast(1)
        // now < attempted + interval * 2^(tries-1)
        val lhs = ln(deltaSeconds / intervalSeconds.toFloat())
        val rhs = (attemptedCounter - 1) * ln(2f)
        return lhs < rhs
    }

    fun shouldSyncPassively(
        recentIntervalSeconds: Int = 1800,
        backoffIntervalSeconds: Int = 15,
        nowSeconds: Long = Clock.System.now().epochSeconds,
    ) = !(
        isRecent(recentIntervalSeconds, nowSeconds) ||
            isBackingOff(backoffIntervalSeconds, nowSeconds)
        )
}
