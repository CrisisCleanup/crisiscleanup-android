package com.crisiscleanup.feature.cases.map

import org.junit.Test
import kotlin.test.assertEquals

class CoordinateUtilTest {
    @Test
    fun orderedTest() {
        val coordinates = listOf(
            Pair(-180.0, 180.0),
            Pair(0.0, 0.0),
            Pair(-125.4, 10.6),
            Pair(-25.54, 54.2),
        )
        val expecteds = listOf(
            0.0,
            0.0,
            -57.4,
            14.33,
        )

        for (i in expecteds.indices) {
            val (left, right) = coordinates[i]
            val actual = CoordinateUtil.getMiddleLongitude(left, right)
            assertEquals(expecteds[i], actual, 1e-9)
        }
    }

    @Test
    fun crossoverTest() {
        val coordinates = listOf(
            Pair(170.0, -170.0),
            Pair(165.4, -2.1),
            Pair(165.4, -172.1),
            Pair(177.7, -36.8),
            Pair(177.7, -178.8),
            Pair(170.0 + 360, -170.0),
            Pair(165.4 + 360, -2.1),
            Pair(165.4 + 360, -172.1),
            Pair(177.7 + 360, -36.8),
            Pair(177.7 + 360, -178.8),
            Pair(170.0, -170.0 - 360),
            Pair(165.4, -2.1 - 360),
            Pair(165.4, -172.1 - 360),
            Pair(177.7, -36.8 - 360),
            Pair(177.7, -178.8 - 360),
        )
        val expecteds = listOf(
            -180.0,
            -98.35,
            176.65,
            -109.55,
            179.45,
            -180.0,
            -98.35,
            176.65,
            -109.55,
            179.45,
            -180.0,
            -98.35,
            176.65,
            -109.55,
            179.45,
        )

        for (i in expecteds.indices) {
            val (left, right) = coordinates[i]
            val actual = CoordinateUtil.getMiddleLongitude(left, right)
            assertEquals(expecteds[i], actual, 1e-9)
        }
    }
}
