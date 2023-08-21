package com.crisiscleanup.core.database.model

import org.junit.Test
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoordinateGridQueryTest {
    @Test
    fun radialDirectedOrdering() {
        val gridQuery = CoordinateGridQuery(SwNeBounds(0.0, 0.0, 0.0, 0.0))

        gridQuery.initializeGrid(9, 10)
        assertEquals(listOf(Pair(0, 0)), gridQuery.sortedCellCoordinates)

        gridQuery.initializeGrid(21, 10)
        val expected2 = listOf(
            Pair(1, 1),
            Pair(0, 1),
            Pair(0, 0),
            Pair(1, 0),
        )
        assertEquals(expected2, gridQuery.sortedCellCoordinates)

        gridQuery.initializeGrid(240, 10)
        assertEquals(21, gridQuery.sortedCellCoordinates.size)
        assertEquals(
            gridQuery.sortedCellCoordinates.toSet().size,
            gridQuery.sortedCellCoordinates.size,
        )
        val center = 2.5f
        val radiiSqr5 = gridQuery.sortedCellCoordinates.map {
            (it.first + 0.5f - center).pow(2) + (it.second + 0.5f - center).pow(2)
        }
        for (i in 1 until radiiSqr5.size) {
            assertTrue(radiiSqr5[i] >= radiiSqr5[i - 1])
        }
    }
}
