package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkWorksitesSearchTest {
    @Test
    fun getLocationSearchResult() {
        val result =
            TestUtil.decodeResource<NetworkWorksiteLocationSearchResult>("/worksiteLocationSearchResult.json")

        assertNull(result.errors)
        assertEquals(58, result.count)

        val actual = result.results?.get(1)
        val expected = NetworkWorksiteLocationSearch(
            incidentId = 255,
            id = 245758,
            name = "test user",
            address = "test address",
            caseNumber = "W10",
            postalCode = "32034",
            keyWorkType = NetworkWorkType(
                id = 1101893,
                workType = "tarp",
                orgClaim = null,
                phase = 4,
                status = "open_unassigned",
                recur = null,
                nextRecurAt = null,
                createdAt = Instant.parse("2022-08-11T14:08:14Z"),
            ),
            location = NetworkLocation.LocationPoint(
                type = "Point",
                coordinates = listOf(-82.9313994716109, 34.68370585735137),
            ),
            city = "Fernandina Beach",
            state = "Florida",
            county = "Nassau County",
        )
        assertEquals(expected, actual)
    }
}