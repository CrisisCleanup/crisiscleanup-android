package com.crisiscleanup.core.data.incidentcache

import kotlinx.datetime.Clock

class CountTimeTracker {
    private val counts = mutableListOf<CountTime>()

    suspend fun <T> time(operation: suspend () -> List<T>): List<T> {
        val startDownloadTime = Clock.System.now()
        val result = operation()
        val endDownloadTime = Clock.System.now()
        val downloadSeconds = (endDownloadTime - startDownloadTime).inWholeSeconds
        onCountTime(result.size, downloadSeconds.toFloat())
        return result
    }

    private fun onCountTime(count: Int, timeSeconds: Float) {
        counts.add(
            CountTime(
                count = count,
                timeSeconds = timeSeconds,
            ),
        )
    }

    fun averageSpeed(): Float {
        val countSnapshot = counts.size
        var totalCount = 0
        var totalSeconds = 0f
        var zeroCounts = 0
        for (i in 0..<countSnapshot) {
            with(counts[i]) {
                if (count == 0 || timeSeconds <= 0) {
                    zeroCounts++
                } else {
                    totalCount += count
                    totalSeconds += timeSeconds
                }
            }
        }
        return if (zeroCounts > 1 || totalSeconds <= 0f) {
            0f
        } else {
            totalCount / totalSeconds
        }
    }
}

private data class CountTime(
    val count: Int,
    val timeSeconds: Float,
)
