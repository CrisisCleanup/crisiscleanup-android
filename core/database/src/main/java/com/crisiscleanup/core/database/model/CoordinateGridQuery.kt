package com.crisiscleanup.core.database.model

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class CoordinateGridQuery(
    private val areaBounds: SwNeBounds,
) {
    var sortedCellCoordinates: List<Pair<Int, Int>> = emptyList()
        private set

    private var latitudeDelta: Double = 0.0
    private var longitudeDelta: Double = 0.0

    fun initializeGrid(totalCount: Int, targetGridSize: Int = 50) {
        val approximateBucketCount = totalCount.toFloat() / targetGridSize
        val dimensionCount = (sqrt(approximateBucketCount) + 1).toInt().coerceAtLeast(1)
        val cellCoordinates = mutableListOf<Triple<Int, Int, Float>>()
        val gridCenter = dimensionCount * 0.5f
        for (i in 0 until dimensionCount) {
            for (j in 0 until dimensionCount) {
                val radiusSqr = (gridCenter - (i + 0.5f)).pow(2) +
                    (gridCenter - (j + 0.5f)).pow(2)
                cellCoordinates.add(Triple(i, j, radiusSqr))
            }
        }

        val centerSqr = gridCenter.pow(2)
        sortedCellCoordinates = cellCoordinates
            .filter { it.third < centerSqr }
            .sortedWith { a, b ->
                val deltaRadiusSqr = abs(a.third - b.third)
                if (deltaRadiusSqr < 1e-5f) {
                    val deltaX = a.first - b.first
                    val deltaY = a.second - b.second
                    val order = if ((deltaX >= 0 && deltaY > 0) ||
                        (deltaX >= 0 && deltaY == 0 && (a.second + 0.5f) > gridCenter) ||
                        (deltaX < 0 && deltaY > 0) ||
                        (deltaX < 0 && deltaY == 0 && (a.second + 0.5f) < gridCenter)
                    ) {
                        -1
                    } else {
                        1
                    }
                    order
                } else if (a.third < b.third) {
                    -1
                } else {
                    1
                }
            }
            .map { Pair(it.first, it.second) }

        latitudeDelta = (areaBounds.north - areaBounds.south) / dimensionCount
        longitudeDelta = (areaBounds.east - areaBounds.west) / dimensionCount
    }

    fun getSwNeGridCells() = sortedCellCoordinates.map { cellCoordinates ->
        val south = areaBounds.south + cellCoordinates.second * latitudeDelta
        val north = south + latitudeDelta
        val west = areaBounds.west + cellCoordinates.first * longitudeDelta
        val east = west + longitudeDelta
        SwNeBounds(
            south = south,
            north = north,
            west = west,
            east = east,
        )
    }
}
