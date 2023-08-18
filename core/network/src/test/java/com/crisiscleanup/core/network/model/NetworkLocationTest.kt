package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkLocationTest {
    @Test
    fun getLocationsSuccessResult() {
        val result = TestUtil.decodeResource<NetworkLocationsResult>("/getIncidentLocations.json")

        assertNull(result.errors)
        assertEquals(3, result.count)

        val locations = result.results!!
        assertEquals(
            listOf("Polygon", "MultiPolygon", "Point"),
            locations.map(NetworkLocation::shapeType),
        )

        assertEquals(
            1074,
            locations[0].poly!!.condensedCoordinates.size,
        )

        val geoCoordinates = locations[1].geom!!.condensedCoordinates
        assertEquals(
            6,
            geoCoordinates.size,
        )
        assertEquals(
            listOf(1582, 10, 16, 18, 26, 42),
            geoCoordinates.map { it.size },
        )

        assertEquals(
            2,
            locations[2].point!!.coordinates.size,
        )
    }
}
