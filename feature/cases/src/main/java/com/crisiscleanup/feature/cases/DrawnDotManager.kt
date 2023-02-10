package com.crisiscleanup.feature.cases

import android.util.Log
import java.time.Clock
import kotlin.math.floor
import kotlin.math.roundToLong

// TODO Profile on actual devices and use if gains are noticeable
// Tracks dots drawn skipping where overlap would be unnoticeable
internal class DrawnDotManager(
    private val worksitesCount: Int,
    private val areaBuckets: Int = 256,
    /**
     * Zoom level that skips drawing dots in the same areas
     */
    private val farZoomThreshold: Int = 7,
    private val smallWorksitesCount: Int = 300,
) {
    // Tile size squared must be smaller than long max
    private var drawnDotIndexes = mutableSetOf<Long>()

    private var skipDotCounter = 0

    private var profileTimeMillis: Long = 0

    fun startTiming() {
        profileTimeMillis = Clock.systemUTC().millis()
    }

    val timingDeltaMillis: Long
        get() = Clock.systemUTC().millis() - profileTimeMillis

    val timingDelta: Double
        get() = timingDeltaMillis / 1000.0

    fun isDotOccupied(
        zoom: Int,
        xNorm: Double,
        yNorm: Double
    ): Boolean {
        if (worksitesCount < smallWorksitesCount ||
            zoom > farZoomThreshold
        ) {
            return false
        }

        val xFloor = floor(xNorm * areaBuckets).roundToLong()
        val yFloor = floor(yNorm * areaBuckets).roundToLong()
        val index = xFloor * areaBuckets + yFloor
        if (drawnDotIndexes.contains(index)) {
            skipDotCounter++
            return true
        }

        drawnDotIndexes.add(index)
        return false
    }

    fun reset() {
        drawnDotIndexes = mutableSetOf()
        skipDotCounter = 0
        startTiming()
    }

    fun logDebug() = Log.d("map-dot-draw", toString())

    override fun toString(): String =
        "Skipped $skipDotCounter, drawn ${drawnDotIndexes.size}, delta time $timingDeltaMillis"
}