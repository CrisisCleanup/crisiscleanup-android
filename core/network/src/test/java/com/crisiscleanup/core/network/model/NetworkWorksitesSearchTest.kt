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
            workTypes = listOf(
                NetworkWorksiteFull.WorkType(
                    id = 1481876,
                    workType = "trees",
                    orgClaim = null,
                    phase = 4,
                    status = "open_unassigned",
                    recur = null,
                    nextRecurAt = null,
                    createdAt = Instant.parse("2022-11-10T15:16:47Z"),
                )
            ),
            city = "Fernandina Beach",
            state = "Florida",
            county = "Nassau County",
        )
        assertEquals(expected, actual)
    }
}