package com.crisiscleanup.core.mapmarker.model

import org.junit.Test
import kotlin.test.assertEquals

class CoordinateGridQueryTest {
    // TODO More comprehensive tests at multiple zoom levels

    @Test
    fun coordinatesToTileCoordinates() {
        val expectedLookup = mapOf(
            Pair(70, -135) to Pair(0.0, 0.7904007883569593),
            Pair(70, -130) to Pair(0.11111111111111112, 0.7904007883569593),
            Pair(70, -125) to Pair(0.22222222222222224, 0.7904007883569593),
            Pair(70, -120) to Pair(0.33333333333333337, 0.7904007883569593),
            Pair(70, -115) to Pair(0.4444444444444445, 0.7904007883569593),
            Pair(70, -110) to Pair(0.5555555555555556, 0.7904007883569593),
            Pair(70, -105) to Pair(0.6666666666666667, 0.7904007883569593),
            Pair(70, -100) to Pair(0.7777777777777778, 0.7904007883569593),
            Pair(70, -95) to Pair(0.888888888888889, 0.7904007883569593),
            Pair(70, -90) to Pair(1.0, 0.7904007883569593),
            Pair(75, -135) to Pair(0.0, 0.41839296767736744),
            Pair(75, -130) to Pair(0.11111111111111112, 0.41839296767736744),
            Pair(75, -125) to Pair(0.22222222222222224, 0.41839296767736744),
            Pair(75, -120) to Pair(0.33333333333333337, 0.41839296767736744),
            Pair(75, -115) to Pair(0.4444444444444445, 0.41839296767736744),
            Pair(75, -110) to Pair(0.5555555555555556, 0.41839296767736744),
            Pair(75, -105) to Pair(0.6666666666666667, 0.41839296767736744),
            Pair(75, -100) to Pair(0.7777777777777778, 0.41839296767736744),
            Pair(75, -95) to Pair(0.888888888888889, 0.41839296767736744),
            Pair(75, -90) to Pair(1.0, 0.41839296767736744),
        )
        val tileCoordinates = TileCoordinates(1, 1, 3)

        for (lat in -90 until 90 step 5) {
            for (lng in -180 until 180 step 5) {
                val n = tileCoordinates.fromLatLng(lat.toDouble(), lng.toDouble())
                val expected = expectedLookup[Pair(lat, lng)]
                assertEquals(expected, n)
            }
        }
    }
}
